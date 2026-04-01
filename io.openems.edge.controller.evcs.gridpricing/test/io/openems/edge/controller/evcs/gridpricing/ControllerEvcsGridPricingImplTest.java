package io.openems.edge.controller.evcs.gridpricing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static io.openems.common.test.TestUtils.createDummyClock;

import java.time.ZonedDateTime;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.evcs.pricing.DummyEvcsPricing;
import io.openems.edge.evcs.pricing.EvcsPricingController;
import io.openems.edge.timeofusetariff.test.DummyTimeOfUseTariffProvider;

public class ControllerEvcsGridPricingImplTest {

	private static final String CTRL_ID = "ctrlEvcsGridPricing0";
	private static final TimeLeapClock CLOCK = createDummyClock();

	// Prices in ct/kWh used for config
	private static final double PRICE_THRESHOLD = 2.0;
	private static final double CEILING_PRICE = 5.0;

	// 5.0 ct/kWh ÷ 100 = 0.05 EUR/kWh (expected submitted value)
	private static final double CEILING_PRICE_EUR = 0.05;

	// Delta for floating-point assertions
	private static final double DELTA = 1e-6;

	/**
	 * Builds a config with the default test threshold and ceiling.
	 *
	 * @return a {@link MyConfig} with default threshold and ceiling price
	 */
	private static MyConfig baseConfig() {
		return MyConfig.create() //
				.setId(CTRL_ID) //
				.setAlias("Test Grid Pricing Controller") //
				.setPriceThreshold(PRICE_THRESHOLD) //
				.setCeilingPrice(CEILING_PRICE) //
				.build();
	}

	/**
	 * Builds a disabled config keeping the same threshold/ceiling.
	 *
	 * @return a {@link MyConfig} with enabled set to false
	 */
	private static MyConfig disabledConfig() {
		return MyConfig.create() //
				.setId(CTRL_ID) //
				.setAlias("Test Grid Pricing Controller") //
				.setEnabled(false) //
				.setPriceThreshold(PRICE_THRESHOLD) //
				.setCeilingPrice(CEILING_PRICE) //
				.build();
	}

	/**
	 * Price below threshold: 10 Currency/MWh → 1.0 ct/kWh &lt; 2.0 threshold.
	 * Ceiling must be set and ACTIVE_CEILING must reflect it.
	 */
	@Test
	public void belowThreshold_setsCeiling() throws Exception {
		var dummy = new DummyEvcsPricing();
		var tariff = DummyTimeOfUseTariffProvider.fromQuarterlyPrices(CLOCK, 10.0, 10.0, 10.0,
				10.0);

		new ControllerTest(new ControllerEvcsGridPricingImpl()) //
				.addReference("componentManager", new DummyComponentManager(CLOCK)) //
				.addReference("evcsPricing", dummy) //
				.addReference("timeOfUseTariff", tariff) //
				.activate(baseConfig()) //
				.next(new TestCase() //
						.output(EvcsPricingController.ChannelId.ACTIVE_CEILING, CEILING_PRICE_EUR)) //
				.deactivate();

		assertEquals(Double.valueOf(CEILING_PRICE_EUR), dummy.getLastCeilingPrice());
	}

	/**
	 * Price above threshold: 100 Currency/MWh → 10.0 ct/kWh ≥ 2.0 threshold.
	 * No ceiling must be submitted and ACTIVE_CEILING must be null.
	 */
	@Test
	public void aboveThreshold_noCeiling() throws Exception {
		var dummy = new DummyEvcsPricing();
		var tariff = DummyTimeOfUseTariffProvider.fromQuarterlyPrices(CLOCK, 100.0, 100.0, 100.0,
				100.0);

		new ControllerTest(new ControllerEvcsGridPricingImpl()) //
				.addReference("componentManager", new DummyComponentManager(CLOCK)) //
				.addReference("evcsPricing", dummy) //
				.addReference("timeOfUseTariff", tariff) //
				.activate(baseConfig()) //
				.next(new TestCase() //
						.output(EvcsPricingController.ChannelId.ACTIVE_CEILING, null)) //
				.deactivate();

		assertNull(dummy.getLastCeilingPrice());
	}

	/**
	 * Price exactly equal to the threshold: 20 Currency/MWh → 2.0 ct/kWh == 2.0.
	 * The strict {@code <} comparison must NOT apply the ceiling at the boundary.
	 */
	@Test
	public void atThreshold_noCeiling() throws Exception {
		var dummy = new DummyEvcsPricing();
		// 20 Currency/MWh -> 20/10 = 2.0 ct/kWh; 2.0 < 2.0 is false -> no ceiling
		var tariff = DummyTimeOfUseTariffProvider.fromQuarterlyPrices(CLOCK, 20.0, 20.0, 20.0,
				20.0);

		new ControllerTest(new ControllerEvcsGridPricingImpl()) //
				.addReference("componentManager", new DummyComponentManager(CLOCK)) //
				.addReference("evcsPricing", dummy) //
				.addReference("timeOfUseTariff", tariff) //
				.activate(baseConfig()) //
				.next(new TestCase() //
						.output(EvcsPricingController.ChannelId.ACTIVE_CEILING, null)) //
				.deactivate();

		assertNull(dummy.getLastCeilingPrice());
	}

