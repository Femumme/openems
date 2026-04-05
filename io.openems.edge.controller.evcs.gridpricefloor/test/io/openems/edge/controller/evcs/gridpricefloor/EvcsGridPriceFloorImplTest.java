package io.openems.edge.controller.evcs.gridpricefloor;

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

public class EvcsGridPriceFloorImplTest {

	private static final String CTRL_ID = "ctrlEvcsGridPriceFloor0";
	private static final TimeLeapClock CLOCK = createDummyClock();
	private static final double MARGIN = 2.0; // ct/kWh
	private static final double DELTA = 1e-6;

	/**
	 * Builds a config with the default margin.
	 *
	 * @return a {@link MyConfig} with MARGIN applied
	 */
	private static MyConfig baseConfig() {
		return MyConfig.create() //
				.setId(CTRL_ID) //
				.setAlias("Test Grid Price Floor Controller") //
				.setMargin(MARGIN) //
				.build();
	}

	/**
	 * Builds a disabled config keeping the same margin.
	 *
	 * @return a {@link MyConfig} with enabled set to false
	 */
	private static MyConfig disabledConfig() {
		return MyConfig.create() //
				.setId(CTRL_ID) //
				.setAlias("Test Grid Price Floor Controller") //
				.setEnabled(false) //
				.setMargin(MARGIN) //
				.build();
	}

	/**
	 * avg grid price 8.0 ct/kWh + margin 2.0 = 10.0 ct/kWh → 0.10 EUR/kWh
	 * submitted.
	 */
	@Test
	public void withPrices_setsFloor() throws Exception {
		var dummy = new DummyEvcsPricing();
		// 80 Currency/MWh ÷ 10 = 8.0 ct/kWh + 2.0 margin = 10.0 ct/kWh → 0.10 EUR/kWh
		var tariff = DummyTimeOfUseTariffProvider.fromQuarterlyPrices(CLOCK, 80.0, 80.0, 80.0, 80.0);

		new ControllerTest(new EvcsGridPriceFloorImpl()) //
				.addReference("componentManager", new DummyComponentManager(CLOCK)) //
				.addReference("evcsPricing", dummy) //
				.addReference("timeOfUseTariff", tariff) //
				.activate(baseConfig()) //
				.next(new TestCase() //
						.output(EvcsPricingController.ChannelId.ACTIVE_FLOOR, 0.10) //
						.output(EvcsPricingController.ChannelId.ACTIVE_CEILING, null) //
						.output(EvcsPricingController.ChannelId.ACTIVE_OVERRIDE, null)) //
				.deactivate();

		assertEquals(0.10, dummy.getLastFloorPrice(), DELTA);
		assertEquals(CTRL_ID, dummy.getLastFloorSource());
	}

	/**
	 * negative grid price: -3.0 ct/kWh + 2.0 margin = -1.0 ct/kWh → -0.01 EUR/kWh.
	 */
	@Test
	public void negativePrices_setsNegativeFloor() throws Exception {
		var dummy = new DummyEvcsPricing();
		// -30 Currency/MWh ÷ 10 = -3.0 ct/kWh + 2.0 margin = -1.0 ct/kWh → -0.01 EUR/kWh
		var tariff = DummyTimeOfUseTariffProvider.fromQuarterlyPrices(CLOCK, -30.0, -30.0, -30.0, -30.0);

		new ControllerTest(new EvcsGridPriceFloorImpl()) //
				.addReference("componentManager", new DummyComponentManager(CLOCK)) //
				.addReference("evcsPricing", dummy) //
				.addReference("timeOfUseTariff", tariff) //
				.activate(baseConfig()) //
				.next(new TestCase() //
						.output(EvcsPricingController.ChannelId.ACTIVE_FLOOR, -0.01)) //
				.deactivate();

		assertEquals(-0.01, dummy.getLastFloorPrice(), DELTA);
	}

