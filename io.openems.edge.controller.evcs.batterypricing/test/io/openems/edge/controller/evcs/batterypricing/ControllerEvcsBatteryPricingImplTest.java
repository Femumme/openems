package io.openems.edge.controller.evcs.batterypricing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.evcs.pricing.EvcsPricingController;

public class ControllerEvcsBatteryPricingImplTest {

	private static final String CTRL_ID = "ctrlEvcsBatteryPricing0";

	// Default config values used across tests
	private static final int LOW_SOC_THRESHOLD = 30;
	private static final int HIGH_SOC_THRESHOLD = 80;
	private static final double LOW_SOC_FLOOR_PRICE = 0.45;
	private static final double HIGH_SOC_CEIL_PRICE = 0.55;
	private static final double FULL_CEIL_PRICE = 0.35;

	/**
	 * Builds the base config with a 1-minute window so a single {@code run()} call
	 * is sufficient to produce a non-empty average.
	 *
	 * @return a {@link MyConfig} with default test values
	 */
	private static MyConfig baseConfig() {
		return MyConfig.create() //
				.setId(CTRL_ID) //
				.setLowSocThreshold(LOW_SOC_THRESHOLD) //
				.setHighSocThreshold(HIGH_SOC_THRESHOLD) //
				.setLowSocFloorPrice(LOW_SOC_FLOOR_PRICE) //
				.setHighSocCeilPrice(HIGH_SOC_CEIL_PRICE) //
				.setFullCeilPrice(FULL_CEIL_PRICE) //
				.setDataCollectionWindowMinutes(1) //
				.build();
	}

	/**
	 * SoC below low threshold: {@code ACTIVE_FLOOR} must be set and
	 * {@code addPriceFloor} must be called with the floor price.
	 */
	@Test
	public void lowSoc_setsFloor() throws Exception {
		var dummy = new DummyEvcsPricing();
		var sum = new DummySum().withEssSoc(20);

		new ControllerTest(new ControllerEvcsBatteryPricingImpl()) //
				.addReference("evcsPricing", dummy) //
				.addReference("sum", sum) //
				.activate(baseConfig()) //
				.next(new TestCase() //
						.output(EvcsPricingController.ChannelId.ACTIVE_FLOOR, LOW_SOC_FLOOR_PRICE)) //
				.deactivate();

		assertEquals(Double.valueOf(LOW_SOC_FLOOR_PRICE), dummy.getLastFloorPrice());
		assertNull(dummy.getLastCeilingPrice());
	}

	/**
	 * SoC between thresholds: no constraint applied, channels must be cleared.
	 */
	@Test
	public void midSoc_clearsChannels() throws Exception {
		var dummy = new DummyEvcsPricing();
		var sum = new DummySum().withEssSoc(50);

		new ControllerTest(new ControllerEvcsBatteryPricingImpl()) //
				.addReference("evcsPricing", dummy) //
				.addReference("sum", sum) //
				.activate(baseConfig()) //
				.next(new TestCase() //
						.output(EvcsPricingController.ChannelId.ACTIVE_FLOOR, null) //
						.output(EvcsPricingController.ChannelId.ACTIVE_CEILING, null)) //
				.deactivate();

		assertNull(dummy.getLastFloorPrice());
		assertNull(dummy.getLastCeilingPrice());
	}

	/**
	 * SoC exactly at high threshold: ratio = 0, ceiling must equal
	 * {@code highSocCeilPrice}.
	 */
	@Test
	public void atHighThreshold_setsCeilingToHighSocCeilPrice() throws Exception {
		var dummy = new DummyEvcsPricing();
		var sum = new DummySum().withEssSoc(80);

		new ControllerTest(new ControllerEvcsBatteryPricingImpl()) //
				.addReference("evcsPricing", dummy) //
				.addReference("sum", sum) //
				.activate(baseConfig()) //
				.next(new TestCase() //
						.output(EvcsPricingController.ChannelId.ACTIVE_CEILING, HIGH_SOC_CEIL_PRICE)) //
				.deactivate();

		assertEquals(Double.valueOf(HIGH_SOC_CEIL_PRICE), dummy.getLastCeilingPrice());
	}

	/**
	 * SoC at 100%: ratio = 1, ceiling must equal {@code fullCeilPrice}.
	 */
	@Test
	public void atFullSoc_setsCeilingToFullCeilPrice() throws Exception {
		var dummy = new DummyEvcsPricing();
		var sum = new DummySum().withEssSoc(100);

		new ControllerTest(new ControllerEvcsBatteryPricingImpl()) //
				.addReference("evcsPricing", dummy) //
				.addReference("sum", sum) //
				.activate(baseConfig()) //
				.next(new TestCase() //
						.output(EvcsPricingController.ChannelId.ACTIVE_CEILING, FULL_CEIL_PRICE)) //
				.deactivate();

		assertEquals(Double.valueOf(FULL_CEIL_PRICE), dummy.getLastCeilingPrice());
	}

	/**
	 * SoC at midpoint between high threshold and 100%: ceiling must be
	 * linearly interpolated.
	 *
	 * <p>
	 * range = 100 - 80 = 20, ratio = (90 - 80) / 20 = 0.5,
	 * ceiling = 0.55 - 0.5 * (0.55 - 0.35) = 0.45.
	 */
	@Test
	public void interpolatedHighSoc_setsInterpolatedCeiling() throws Exception {
		var dummy = new DummyEvcsPricing();
		var sum = new DummySum().withEssSoc(90);

		// expected: 0.55 - 0.5 * (0.55 - 0.35) = 0.4500
		var expectedCeiling = 0.45;

		new ControllerTest(new ControllerEvcsBatteryPricingImpl()) //
				.addReference("evcsPricing", dummy) //
				.addReference("sum", sum) //
				.activate(baseConfig()) //
				.next(new TestCase() //
						.output(EvcsPricingController.ChannelId.ACTIVE_CEILING, expectedCeiling)) //
				.deactivate();

		assertEquals(Double.valueOf(expectedCeiling), dummy.getLastCeilingPrice());
	}

	/**
	 * Missing SoC value: channels are cleared and no exception escapes.
	 *
	 * <p>
	 * When no SoC is set on the {@link DummySum}, the controller catches the
	 * unavailable-value error internally, clears all pricing channels, and returns
	 * without throwing.
	 */
	@Test
	public void missingSoc_clearsChannels_noException() throws Exception {
		var dummy = new DummyEvcsPricing();
		var sum = new DummySum();

		new ControllerTest(new ControllerEvcsBatteryPricingImpl()) //
				.addReference("evcsPricing", dummy) //
				.addReference("sum", sum) //
				.activate(baseConfig()) //
				.next(new TestCase() //
						.output(EvcsPricingController.ChannelId.ACTIVE_FLOOR, null) //
						.output(EvcsPricingController.ChannelId.ACTIVE_CEILING, null)) //
				.deactivate();

		assertNull(dummy.getLastFloorPrice());
		assertNull(dummy.getLastCeilingPrice());
	}
}