	/**
	 * Empty tariff prices: all channels must be cleared; no exception.
	 */
	@Test
	public void emptyPrices_noCeiling() throws Exception {
		var dummy = new DummyEvcsPricing();
		var tariff = DummyTimeOfUseTariffProvider.empty(CLOCK);

		new ControllerTest(new ControllerEvcsGridPricingImpl()) //
				.addReference("componentManager", new DummyComponentManager(CLOCK)) //
				.addReference("evcsPricing", dummy) //
				.addReference("timeOfUseTariff", tariff) //
				.activate(baseConfig()) //
				.next(new TestCase() //
						.output(EvcsPricingController.ChannelId.ACTIVE_CEILING, null) //
						.output(ControllerEvcsGridPricing.ChannelId.AVERAGE_GRID_PRICE, null)) //
				.deactivate();

		assertNull(dummy.getLastCeilingPrice());
	}

	/**
	 * No tariff provider bound (OPTIONAL reference stays null): all channels must
	 * be cleared; no exception.
	 */
	@Test
	public void noTariffProvider_noCeiling() throws Exception {
		var dummy = new DummyEvcsPricing();

		new ControllerTest(new ControllerEvcsGridPricingImpl()) //
				.addReference("componentManager", new DummyComponentManager(CLOCK)) //
				.addReference("evcsPricing", dummy) //
				.activate(baseConfig()) //
				.next(new TestCase() //
						.output(EvcsPricingController.ChannelId.ACTIVE_CEILING, null) //
						.output(ControllerEvcsGridPricing.ChannelId.AVERAGE_GRID_PRICE, null)) //
				.deactivate();

		assertNull(dummy.getLastCeilingPrice());
	}

	/**
	 * Unit-conversion verification: 50 Currency/MWh ÷ 10 = 5.0 ct/kWh.
	 * With threshold=6.0, price is below threshold so the ceiling must be applied.
	 * AVERAGE_GRID_PRICE must report exactly 5.0.
	 */
	@Test
	public void unitConversion_currencyMwhToCtKwh() throws Exception {
		var dummy = new DummyEvcsPricing();
		var tariff = DummyTimeOfUseTariffProvider.fromQuarterlyPrices(CLOCK, 50.0, 50.0, 50.0,
				50.0);

		var config = MyConfig.create() //
				.setId(CTRL_ID) //
				.setPriceThreshold(6.0) //
				.setCeilingPrice(CEILING_PRICE) //
				.build();

		new ControllerTest(new ControllerEvcsGridPricingImpl()) //
				.addReference("componentManager", new DummyComponentManager(CLOCK)) //
				.addReference("evcsPricing", dummy) //
				.addReference("timeOfUseTariff", tariff) //
				.activate(config) //
				.next(new TestCase() //
						.output(ControllerEvcsGridPricing.ChannelId.AVERAGE_GRID_PRICE, 5.0)) //
				.deactivate();
	}

	/**
	 * Unit-conversion verification: ceiling submitted as EUR/kWh (ct/kWh ÷ 100).
	 * 5.0 ct/kWh ÷ 100 must equal 0.05 EUR/kWh submitted to EvcsPricing.
	 */
	@Test
	public void unitConversion_ceilingSubmission() throws Exception {
		var dummy = new DummyEvcsPricing();
		// 50 Currency/MWh → 5.0 ct/kWh, threshold=10.0 → below → ceiling applied
		var tariff = DummyTimeOfUseTariffProvider.fromQuarterlyPrices(CLOCK, 50.0, 50.0, 50.0,
				50.0);

		var config = MyConfig.create() //
				.setId(CTRL_ID) //
				.setPriceThreshold(10.0) //
				.setCeilingPrice(CEILING_PRICE) //
				.build();

		new ControllerTest(new ControllerEvcsGridPricingImpl()) //
				.addReference("componentManager", new DummyComponentManager(CLOCK)) //
				.addReference("evcsPricing", dummy) //
				.addReference("timeOfUseTariff", tariff) //
				.activate(config) //
				.next(new TestCase()) //
				.deactivate();

		assertEquals(0.05, dummy.getLastCeilingPrice(), DELTA);
	}

