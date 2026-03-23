package io.openems.edge.evcs.pricing.core;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.evcs.pricing.EvcsPricing;

public class EvcsPricingCoreImplTest {

	/**
	 * Default config used by most tests: min=0, max=9.99, hourly cron.
	 *
	 * @return a {@link MyConfig}
	 */
	private static MyConfig defaultConfig() {
		return MyConfig.create() //
				.setId("_evcsPricing") //
				.setAlias("EVCS Pricing") //
				.setEnabled(true) //
				.setCronExpression("0 0 * * * *") //
				.setAbsoluteMinPrice(0.00) //
				.setAbsoluteMaxPrice(9.99) //
				.build();
	}

	// -------------------------------------------------------------------------
	// Override wins
	// -------------------------------------------------------------------------

	/**
	 * When a single override is set, the PRICE channel reflects the override value.
	 *
	 * <p>
	 * The first cycle always locks the price because nextIntervalTick starts at
	 * Instant.MIN (already in the past).
	 */
	@Test
	public void overrideWins_priceEqualsOverrideValue() throws Exception {
		var sut = new EvcsPricingCoreImpl();

		new ComponentTest(sut) //
				.activate(defaultConfig()) //
				.next(new TestCase("override wins") //
						.onExecuteControllersCallbacks(() -> sut.setOverride("ctrl0", 0.25)) //
						.output(EvcsPricing.ChannelId.PRICE, 0.25)) //
				.deactivate();
	}

	// -------------------------------------------------------------------------
	// Ceiling resolution
	// -------------------------------------------------------------------------

	/**
	 * When two ceilings are added, the lowest one wins.
	 *
	 * <p>
	 * Uses an override to ensure the price is locked on the first cycle, then
	 * removes it before the ceiling test so the ceiling path is exercised. We rely
	 * on nextIntervalTick=Instant.MIN for the first lock, then reset the override.
	 *
	 * <p>
	 * Strategy: cycle 1 sets an override so the price gets locked. Cycle 2 removes
	 * the override — the override→null change triggers a lock so the ceiling
	 * resolves.
	 */
	@Test
	public void ceilingResolution_lowestCeilingWins() throws Exception {
		var sut = new EvcsPricingCoreImpl();

		new ComponentTest(sut) //
				.activate(defaultConfig()) //
				// Cycle 1: set an override to prime the locked price
				.next(new TestCase("prime override") //
						.onExecuteControllersCallbacks(() -> sut.setOverride("ctrl0", 0.50))) //
				// Cycle 2: remove override, add two ceilings → override-change triggers lock
				.next(new TestCase("ceiling resolution") //
						.onExecuteControllersCallbacks(() -> {
							sut.removeOverride("ctrl0");
							sut.addPriceCeiling("ctrl1", 0.30);
							sut.addPriceCeiling("ctrl2", 0.20);
						}) //
						.output(EvcsPricing.ChannelId.PRICE, 0.20)) //
				.deactivate();
	}

	// -------------------------------------------------------------------------
	// Floor beats ceiling
	// -------------------------------------------------------------------------

	/**
	 * When the floor exceeds the ceiling, the floor wins.
	 *
	 * <p>
	 * Cycle 1: prime locked price via override. Cycle 2: remove override, add
	 * ceiling=0.10 and floor=0.30 → floor wins → price=0.30.
	 */
	@Test
	public void floorBeatsCeiling_floorWinsWhenHigherThanCeiling() throws Exception {
		var sut = new EvcsPricingCoreImpl();

		new ComponentTest(sut) //
				.activate(defaultConfig()) //
				// Cycle 1: prime locked price
				.next(new TestCase("prime override") //
						.onExecuteControllersCallbacks(() -> sut.setOverride("ctrl0", 0.10))) //
				// Cycle 2: floor > ceiling → floor wins
				.next(new TestCase("floor beats ceiling") //
						.onExecuteControllersCallbacks(() -> {
							sut.removeOverride("ctrl0");
							sut.addPriceCeiling("ctrl1", 0.10);
							sut.addPriceFloor("ctrl2", 0.30);
						}) //
						.output(EvcsPricing.ChannelId.PRICE, 0.30)) //
				.deactivate();
	}

