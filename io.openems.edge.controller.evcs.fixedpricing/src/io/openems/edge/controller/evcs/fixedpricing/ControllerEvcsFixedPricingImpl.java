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

/**
 * Controller that sets a fixed EVCS price.
 * 
 * <p>
 * This controller writes to the global {@link EvcsPricing} singleton component.
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Evcs.FixedPricing", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerEvcsFixedPricingImpl extends AbstractOpenemsComponent
		implements ControllerEvcsFixedPricing, Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(ControllerEvcsFixedPricingImpl.class);

	@Reference(target = "(id=" + EvcsPricing.SINGLETON_COMPONENT_ID + ")")
	private EvcsPricing evcsPricing;

	private Config config;

	public ControllerEvcsFixedPricingImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerEvcsFixedPricing.ChannelId.values() //
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
		if (!this.config.enabled()) {
			return;
		}
		var price = roundPrice(this.config.priceEurPerKwh());
		this.log.info("Setting EVCS price to {} €/kWh", price);
		this.evcsPricing._setPrice(price);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		// Continuously set the price every cycle to ensure it's current
		if (this.config != null && this.config.enabled()) {
			this.evcsPricing._setPrice(roundPrice(this.config.priceEurPerKwh()));
		}
	}

	/**
	 * Rounds the price to 4 decimal places to avoid floating point precision
	 * issues from OSGi config storage (float to double conversion).
	 *
	 * @param price the raw price from config
	 * @return the rounded price
	 */
	private static double roundPrice(double price) {
		return BigDecimal.valueOf(price)
				.setScale(4, RoundingMode.HALF_UP)
				.doubleValue();
	}
}