	/**
	 * Ceiling price requiring rounding: 3.3333 ct/kWh ÷ 100 = 0.033333 EUR/kWh
	 * which must be rounded to 4 decimal places (HALF_UP) → 0.0333.
	 */
	@Test
	public void roundPrice_roundsCeilingTo4Decimals() throws Exception {
		var dummy = new DummyEvcsPricing();
		// 10 Currency/MWh -> 1.0 ct/kWh < 2.0 threshold -> ceiling applied
		var tariff = DummyTimeOfUseTariffProvider.fromQuarterlyPrices(CLOCK, 10.0, 10.0, 10.0,
				10.0);

		// 3.3333 ct/kWh / 100 = 0.033333 EUR/kWh; rounded to 4 decimals HALF_UP = 0.0333
		var config = MyConfig.create() //
				.setId(CTRL_ID) //
				.setPriceThreshold(PRICE_THRESHOLD) //
				.setCeilingPrice(3.3333) //
				.build();

		var expectedCeiling = 0.0333;

		new ControllerTest(new ControllerEvcsGridPricingImpl()) //
				.addReference("componentManager", new DummyComponentManager(CLOCK)) //
				.addReference("evcsPricing", dummy) //
				.addReference("timeOfUseTariff", tariff) //
				.activate(config) //
				.next(new TestCase() //
						.output(EvcsPricingController.ChannelId.ACTIVE_CEILING, expectedCeiling)) //
				.deactivate();

		assertEquals(expectedCeiling, dummy.getLastCeilingPrice(), DELTA);
	}

	/**
	 * On deactivate(), removeConstraint must be called with the controller ID.
	 */
	@Test
	public void deactivate_removesConstraint() throws Exception {
		var dummy = new DummyEvcsPricing();
		var tariff = DummyTimeOfUseTariffProvider.fromQuarterlyPrices(CLOCK, 10.0, 10.0, 10.0,
				10.0);

		new ControllerTest(new ControllerEvcsGridPricingImpl()) //
				.addReference("componentManager", new DummyComponentManager(CLOCK)) //
				.addReference("evcsPricing", dummy) //
				.addReference("timeOfUseTariff", tariff) //
				.activate(baseConfig()) //
				.next(new TestCase()) //
				.deactivate();

		assertEquals(CTRL_ID, dummy.getLastRemoveConstraintSource());
	}

	/**
	 * When modified() is called with enabled=false, removeConstraint must be called
	 * immediately — before deactivate() — so stale ceilings are not carried over.
	 */
	@Test
	public void disabledViaConfig_removesConstraint() throws Exception {
		var dummy = new DummyEvcsPricing();
		var tariff = DummyTimeOfUseTariffProvider.fromQuarterlyPrices(CLOCK, 10.0, 10.0, 10.0,
				10.0);

		// Assert after modified() but before deactivate() to prove modified() itself
		// calls removeConstraint, not just deactivate()
		var test = new ControllerTest(new ControllerEvcsGridPricingImpl()) //
				.addReference("componentManager", new DummyComponentManager(CLOCK)) //
				.addReference("evcsPricing", dummy) //
				.addReference("timeOfUseTariff", tariff) //
				.activate(baseConfig()) //
				.next(new TestCase()) //
				.modified(disabledConfig());

		assertEquals(CTRL_ID, dummy.getLastRemoveConstraintSource());

		dummy.reset();
		test.deactivate();

		// deactivate also calls removeConstraint
		assertEquals(CTRL_ID, dummy.getLastRemoveConstraintSource());
	}

	/**
	 * resolveNextTick with a future NEXT_PRICE_CHANGE: controller uses the future
	 * timestamp as the window upper bound.
	 *
	 * <p>
	 * With a 30-minute window, two quarters (q0=10, q1=100) are averaged:
	 * (10+100)/2 = 55 Currency/MWh → 5.5 ct/kWh ≥ 2.0 → no ceiling. If the
	 * fallback (+15 min, 1 quarter) were used instead, avg would be 1.0 → ceiling
	 * would be set, proving the future timestamp was actually used.
	 */
	@Test
	public void resolveNextTick_futureTimestamp_usesIt() throws Exception {
		var dummy = new DummyEvcsPricing();
		var now = ZonedDateTime.now(CLOCK);
		var futureMs = now.plusMinutes(30).toInstant().toEpochMilli();
		dummy._setNextPriceChange(futureMs);
		dummy.getNextPriceChangeChannel().nextProcessImage();

		// q0=10, q1=100 — 2-quarter window gives avg=55/10=5.5 > threshold -> no ceiling
		var tariff = DummyTimeOfUseTariffProvider.fromQuarterlyPrices(CLOCK, 10.0, 100.0, 100.0,
				100.0);

		new ControllerTest(new ControllerEvcsGridPricingImpl()) //
				.addReference("componentManager", new DummyComponentManager(CLOCK)) //
				.addReference("evcsPricing", dummy) //
				.addReference("timeOfUseTariff", tariff) //
				.activate(baseConfig()) //
				.next(new TestCase() //
						.output(EvcsPricingController.ChannelId.ACTIVE_CEILING, null)) //
				.deactivate();

		assertNull(dummy.getLastCeilingPrice());
	}