	// -------------------------------------------------------------------------
	// No constraints → price stays at last locked value
	// -------------------------------------------------------------------------

	/**
	 * When no ceilings are present (and no override), resolveConstraints returns
	 * the previously locked price unchanged.
	 *
	 * <p>
	 * Cycle 1: override=0.40 → locks price=0.40. Cycle 2: override removed,
	 * no ceilings → override-change triggers lock but resolveConstraints returns
	 * the current channel value (0.40). Cycle 3: no constraints, no override
	 * change, interval not reached → price channel unchanged (0.40).
	 */
	@Test
	public void noConstraints_priceRetainsPreviousLockedValue() throws Exception {
		var sut = new EvcsPricingCoreImpl();

		new ComponentTest(sut) //
				.activate(defaultConfig()) //
				// Cycle 1: lock an initial price via override
				.next(new TestCase("lock initial price") //
						.onExecuteControllersCallbacks(() -> sut.setOverride("ctrl0", 0.40))) //
				// Cycle 2: remove override → triggers override-change lock;
				// no ceilings → price reverts to current channel value
				.next(new TestCase("no constraints after override removed") //
						.onExecuteControllersCallbacks(() -> sut.removeOverride("ctrl0")) //
						.output(EvcsPricing.ChannelId.PRICE, 0.40)) //
				.deactivate();
	}

	// -------------------------------------------------------------------------
	// Override removed → reverts to constraint-based price
	// -------------------------------------------------------------------------

	/**
	 * After removeOverride, the next cycle resolves from ceilings instead.
	 *
	 * <p>
	 * Cycle 1: override=0.99 locks price. Cycle 2: remove override + add
	 * ceiling=0.15 → override-change triggers lock → ceiling wins → 0.15.
	 */
	@Test
	public void overrideRemoved_revertsToConstraintBasedPrice() throws Exception {
		var sut = new EvcsPricingCoreImpl();

		new ComponentTest(sut) //
				.activate(defaultConfig()) //
				// Cycle 1: set override
				.next(new TestCase("set override") //
						.onExecuteControllersCallbacks(() -> sut.setOverride("ctrl0", 0.99))) //
				// Cycle 2: remove override, ceiling now drives the price
				.next(new TestCase("after override removed") //
						.onExecuteControllersCallbacks(() -> {
							sut.removeOverride("ctrl0");
							sut.addPriceCeiling("ctrl1", 0.15);
						}) //
						.output(EvcsPricing.ChannelId.PRICE, 0.15)) //
				.deactivate();
	}

	// -------------------------------------------------------------------------
	// Absolute clamp
	// -------------------------------------------------------------------------

	/**
	 * A price above absoluteMaxPrice is clamped to absoluteMaxPrice.
	 *
	 * <p>
	 * Config: min=0.05, max=0.50. Override=0.99 → clamped to 0.50.
	 */
	@Test
	public void absoluteClamp_overrideAboveMaxIsClamped() throws Exception {
		var sut = new EvcsPricingCoreImpl();
		var config = MyConfig.create() //
				.setId("_evcsPricing") //
				.setAlias("EVCS Pricing") //
				.setEnabled(true) //
				.setCronExpression("0 0 * * * *") //
				.setAbsoluteMinPrice(0.05) //
				.setAbsoluteMaxPrice(0.50) //
				.build();

		new ComponentTest(sut) //
				.activate(config) //
				.next(new TestCase("override clamped to max") //
						.onExecuteControllersCallbacks(() -> sut.setOverride("ctrl0", 0.99)) //
						.output(EvcsPricing.ChannelId.PRICE, 0.50)) //
				.deactivate();
	}

	/**
	 * A price below absoluteMinPrice is clamped to absoluteMinPrice.
	 *
	 * <p>
	 * Config: min=0.10, max=9.99. Override=0.01 → clamped to 0.10.
	 */
	@Test
	public void absoluteClamp_overrideBelowMinIsClamped() throws Exception {
		var sut = new EvcsPricingCoreImpl();
		var config = MyConfig.create() //
				.setId("_evcsPricing") //
				.setAlias("EVCS Pricing") //
				.setEnabled(true) //
				.setCronExpression("0 0 * * * *") //
				.setAbsoluteMinPrice(0.10) //
				.setAbsoluteMaxPrice(9.99) //
				.build();

		new ComponentTest(sut) //
				.activate(config) //
				.next(new TestCase("override clamped to min") //
						.onExecuteControllersCallbacks(() -> sut.setOverride("ctrl0", 0.01)) //
						.output(EvcsPricing.ChannelId.PRICE, 0.10)) //
				.deactivate();
	}

