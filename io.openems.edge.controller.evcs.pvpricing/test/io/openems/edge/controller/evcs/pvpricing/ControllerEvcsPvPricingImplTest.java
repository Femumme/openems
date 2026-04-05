package io.openems.edge.controller.evcs.pvpricing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.evcs.pricing.DummyEvcsPricing;
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

	/**
	 * On deactivate(), removeConstraint must be called with the controller ID.
	 */
	@Test
	public void deactivate_removesConstraint() throws Exception {
		var dummy = new DummyEvcsPricing();
		var sum = new DummySum().withProductionActivePower(1000);

		new ControllerTest(new ControllerEvcsPvPricingImpl()) //
				.addReference("evcsPricing", dummy) //
				.addReference("sum", sum) //
				.activate(baseConfig()) //
				.next(new TestCase()) //
				.deactivate();

		assertEquals(CTRL_ID, dummy.getLastRemoveConstraintSource());
	}

	/**
	 * Two-cycle accumulation: RollingAverage must average both samples, not
	 * just the last one.
	 *
	 * <p>
	 * Cycle 1: PV=8000W (full production) → ceiling=minCeiling=0.25.
	 * Cycle 2: PV=500W (at threshold) → if only latest: ceiling=maxCeiling=0.55.
	 * With rolling average of (8000+500)/2=4250W:
	 * ratio=(4250-500)/(8000-500)=0.5 → ceiling=0.55-0.5*(0.55-0.25)=0.40.
	 *
	 * <p>
	 * Window=60 min means both samples are within the window.
	 */
	@Test
	public void twoCycleAccumulation_averagesBothSamples() throws Exception {
		var dummy = new DummyEvcsPricing();

		// Use a 60-minute window so both cycle 1 and cycle 2 samples stay in the window
		var config = MyConfig.create() //
				.setId(CTRL_ID) //
				.setMaxCeiling(MAX_CEILING) //
				.setMinCeiling(MIN_CEILING) //
				.setPvThreshold(PV_THRESHOLD) //
				.setPvFullProduction(PV_FULL_PRODUCTION) //
				.setDataCollectionWindowMinutes(60) //
				.build();

		new ControllerTest(new ControllerEvcsPvPricingImpl()) //
				.addReference("evcsPricing", dummy) //
				.addReference("sum", new DummySum().withProductionActivePower(8000)) //
				.activate(config) //
				.next(new TestCase() //
						// cycle 1: only 8000 in window → ceiling = minCeiling = 0.25
						.output(EvcsPricingController.ChannelId.ACTIVE_CEILING, MIN_CEILING)) //
				.addReference("sum", new DummySum().withProductionActivePower(500)) //
				.next(new TestCase() //
						// cycle 2: avg(8000,500)=4250 → ratio=0.5 → ceiling=0.40
						.output(EvcsPricingController.ChannelId.ACTIVE_CEILING, 0.40)) //
				.deactivate();
	}

	/**
	 * When modified() is called with enabled=false, removeConstraint must be called
	 * immediately — before deactivate() — so stale ceilings are not carried over.
	 */
	@Test
	public void disabledViaConfig_removesConstraint() throws Exception {
		var dummy = new DummyEvcsPricing();
		var sum = new DummySum().withProductionActivePower(1000);

		var test = new ControllerTest(new ControllerEvcsPvPricingImpl()) //
				.addReference("evcsPricing", dummy) //
				.addReference("sum", sum) //
				.activate(baseConfig()) //
				.next(new TestCase()) //
				.modified(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEnabled(false) //
						.setMaxCeiling(MAX_CEILING) //
						.setMinCeiling(MIN_CEILING) //
						.setPvThreshold(PV_THRESHOLD) //
						.setPvFullProduction(PV_FULL_PRODUCTION) //
						.setDataCollectionWindowMinutes(1) //
						.build());

		assertEquals(CTRL_ID, dummy.getLastRemoveConstraintSource());

		dummy.reset();
		test.deactivate();

		assertEquals(CTRL_ID, dummy.getLastRemoveConstraintSource());
	}

	/**
	 * PV above full production: Math.min(1.0, ratio) clamps ratio to 1.0,
	 * so ceiling must equal minCeiling regardless of how far above pvFullProduction.
	 */
	@Test
	public void pvAboveFullProduction_clampsRatioToOne() throws Exception {
		var dummy = new DummyEvcsPricing();
		// 12000W >> pvFullProduction=8000W → ratio would be >1 without clamping
		var sum = new DummySum().withProductionActivePower(12000);

		new ControllerTest(new ControllerEvcsPvPricingImpl()) //
				.addReference("evcsPricing", dummy) //
				.addReference("sum", sum) //
				.activate(baseConfig()) //
				.next(new TestCase() //
						.output(EvcsPricingController.ChannelId.ACTIVE_CEILING, MIN_CEILING)) //
				.deactivate();

		assertEquals(Double.valueOf(MIN_CEILING), dummy.getLastCeilingPrice());
	}
}
