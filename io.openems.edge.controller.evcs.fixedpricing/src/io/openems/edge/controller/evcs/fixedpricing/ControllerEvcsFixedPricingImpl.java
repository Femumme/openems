package io.openems.edge.controller.evcs.fixedpricing;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.evcs.pricing.EvcsPricing;
import io.openems.edge.evcs.pricing.EvcsPricingController;
import io.openems.edge.evcs.pricing.EvcsPricingUtils;

/**
 * Controller that sets a fixed EVCS price as an immediate override.
 *
 * <p>
 * While active, this override bypasses interval-based constraint resolution.
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Evcs.FixedPricing", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerEvcsFixedPricingImpl extends AbstractOpenemsComponent
		implements ControllerEvcsFixedPricing, Controller, OpenemsComponent, EvcsPricingController {

	@Reference(target = "(id=" + EvcsPricing.SINGLETON_COMPONENT_ID + ")")
	private EvcsPricing evcsPricing;

	private volatile Config config;

	public ControllerEvcsFixedPricingImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerEvcsFixedPricing.ChannelId.values(), //
				EvcsPricingController.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.applyConfig(config);
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		super.modified(context, config.id(), config.alias(), config.enabled());
		this.applyConfig(config);
		if (!config.enabled()) {
			this.evcsPricing.removeOverride(this.id());
		}
	}

	private void applyConfig(Config config) {
		this.config = config;
	}

	@Override
	@Deactivate
	protected void deactivate() {
		this.evcsPricing.removeOverride(this.id());
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		switch (this.config.mode()) {
		case MANUAL_ON -> {
			var price = EvcsPricingUtils.roundPrice(this.config.priceEurPerKwh());
			this.evcsPricing.setOverride(this.id(), price);
			this._setActiveOverride(price);
			this._setActiveCeiling(null);
			this._setActiveFloor(null);
		}
		case MANUAL_OFF -> {
			this.evcsPricing.removeOverride(this.id());
			this._setActiveOverride(null);
		}
		}
	}
}