	// -------------------------------------------------------------------------
	// Floor only — no ceiling present
	// -------------------------------------------------------------------------

	/**
	 * When only a floor is set (no ceiling), the price must rise to at least
	 * the floor value, not stay at the previous locked value.
	 *
	 * <p>
	 * Cycle 1: lock price=0.10 via override.
	 * Cycle 2: remove override, add only floor=0.40 → price must become 0.40.
	 */
	@Test
	public void floorOnly_priceMeetsFloor() throws Exception {
		var sut = new EvcsPricingCoreImpl();

		new ComponentTest(sut) //
				.activate(defaultConfig()) //
				.next(new TestCase("prime price") //
						.onExecuteControllersCallbacks(() -> sut.setOverride("ctrl0", 0.10))) //
				.next(new TestCase("floor only") //
						.onExecuteControllersCallbacks(() -> {
							sut.removeOverride("ctrl0");
							sut.addPriceFloor("ctrl1", 0.40);
						}) //
						.output(EvcsPricing.ChannelId.PRICE, 0.40)) //
				.deactivate();
	}

	@Test
	public void floorOnly_aboveAbsoluteMaxIsClamped() throws Exception {
		var sut = new EvcsPricingCoreImpl();
		var config = MyConfig.create() //
				.setId("_evcsPricing").setAlias("EVCS Pricing").setEnabled(true) //
				.setCronExpression("0 0 * * * *") //
				.setAbsoluteMinPrice(0.00).setAbsoluteMaxPrice(0.50) //
				.build();

		new ComponentTest(sut) //
				.activate(config) //
				.next(new TestCase("prime price") //
						.onExecuteControllersCallbacks(() -> sut.setOverride("ctrl0", 0.10))) //
				.next(new TestCase("floor only above max") //
						.onExecuteControllersCallbacks(() -> {
							sut.removeOverride("ctrl0");
							sut.addPriceFloor("ctrl1", 0.99); // above max=0.50
						}) //
						.output(EvcsPricing.ChannelId.PRICE, 0.50)) // clamped
				.deactivate();
	}

	// -------------------------------------------------------------------------
	// Active override channels
	// -------------------------------------------------------------------------

	/**
	 * When two overrides are set, the higher price wins.
	 */
	@Test
	public void multipleOverrides_highestPriceWins() throws Exception {
		var sut = new EvcsPricingCoreImpl();

		new ComponentTest(sut) //
				.activate(defaultConfig()) //
				.next(new TestCase("two overrides") //
						.onExecuteControllersCallbacks(() -> {
							sut.setOverride("ctrl0", 0.25);
							sut.setOverride("ctrl1", 0.75);
						}) //
						.output(EvcsPricing.ChannelId.PRICE, 0.75) //
						.output(EvcsPricing.ChannelId.ACTIVE_OVERRIDE_SOURCE, "ctrl1") //
						.output(EvcsPricing.ChannelId.ACTIVE_OVERRIDE_VALUE, 0.75)) //
				.deactivate();
	}

	/**
	 * When an override is active, ACTIVE_OVERRIDE_SOURCE and ACTIVE_OVERRIDE_VALUE
	 * channels are populated.
	 */
	@Test
	public void activeOverrideChannels_setWhenOverrideActive() throws Exception {
		var sut = new EvcsPricingCoreImpl();

		new ComponentTest(sut) //
				.activate(defaultConfig()) //
				.next(new TestCase("override channels") //
						.onExecuteControllersCallbacks(() -> sut.setOverride("ctrl0", 0.25)) //
						.output(EvcsPricing.ChannelId.ACTIVE_OVERRIDE_SOURCE, "ctrl0") //
						.output(EvcsPricing.ChannelId.ACTIVE_OVERRIDE_VALUE, 0.25)) //
				.deactivate();
	}

