package io.openems.edge.common.filter;

/**
 * This implementation ignores the PID filter and instead just returns the
 * unfiltered target value - making sure it is within the allowed minimum and
 * maximum limits. It is used when {@link PowerComponent} is configured to
 * disable PID filter.
 */
public final class DisabledPidFilter extends PidFilter {

	private static final PidSnapshot EMPTY_SNAPSHOT = new PidSnapshot(0, 0, true);

	public static final DisabledPidFilter INSTANCE = new DisabledPidFilter();

	private DisabledPidFilter() {
	}

	@Override
	public int applyPidFilter(int input, int target) {
		return this.applyLowHighLimits(target);
	}

	@Override
	public PidSnapshot saveState() {
		return EMPTY_SNAPSHOT;
	}

	@Override
	public void restoreState(PidSnapshot snapshot) {
		// No-op: DisabledPidFilter has no mutable state to restore
	}

}
