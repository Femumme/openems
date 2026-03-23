package io.openems.edge.controller.evcs.pvpricing;

import static org.junit.Assert.assertNull;

import org.junit.Test;

import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.evcs.pricing.EvcsPricingController;

public class ControllerEvcsPvPricingImplTest {

	private static final String CTRL_ID = "ctrlEvcsPvPricing0";

	// Default config values used across tests
	private static final double MAX_CEILING = 0.55;
	private static final double MIN_CEILING = 0.25;
	private static final int PV_THRESHOLD = 500;
	private static final int PV_FULL_PRODUCTION = 8000;

	/**
	 * Builds the base config with a 1-minute window so a single {@code run()} call
	 * is sufficient to produce a non-empty average.
	 *
	 * @return a {@link MyConfig} with default test values
	 */
	private static MyConfig baseConfig() {
		return MyConfig.create() //
				.setId(CTRL_ID) //
				.setMaxCeiling(MAX_CEILING) //
				.setMinCeiling(MIN_CEILING) //
				.setPvThreshold(PV_THRESHOLD) //
				.setPvFullProduction(PV_FULL_PRODUCTION) //
				.setDataCollectionWindowMinutes(1) //
				.build();
	}

	/**
	 * PV below threshold: {@code ACTIVE_CEILING} must remain null and
	 * {@code addPriceCeiling} must not be called.
	 */
	@Test
	public void belowThreshold_doesNotSetCeiling() throws Exception {
		var dummy = new DummyEvcsPricing();
		var sum = new DummySum().withProductionActivePower(499);

		new ControllerTest(new ControllerEvcsPvPricingImpl()) //
				.addReference("evcsPricing", dummy) //
				.addReference("sum", sum) //
				.activate(baseConfig()) //
				.next(new TestCase() //
						.output(EvcsPricingController.ChannelId.ACTIVE_CEILING, null)) //
				.deactivate();

		assertNull(dummy.getLastCeilingPrice());
	}

	/**
	 * PV exactly at threshold: ratio = 0, ceiling must equal {@code maxCeiling}.
	 */
	@Test
	public void atThreshold_setsCeilingToMax() throws Exception {
		var dummy = new DummyEvcsPricing();
		var sum = new DummySum().withProductionActivePower(500);

		new ControllerTest(new ControllerEvcsPvPricingImpl()) //
				.addReference("evcsPricing", dummy) //
				.addReference("sum", sum) //
				.activate(baseConfig()) //
				.next(new TestCase() //
						.output(EvcsPricingController.ChannelId.ACTIVE_CEILING, MAX_CEILING)) //
				.deactivate();
	}

	/**
	 * PV at full production: ratio = 1, ceiling must equal {@code minCeiling}.
	 */
	@Test
	public void atFullProduction_setsCeilingToMin() throws Exception {
		var dummy = new DummyEvcsPricing();
		var sum = new DummySum().withProductionActivePower(8000);

		new ControllerTest(new ControllerEvcsPvPricingImpl()) //
				.addReference("evcsPricing", dummy) //
				.addReference("sum", sum) //
				.activate(baseConfig()) //
				.next(new TestCase() //
						.output(EvcsPricingController.ChannelId.ACTIVE_CEILING, MIN_CEILING)) //
				.deactivate();
	}

	/**
	 * PV at midpoint produces a linearly interpolated ceiling.
	 *
	 * <p>
	 * ratio = (4250 - 500) / (8000 - 500) = 3750 / 7500 = 0.5, so ceiling =
	 * 0.55 - 0.5 * (0.55 - 0.25) = 0.40.
	 */
	@Test
	public void interpolatedMidpoint_setsInterpolatedCeiling() throws Exception {
		var dummy = new DummyEvcsPricing();
		var sum = new DummySum().withProductionActivePower(4250);

		// expected: 0.55 - 0.5 * (0.55 - 0.25) = 0.4000
		var expectedCeiling = 0.4000;

		new ControllerTest(new ControllerEvcsPvPricingImpl()) //
				.addReference("evcsPricing", dummy) //
				.addReference("sum", sum) //
				.activate(baseConfig()) //
				.next(new TestCase() //
						.output(EvcsPricingController.ChannelId.ACTIVE_CEILING, expectedCeiling)) //
				.deactivate();
	}

	/**
	 * Missing PV value: channels are cleared and no exception escapes.
	 *
	 * <p>
	 * When no production power is set on the {@link DummySum}, the controller
	 * catches the unavailable-value error internally, clears all pricing channels,
	 * and returns without throwing.
	 */
	@Test
	public void missingPvValue_clearsChannels_noException() throws Exception {
		var dummy = new DummyEvcsPricing();
		var sum = new DummySum();

		new ControllerTest(new ControllerEvcsPvPricingImpl()) //
				.addReference("evcsPricing", dummy) //
				.addReference("sum", sum) //
				.activate(baseConfig()) //
				.next(new TestCase() //
						.output(EvcsPricingController.ChannelId.ACTIVE_CEILING, null)) //
				.deactivate();

		assertNull(dummy.getLastCeilingPrice());
	}
}