	/**
	 * When no override is active, ACTIVE_OVERRIDE_SOURCE and ACTIVE_OVERRIDE_VALUE
	 * are null.
	 */
	@Test
	public void activeOverrideChannels_nullWhenNoOverrideActive() throws Exception {
		var sut = new EvcsPricingCoreImpl();

		new ComponentTest(sut) //
				.activate(defaultConfig()) //
				.next(new TestCase("no override channels") //
						.onExecuteControllersCallbacks(() -> sut.addPriceCeiling("ctrl0", 0.30)) //
						.output(EvcsPricing.ChannelId.ACTIVE_OVERRIDE_SOURCE, null) //
						.output(EvcsPricing.ChannelId.ACTIVE_OVERRIDE_VALUE, null)) //
				.deactivate();
	}

	// -------------------------------------------------------------------------
	// Config change — immediate re-clamp
	// -------------------------------------------------------------------------

	/**
	 * When absoluteMaxPrice is tightened via config change, the locked PRICE is
	 * immediately clamped — it must not wait until the next cron tick.
	 *
	 * <p>
	 * Cycle 1: lock price=0.80 via override. Cycle 2: call applyConfig with
	 * absoluteMaxPrice=0.50 before controllers run — PRICE must be 0.50.
	 */
	@Test
	public void configChange_clampsTightensLockedPrice() throws Exception {
		var sut = new EvcsPricingCoreImpl();

		var tightConfig = MyConfig.create() //
				.setId("_evcsPricing") //
				.setAlias("EVCS Pricing") //
				.setEnabled(true) //
				.setCronExpression("0 0 * * * *") //
				.setAbsoluteMinPrice(0.00) //
				.setAbsoluteMaxPrice(0.50) //
				.build();

		new ComponentTest(sut) //
				.activate(defaultConfig()) // max=9.99
				.next(new TestCase("lock price at 0.80") //
						.onExecuteControllersCallbacks(() -> sut.setOverride("ctrl0", 0.80)) //
						.output(EvcsPricing.ChannelId.PRICE, 0.80)) //
				.next(new TestCase("config tightens max to 0.50 — price clamped immediately") //
						.onBeforeControllersCallbacks(() -> sut.applyConfig(tightConfig)) //
						.output(EvcsPricing.ChannelId.PRICE, 0.50)) //
				.deactivate();
	}

	// -------------------------------------------------------------------------
	// Interval tick promotes constraint-based price
	// -------------------------------------------------------------------------

	/**
	 * After the interval tick is reached, the locked price is updated even with no
	 * override or override change.
	 *
	 * <p>
	 * Cycle 1: clock at 10:30 — interval tick (11:00) not yet reached, ceiling
	 * submitted but not locked. Cycle 2: clock advances past 11:00:01 — interval
	 * reached, ceiling 0.30 is locked.
	 */
	@Test
	public void intervalTick_locksPrice() throws Exception {
		var baseTime = ZonedDateTime.of(2024, 6, 15, 10, 30, 0, 0, ZoneOffset.UTC).toInstant();
		var tickTime = ZonedDateTime.of(2024, 6, 15, 11, 0, 1, 0, ZoneOffset.UTC).toInstant();

		var sut = new EvcsPricingCoreImpl();
		sut.setClock(Clock.fixed(baseTime, ZoneOffset.UTC));

		new ComponentTest(sut) //
				.activate(defaultConfig()) //
				// Cycle 1: before tick — ceiling set but interval not yet reached, no lock
				.next(new TestCase("before tick — interval not reached") //
						.onBeforeControllersCallbacks(() -> sut.setClock(Clock.fixed(baseTime, ZoneOffset.UTC))) //
						.onExecuteControllersCallbacks(() -> sut.addPriceCeiling("ctrl0", 0.30))) //
				// Cycle 2: advance clock past tick — interval reached, price locked to 0.30
				.next(new TestCase("after tick — price locked") //
						.onBeforeControllersCallbacks(() -> sut.setClock(Clock.fixed(tickTime, ZoneOffset.UTC))) //
						.onExecuteControllersCallbacks(() -> sut.addPriceCeiling("ctrl0", 0.30)) //
						.output(EvcsPricing.ChannelId.PRICE, 0.30)) //
				.deactivate();
	}
}
