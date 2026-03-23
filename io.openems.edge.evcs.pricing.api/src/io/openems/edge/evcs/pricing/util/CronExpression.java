package io.openems.edge.evcs.pricing.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Subset of the 6-field Spring/Quartz cron format.
 *
 * <p>
 * Format: {@code seconds minutes hours day-of-month month day-of-week}
 *
 * <p>
 * Supported per-field syntax:
 * <ul>
 * <li>{@code *} — matches any value</li>
 * <li>{@code *&#47;n} — matches every n-th value starting from the field
 * minimum</li>
 * <li>A fixed integer — matches exactly that value</li>
 * </ul>
 */
public class CronExpression {

	private static final int MAX_SEARCH_SECONDS = 366 * 24 * 60 * 60;
	private static final int FIELD_COUNT = 6;

	private final CronField seconds;
	private final CronField minutes;
	private final CronField hours;
	private final CronField dayOfMonth;
	private final CronField month;
	private final CronField dayOfWeek;

	/**
	 * Parses a 6-field cron expression.
	 *
	 * @param expression the cron expression string
	 * @throws IllegalArgumentException if the expression is not valid
	 */
	public CronExpression(String expression) {
		var parts = expression.trim().split("\\s+");
		if (parts.length != FIELD_COUNT) {
			throw new IllegalArgumentException(
					"Cron expression must have exactly 6 fields, got: " + parts.length + " in \"" + expression + "\"");
		}
		this.seconds = CronField.parse(parts[0], 0, 59);
		this.minutes = CronField.parse(parts[1], 0, 59);
		this.hours = CronField.parse(parts[2], 0, 23);
		this.dayOfMonth = CronField.parse(parts[3], 1, 31);
		this.month = CronField.parse(parts[4], 1, 12);
		this.dayOfWeek = CronField.parse(parts[5], 0, 6);
	}

	/**
	 * Computes the next instant strictly after {@code from} that matches this cron
	 * expression.
	 *
	 * @param from the reference instant (exclusive)
	 * @param zone the time zone to use for calendar arithmetic
	 * @return the next matching instant
	 * @throws IllegalStateException if no match is found within one year
	 */
	public Instant nextTick(Instant from, ZoneId zone) {
		var candidate = ZonedDateTime.ofInstant(from, zone).withNano(0).plusSeconds(1);

		for (int steps = 0; steps < MAX_SEARCH_SECONDS; steps++) {
			if (this.matchesAll(candidate)) {
				return candidate.toInstant();
			}
			candidate = advanceByOneSecond(candidate);
		}
		throw new IllegalStateException("No next tick found within one year for expression");
	}

	private boolean matchesAll(ZonedDateTime dt) {
		return this.seconds.matches(dt.getSecond())
				&& this.minutes.matches(dt.getMinute())
				&& this.hours.matches(dt.getHour())
				&& this.dayOfMonth.matches(dt.getDayOfMonth())
				&& this.month.matches(dt.getMonthValue())
				&& this.dayOfWeek.matches(dt.getDayOfWeek().getValue() % 7);
	}

	private static ZonedDateTime advanceByOneSecond(ZonedDateTime dt) {
		return dt.plusSeconds(1);
	}

	// -------------------------------------------------------------------------
	// Inner type: CronField
	// -------------------------------------------------------------------------

	private sealed interface CronField permits CronField.Any, CronField.EveryN, CronField.Fixed {

		boolean matches(int value);

		static CronField parse(String token, int min, int max) {
			if ("*".equals(token)) {
				return new Any();
			}
			if (token.startsWith("*/")) {
				return parseEveryN(token, min, max);
			}
			return parseFixed(token, min, max);
		}

		private static CronField parseEveryN(String token, int min, int max) {
			try {
				int step = Integer.parseInt(token.substring(2));
				if (step <= 0) {
					throw new IllegalArgumentException("Step must be positive in: \"" + token + "\"");
				}
				return new EveryN(min, step);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Invalid step value in: \"" + token + "\"", e);
			}
		}

		private static CronField parseFixed(String token, int min, int max) {
			try {
				int value = Integer.parseInt(token);
				if (value < min || value > max) {
					throw new IllegalArgumentException(
							"Value " + value + " out of range [" + min + "," + max + "] in: \"" + token + "\"");
				}
				return new Fixed(value);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Unsupported cron field token: \"" + token + "\"", e);
			}
		}

		record Any() implements CronField {
			@Override
			public boolean matches(int value) {
				return true;
			}
		}

		record EveryN(int start, int step) implements CronField {
			@Override
			public boolean matches(int value) {
				return value >= this.start && (value - this.start) % this.step == 0;
			}
		}

		record Fixed(int expected) implements CronField {
			@Override
			public boolean matches(int value) {
				return value == this.expected;
			}
		}
	}
}
