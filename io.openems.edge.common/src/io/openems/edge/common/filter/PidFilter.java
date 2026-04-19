package io.openems.edge.common.filter;

/**
 * A proportional-integral-derivative controller.
 *
 * @see <a href=
 *      "https://en.wikipedia.org/wiki/PID_controller">https://en.wikipedia.org/wiki/PID_controller</a>
 */
public class PidFilter {

	public static final double DEFAULT_P = 0.3;
	public static final double DEFAULT_I = 0.3;
	public static final double DEFAULT_D = 0.1;

	/**
	 * Safety margin applied to the dynamic integrator limit. The integrator ceiling
	 * is computed as {@code max(|lowLimit|, |highLimit|) / I * SAFETY_MARGIN} so
	 * that the integrator can always accumulate enough to drive the output to the
	 * full target value at steady state.
	 */
	private static final double ERROR_SUM_LIMIT_SAFETY_MARGIN = 1.5;

	private final double p;
	private final double i;
	private final double d;

	private boolean firstRun = true;

	private double lastInput = 0;
	private double errorSum = 0;
	private Integer lowLimit = null;
	private Integer highLimit = null;

	/**
	 * Creates a PidFilter.
	 *
	 * @param p the proportional gain
	 * @param i the integral gain
	 * @param d the derivative gain
	 */
	public PidFilter(double p, double i, double d) {
		this.p = p;
		this.i = i;
		this.d = d;
	}

	/**
	 * Creates a PidFilter using default values.
	 */
	public PidFilter() {
		this(DEFAULT_P, DEFAULT_I, DEFAULT_D);
	}

	/**
	 * Limit the output value.
	 *
	 * @param lowLimit  lowest allowed output value
	 * @param highLimit highest allowed output value
	 */
	public void setLimits(Integer lowLimit, Integer highLimit) {
		if (lowLimit != null && highLimit != null && lowLimit > highLimit) {
			throw new IllegalArgumentException(
					"Given LowLimit [" + lowLimit + "] is higher than HighLimit [" + highLimit + "]");
		}
		this.lowLimit = lowLimit;
		this.highLimit = highLimit;
	}

	/**
	 * Apply the PID filter using the current Channel value as input and the target
	 * value.
	 *
	 * @param input  the input value, e.g. the measured Channel value
	 * @param target the target value
	 * @return the filtered set-point value
	 */
	public int applyPidFilter(int input, int target) {
		// Pre-process the target value: apply output value limits
		target = this.applyLowHighLimits(target);

		// Calculate the error
		var error = target - input;

		// We are already there
		if (error == 0) {
			return target;
		}

		// Calculate P
		var outputP = this.p * error;

		// Set last values on first run
		if (this.firstRun) {
			this.lastInput = input;
			this.firstRun = false;
		}

		// Calculate I
		var outputI = this.i * this.errorSum;

		// Calculate D
		var outputD = -this.d * (input - this.lastInput);

		// Store last input value
		this.lastInput = input;

		// Sum outputs
		var output = outputP + outputI + outputD;

		// Sum up the error and limit Error-Sum to not grow too much. Otherwise the PID
		// filter will stop reacting on changes properly.
		this.errorSum = this.applyErrorSumLimit(this.errorSum + error);

		// Post-process the output value: convert to integer and apply value limits
		return this.applyLowHighLimits(Math.round((float) output));
	}

	/**
	 * Captures the mutable PID integrator state for later restoration.
	 *
	 * <p>This snapshot includes only the internal integrator state ({@code errorSum},
	 * {@code lastInput}, {@code firstRun}). Output limits ({@code lowLimit},
	 * {@code highLimit}) are not captured because they are set externally each cycle
	 * via {@link #setLimits(Integer, Integer)}.
	 *
	 * @param errorSum  accumulated integral error
	 * @param lastInput previous input sample
	 * @param firstRun  whether the filter has not yet processed any input
	 */
	public record PidSnapshot(double errorSum, double lastInput, boolean firstRun) {
	}

	/**
	 * Saves the current PID state as a snapshot.
	 *
	 * @return snapshot of current internal state
	 */
	public PidSnapshot saveState() {
		return new PidSnapshot(this.errorSum, this.lastInput, this.firstRun);
	}

	/**
	 * Restores PID state from a previously saved snapshot.
	 *
	 * @param snapshot the state to restore
	 */
	public void restoreState(PidSnapshot snapshot) {
		this.errorSum = snapshot.errorSum();
		this.lastInput = snapshot.lastInput();
		this.firstRun = snapshot.firstRun();
	}

	/**
	 * Reset the PID filter.
	 *
	 * <p>
	 * This method should be called when the filter was not used for a while.
	 */
	public void reset() {
		this.errorSum = 0;
		this.firstRun = true;
	}

	/**
	 * Applies the configured PID low and high limits to a value.
	 *
	 * @param value the input value
	 * @return the value within low and high limit
	 */
	protected int applyLowHighLimits(int value) {
		if (this.lowLimit != null && value < this.lowLimit) {
			value = this.lowLimit;
		}
		if (this.highLimit != null && value > this.highLimit) {
			value = this.highLimit;
		}
		return value;
	}

	/**
	 * Applies the low and high limits to the error sum.
	 *
	 * <p>
	 * The limit is derived dynamically from the output limits and the integral
	 * gain. At steady state the integrator must supply {@code target / I} to
	 * maintain the output at the target value. The limit is set to
	 * {@code max(|lowLimit|, |highLimit|) / I * SAFETY_MARGIN} so the integrator
	 * is never the bottleneck preventing the PID from reaching the full target.
	 *
	 * @param value the input value
	 * @return the value within low and high limit
	 */
	private double applyErrorSumLimit(double value) {
		// find (positive) limit from low & high limits
		double errorSumLimit;
		if (this.lowLimit != null && this.highLimit != null) {
			errorSumLimit = Math.max(Math.abs(this.lowLimit), Math.abs(this.highLimit));
		} else if (this.lowLimit != null) {
			errorSumLimit = Math.abs(this.lowLimit);
		} else if (this.highLimit != null) {
			errorSumLimit = Math.abs(this.highLimit);
		} else {
			return value;
		}

		// Scale by inverse of integral gain so the integrator can supply the full
		// output range at steady state. Falls back to a fixed factor of 4 when the
		// integral gain is zero or negligible to avoid division by near-zero.
		if (this.i > 1e-6) {
			errorSumLimit = errorSumLimit / this.i * ERROR_SUM_LIMIT_SAFETY_MARGIN;
		} else {
			errorSumLimit *= 4;
		}

		// apply limit
		if (value < errorSumLimit * -1) {
			return errorSumLimit * -1;
		}
		if (value > errorSumLimit) {
			return errorSumLimit;
		}
		return value;
	}
}