	/**
	 * resolveNextTick with a past NEXT_PRICE_CHANGE: controller falls back to
	 * +15 min window (1 quarter).
	 *
	 * <p>
	 * With q0=10 only in the fallback window, avg = 1.0 ct/kWh &lt; 2.0 → ceiling
	 * is applied. If the past timestamp were used as-is (before nowRounded), no
	 * prices would be in the window and the ceiling would not be set.
	 */
	@Test
	public void resolveNextTick_pastTimestamp_fallsBack() throws Exception {
		var dummy = new DummyEvcsPricing();
		var now = ZonedDateTime.now(CLOCK);
		var pastMs = now.minusHours(1).toInstant().toEpochMilli();
		dummy._setNextPriceChange(pastMs);
		dummy.getNextPriceChangeChannel().nextProcessImage();

		// q0=10 only in the 1-quarter fallback window; avg=1.0 ct/kWh < threshold -> ceiling
		var tariff = DummyTimeOfUseTariffProvider.fromQuarterlyPrices(CLOCK, 10.0, 100.0, 100.0,
				100.0);

		new ControllerTest(new ControllerEvcsGridPricingImpl()) //
				.addReference("componentManager", new DummyComponentManager(CLOCK)) //
				.addReference("evcsPricing", dummy) //
				.addReference("timeOfUseTariff", tariff) //
				.activate(baseConfig()) //
				.next(new TestCase() //
						.output(EvcsPricingController.ChannelId.ACTIVE_CEILING, CEILING_PRICE_EUR)) //
				.deactivate();

		assertEquals(Double.valueOf(CEILING_PRICE_EUR), dummy.getLastCeilingPrice());
	}

	/**
	 * AVERAGE_GRID_PRICE channel reflects the average of non-uniform prices.
	 * q0=20, q1=40 Currency/MWh over a 30-minute window: (20+40)/2 = 30 → 3.0
	 * ct/kWh.
	 */
	@Test
	public void averageGridPriceChannel_reflects() throws Exception {
		var dummy = new DummyEvcsPricing();
		var now = ZonedDateTime.now(CLOCK);
		// Extend window to 30 min so q0=20 and q1=40 are both averaged
		var futureMs = now.plusMinutes(30).toInstant().toEpochMilli();
		dummy._setNextPriceChange(futureMs);
		dummy.getNextPriceChangeChannel().nextProcessImage();

		// (20+40)/2 = 30 Currency/MWh -> 3.0 ct/kWh
		var tariff = DummyTimeOfUseTariffProvider.fromQuarterlyPrices(CLOCK, 20.0, 40.0, 30.0,
				30.0);

		new ControllerTest(new ControllerEvcsGridPricingImpl()) //
				.addReference("componentManager", new DummyComponentManager(CLOCK)) //
				.addReference("evcsPricing", dummy) //
				.addReference("timeOfUseTariff", tariff) //
				.activate(baseConfig()) //
				.next(new TestCase() //
						.output(ControllerEvcsGridPricing.ChannelId.AVERAGE_GRID_PRICE, 3.0)) //
				.deactivate();
	}

	/**
	 * Negative grid price is still below a zero-threshold:
	 * -20 Currency/MWh → -2.0 ct/kWh &lt; 0.0 → ceiling must be set.
	 *
	 * <p>
	 * The default MyConfig.Builder threshold is 0.0; only negative prices trigger
	 * the ceiling in this mode.
	 */
	@Test
	public void negativeGridPrice_setsCeiling() throws Exception {
		var dummy = new DummyEvcsPricing();
		var tariff = DummyTimeOfUseTariffProvider.fromQuarterlyPrices(CLOCK, -20.0, -20.0, -20.0,
				-20.0);

		// threshold=0.0 is the default in the builder; -2.0 < 0.0 triggers the ceiling
		var config = MyConfig.create() //
				.setId(CTRL_ID) //
				.setPriceThreshold(0.0) //
				.setCeilingPrice(CEILING_PRICE) //
				.build();

		new ControllerTest(new ControllerEvcsGridPricingImpl()) //
				.addReference("componentManager", new DummyComponentManager(CLOCK)) //
				.addReference("evcsPricing", dummy) //
				.addReference("timeOfUseTariff", tariff) //
				.activate(config) //
				.next(new TestCase() //
						.output(EvcsPricingController.ChannelId.ACTIVE_CEILING, CEILING_PRICE_EUR)) //
				.deactivate();

		assertEquals(Double.valueOf(CEILING_PRICE_EUR), dummy.getLastCeilingPrice());
	}
}
