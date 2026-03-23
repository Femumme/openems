package io.openems.edge.evcs.pricing.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Test;

public class CronExpressionTest {

	private static final ZoneId UTC = ZoneId.of("UTC");

	private static Instant at(int hour, int minute, int second) {
		return ZonedDateTime.of(2024, 6, 15, hour, minute, second, 0, UTC).toInstant();
	}

	@Test
	public void hourly_fromMidHour_nextIsFullHour() {
		// 0 0 * * * * — every full hour
		var cron = new CronExpression("0 0 * * * *");
		var next = cron.nextTick(at(10, 30, 0), UTC);
		assertEquals(at(11, 0, 0), next);
	}

	@Test
	public void everyThirtyMinutes_justBefore30_nextIs30() {
		// 0 */30 * * * * — :00 and :30 each hour
		var cron = new CronExpression("0 */30 * * * *");
		var next = cron.nextTick(at(10, 29, 59), UTC);
		assertEquals(at(10, 30, 0), next);
	}

	@Test
	public void everyThirtyMinutes_justAfter30_nextIsNextHour() {
		var cron = new CronExpression("0 */30 * * * *");
		var next = cron.nextTick(at(10, 31, 0), UTC);
		assertEquals(at(11, 0, 0), next);
	}

	@Test
	public void everyFifteenMinutes_justBefore15_nextIs15() {
		// 0 */15 * * * * — :00, :15, :30, :45 each hour
		var cron = new CronExpression("0 */15 * * * *");
		var next = cron.nextTick(at(10, 14, 59), UTC);
		assertEquals(at(10, 15, 0), next);
	}

	@Test
	public void everyTwoHours_midOddHour_nextIsNextEvenHour() {
		// 0 0 */2 * * * — 00:00, 02:00, 04:00, ...
		var cron = new CronExpression("0 0 */2 * * *");
		var next = cron.nextTick(at(1, 30, 0), UTC);
		assertEquals(at(2, 0, 0), next);
	}

	@Test
	public void invalidExpression_tooFewFields_throwsIllegalArgumentException() {
		assertThrows(IllegalArgumentException.class, () -> new CronExpression("not valid"));
	}

	@Test
	public void invalidExpression_badFieldValue_throwsIllegalArgumentException() {
		assertThrows(IllegalArgumentException.class, () -> new CronExpression("abc 0 * * * *"));
	}
}