	/**
	 * no prices → no floor submitted, channels cleared.
	 */
	@Test
	public void emptyPrices_noFloor() throws Exception {
		var dummy = new DummyEvcsPricing();
		var tariff = DummyTimeOfUseTariffProvider.empty(CLOCK);

		new ControllerTest(new EvcsGridPriceFloorImpl()) //
				.addReference("componentManager", new DummyComponentManager(CLOCK)) //
				.addReference("evcsPricing", dummy) //
				.addReference("timeOfUseTariff", tariff) //
				.activate(baseConfig()) //
				.next(new TestCase() //
						.output(EvcsPricingController.ChannelId.ACTIVE_FLOOR, null) //
						.output(EvcsPricingController.ChannelId.ACTIVE_CEILING, null) //
						.output(EvcsPricingController.ChannelId.ACTIVE_OVERRIDE, null)) //
				.deactivate();

		assertNull(dummy.getLastFloorPrice());
	}

	/**
	 * no tariff provider → no floor, channels cleared.
	 */
	@Test
	public void noTariffProvider_noFloor() throws Exception {
		var dummy = new DummyEvcsPricing();

		new ControllerTest(new EvcsGridPriceFloorImpl()) //
				.addReference("componentManager", new DummyComponentManager(CLOCK)) //
				.addReference("evcsPricing", dummy) //
				.activate(baseConfig()) //
				.next(new TestCase() //
						.output(EvcsPricingController.ChannelId.ACTIVE_FLOOR, null) //
						.output(EvcsPricingController.ChannelId.ACTIVE_CEILING, null) //
						.output(EvcsPricingController.ChannelId.ACTIVE_OVERRIDE, null) //
						.output(EvcsGridPriceFloor.ChannelId.AVERAGE_GRID_PRICE, null)) //
				.deactivate();

		assertNull(dummy.getLastFloorPrice());
	}

	/**
	 * 50 Currency/MWh ÷ 10 = 5.0 ct/kWh stored in AVERAGE_GRID_PRICE.
	 */
	@Test
	public void unitConversion_currencyMwhToCtKwh() throws Exception {
		var dummy = new DummyEvcsPricing();
		var tariff = DummyTimeOfUseTariffProvider.fromQuarterlyPrices(CLOCK, 50.0, 50.0, 50.0, 50.0);

		// margin=0.0 so no offset is applied — pure conversion check
		var config = MyConfig.create() //
				.setId(CTRL_ID) //
				.setMargin(0.0) //
				.build();

		new ControllerTest(new EvcsGridPriceFloorImpl()) //
				.addReference("componentManager", new DummyComponentManager(CLOCK)) //
				.addReference("evcsPricing", dummy) //
				.addReference("timeOfUseTariff", tariff) //
				.activate(config) //
				.next(new TestCase() //
						.output(EvcsGridPriceFloor.ChannelId.AVERAGE_GRID_PRICE, 5.0)) //
				.deactivate();
	}

	/**
	 * margin 1.33333 → floor 9.33333 ct/kWh → 0.0933333 EUR/kWh → rounded to
	 * 0.0933.
	 */
	@Test
	public void roundPrice_roundsFloorTo4Decimals() throws Exception {
		var dummy = new DummyEvcsPricing();
		// 80 Currency/MWh → 8.0 ct/kWh + 1.33333 margin = 9.33333 ct/kWh
		// 9.33333 ÷ 100 = 0.0933333 EUR/kWh → HALF_UP 4dp = 0.0933
		var tariff = DummyTimeOfUseTariffProvider.fromQuarterlyPrices(CLOCK, 80.0, 80.0, 80.0, 80.0);

		var config = MyConfig.create() //
				.setId(CTRL_ID) //
				.setMargin(1.33333) //
				.build();

		new ControllerTest(new EvcsGridPriceFloorImpl()) //
				.addReference("componentManager", new DummyComponentManager(CLOCK)) //
				.addReference("evcsPricing", dummy) //
				.addReference("timeOfUseTariff", tariff) //
				.activate(config) //
				.next(new TestCase()) //
				.deactivate();

		assertEquals(0.0933, dummy.getLastFloorPrice(), DELTA);
	}

