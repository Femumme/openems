package io.openems.edge.evcs.pricing.util;

import java.time.Duration;
import java.time.Instant;
import java.util.OptionalDouble;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Thread-safe rolling average over a configurable time window.
 *
 * <p>
 * Maintains a buffer of timestamped values and computes the mean over the most
 * recent {@code windowMinutes}. Stale entries are evicted on each
 * {@link #add(double)} call.
 */
public class RollingAverage {

	private record Sample(Instant timestamp, double value) {
	}

	private final Duration window;
	private final ConcurrentLinkedDeque<Sample> buffer = new ConcurrentLinkedDeque<>();

	/**
	 * Creates a new RollingAverage with the given window.
	 *
	 * @param windowMinutes the rolling average window in minutes
	 */
	public RollingAverage(int windowMinutes) {
		this.window = Duration.ofMinutes(windowMinutes);
	}

	/**
	 * Adds a value sample at the current time.
	 *
	 * @param value the value to add
	 */
	public void add(double value) {
		this.buffer.addLast(new Sample(Instant.now(), value));
		this.evictStale();
	}

	/**
	 * Returns the average of all samples within the window.
	 *
	 * @return the average, or empty if no samples are present
	 */
	public OptionalDouble getAverage() {
		var cutoff = Instant.now().minus(this.window);
		return this.buffer.stream()
				.filter(s -> s.timestamp.isAfter(cutoff))
				.mapToDouble(Sample::value)
				.average();
	}

	/**
	 * Removes samples that are older than the window.
	 */
	private void evictStale() {
		var cutoff = Instant.now().minus(this.window);
		while (!this.buffer.isEmpty() && this.buffer.peekFirst().timestamp.isBefore(cutoff)) {
			this.buffer.pollFirst();
		}
	}
}
