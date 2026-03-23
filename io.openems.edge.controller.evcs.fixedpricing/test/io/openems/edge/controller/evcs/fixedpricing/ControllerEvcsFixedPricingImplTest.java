package io.openems.edge.controller.evcs.fixedpricing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.evcs.pricing.DummyEvcsPricing;
import io.openems.edge.evcs.pricing.EvcsPricingController;

public class ControllerEvcsFixedPricingImplTest {

	private static final String CTRL_ID = "ctrlEvcsFixedPricing0";
	private static final double PRICE = 0.35;
	// Price with more than 4 significant decimals to exercise rounding (HALF_UP, scale=4)
	private static final double PRICE_UNROUNDED = 0.123456;
	private static final double PRICE_ROUNDED = 0.1235;

	/**
	 * After {@code run()}, {@code setOverride} must be called with the price
	 * rounded to 4 decimal places (HALF_UP) and {@code ACTIVE_OVERRIDE} channel
	 * must reflect that rounded value.
	 */
	@Test
	public void setsOverride_onRun() throws Exception {
		var dummy = new DummyEvcsPricing();

		new ControllerTest(new ControllerEvcsFixedPricingImpl()) //
				.addReference("evcsPricing", dummy) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setPriceEurPerKwh(PRICE_UNROUNDED) //
						.build()) //
				.next(new TestCase() //
						.output(EvcsPricingController.ChannelId.ACTIVE_OVERRIDE, PRICE_ROUNDED)) //
				.deactivate();

		assertEquals(CTRL_ID, dummy.getLastSetOverrideSource());
		assertEquals(PRICE_ROUNDED, dummy.getLastSetOverridePrice(), 1e-9);
	}

	/**
	 * {@code deactivate()} must call {@code removeOverride} with the controller ID.
	 */
	@Test
	public void deactivate_removesOverride() throws Exception {
		var dummy = new DummyEvcsPricing();

		new ControllerTest(new ControllerEvcsFixedPricingImpl()) //
				.addReference("evcsPricing", dummy) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setPriceEurPerKwh(PRICE) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();

		assertEquals(CTRL_ID, dummy.getLastRemoveOverrideSource());
	}

	/**
	 * When {@code mode=MANUAL_OFF}, {@code run()} must not call {@code setOverride}
	 * and {@code ACTIVE_OVERRIDE} must remain null.
	 */
	@Test
	public void mode_off_doesNotSetOverride() throws Exception {
		var dummy = new DummyEvcsPricing();

		new ControllerTest(new ControllerEvcsFixedPricingImpl()) //
				.addReference("evcsPricing", dummy) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setMode(Mode.MANUAL_OFF) //
						.setPriceEurPerKwh(PRICE) //
						.build()) //
				.next(new TestCase() //
						.output(EvcsPricingController.ChannelId.ACTIVE_OVERRIDE, null)) //
				.deactivate();

		assertNull(dummy.getLastSetOverridePrice());
	}

	/**
	 * When {@code mode} is changed from {@code MANUAL_OFF} to {@code MANUAL_ON},
	 * the subsequent {@code run()} must set the override.
	 */
	@Test
	public void mode_switch_to_manual_on_setsOverride() throws Exception {
		var dummy = new DummyEvcsPricing();

		new ControllerTest(new ControllerEvcsFixedPricingImpl()) //
				.addReference("evcsPricing", dummy) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setMode(Mode.MANUAL_OFF) //
						.setPriceEurPerKwh(PRICE) //
						.build()) //
				.next(new TestCase() //
						.output(EvcsPricingController.ChannelId.ACTIVE_OVERRIDE, null)) //
				.modified(MyConfig.create() //
						.setId(CTRL_ID) //
						.setMode(Mode.MANUAL_ON) //
						.setPriceEurPerKwh(PRICE) //
						.build()) //
				.next(new TestCase() //
						.output(EvcsPricingController.ChannelId.ACTIVE_OVERRIDE, PRICE)) //
				.deactivate();

		assertEquals(CTRL_ID, dummy.getLastSetOverrideSource());
	}
}