	/**
	 * On deactivate(), removeConstraint must be called with the controller ID.
	 */
	@Test
	public void deactivate_removesConstraint() throws Exception {
		var dummy = new DummyEvcsPricing();
		var tariff = DummyTimeOfUseTariffProvider.fromQuarterlyPrices(CLOCK, 80.0, 80.0, 80.0, 80.0);

		new ControllerTest(new EvcsGridPriceFloorImpl()) //
				.addReference("componentManager", new DummyComponentManager(CLOCK)) //
				.addReference("evcsPricing", dummy) //
				.addReference("timeOfUseTariff", tariff) //
				.activate(baseConfig()) //
				.next(new TestCase()) //
				.deactivate();

		assertEquals(CTRL_ID, dummy.getLastRemoveConstraintSource());
		assertEquals(CTRL_ID, dummy.getLastFloorSource());
	}

	/**
	 * When modified() is called with enabled=false, removeConstraint is called
	 * immediately — before deactivate() — so stale floors are not carried over.
	 */
	@Test
	public void disabledViaConfig_removesConstraint() throws Exception {
		var dummy = new DummyEvcsPricing();
		var tariff = DummyTimeOfUseTariffProvider.fromQuarterlyPrices(CLOCK, 80.0, 80.0, 80.0, 80.0);

		// Assert after modified() but before deactivate() to prove modified() itself
		// calls removeConstraint, not just deactivate()
		var test = new ControllerTest(new EvcsGridPriceFloorImpl()) //
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
	 * multi-quarter average: (20+40)/2=30 Currency/MWh → 3.0 ct/kWh.
	 */
	@Test
	public void averageGridPriceChannel_reflectsMultipleQuarters() throws Exception {
		var dummy = new DummyEvcsPricing();
		var now = ZonedDateTime.now(CLOCK);
		// Extend window to 30 min so q0=20 and q1=40 are both included in the average
		var futureMs = now.plusMinutes(30).toInstant().toEpochMilli();
		dummy._setNextPriceChange(futureMs);
		dummy.getNextPriceChangeChannel().nextProcessImage();

		// (20+40)/2 = 30 Currency/MWh → 30 ÷ 10 = 3.0 ct/kWh
		var tariff = DummyTimeOfUseTariffProvider.fromQuarterlyPrices(CLOCK, 20.0, 40.0, 30.0, 30.0);

		new ControllerTest(new EvcsGridPriceFloorImpl()) //
				.addReference("componentManager", new DummyComponentManager(CLOCK)) //
				.addReference("evcsPricing", dummy) //
				.addReference("timeOfUseTariff", tariff) //
				.activate(baseConfig()) //
				.next(new TestCase() //
						.output(EvcsGridPriceFloor.ChannelId.AVERAGE_GRID_PRICE, 3.0)) //
				.deactivate();
	}

	/**
	 * NEXT_PRICE_CHANGE only 5 min away → minimum 15-min window enforced → only
	 * current quarter (80 Currency/MWh → 8.0 ct/kWh) is averaged → floor = 10.0
	 * ct/kWh = 0.10 EUR/kWh.
	 */
	@Test
	public void nextPriceChangeLessThan15Min_enforcesMinimumWindow() throws Exception {
		var dummy = new DummyEvcsPricing();
		var now = ZonedDateTime.now(CLOCK);
		// 5 min is strictly less than the 15-min minimum window
		var nearFutureMs = now.plusMinutes(5).toInstant().toEpochMilli();
		dummy._setNextPriceChange(nearFutureMs);
		dummy.getNextPriceChangeChannel().nextProcessImage();

		// 80 Currency/MWh → 8.0 ct/kWh + 2.0 margin = 10.0 ct/kWh → 0.10 EUR/kWh
		var tariff = DummyTimeOfUseTariffProvider.fromQuarterlyPrices(CLOCK, 80.0, 80.0, 80.0, 80.0);

		new ControllerTest(new EvcsGridPriceFloorImpl()) //
				.addReference("componentManager", new DummyComponentManager(CLOCK)) //
				.addReference("evcsPricing", dummy) //
				.addReference("timeOfUseTariff", tariff) //
				.activate(baseConfig()) //
				.next(new TestCase() //
						.output(EvcsPricingController.ChannelId.ACTIVE_FLOOR, 0.10)) //
				.deactivate();

		assertEquals(0.10, dummy.getLastFloorPrice(), DELTA);
	}
}
