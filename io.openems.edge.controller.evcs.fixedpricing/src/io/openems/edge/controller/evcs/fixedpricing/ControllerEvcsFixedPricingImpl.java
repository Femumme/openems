package io.openems.edge.controller.evcs.fixedpricing;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.evcs.pricing.EvcsPricing;
import io.openems.edge.evcs.pricing.EvcsPricingController;

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

	private final Logger log = LoggerFactory.getLogger(ControllerEvcsFixedPricingImpl.class);

	@Reference(target = "(id=" + EvcsPricing.SINGLETON_COMPONENT_ID + ")")
	private EvcsPricing evcsPricing;

	private Config config;

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
	}

	private synchronized void applyConfig(Config config) {
		this.config = config;
		if (this.config.mode() == Mode.MANUAL_ON) {
			this.log.info("Setting EVCS price override to {} EUR/kWh", roundPrice(this.config.priceEurPerKwh()));
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		this.evcsPricing.removeOverride(this.id());
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		if (this.config == null) {
			return;
		}
		switch (this.config.mode()) {
		case MANUAL_ON -> {
			var price = roundPrice(this.config.priceEurPerKwh());
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

	private static double roundPrice(double price) {
		return BigDecimal.valueOf(price)
				.setScale(4, RoundingMode.HALF_UP)
				.doubleValue();
	}
}
