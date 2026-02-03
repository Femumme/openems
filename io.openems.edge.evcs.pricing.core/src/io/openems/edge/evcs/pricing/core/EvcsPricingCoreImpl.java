package io.openems.edge.evcs.pricing.core;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evcs.pricing.EvcsPricing;

/**
 * Core singleton component that holds the global EVCS price.
 * 
 * <p>
 * Controllers (like FixedPricing, AutomaticPricing) write to this component's
 * Price channel. Exporters (like Eaaze) read from it and export to backends.
 */
@Designate(ocd = Config.class, factory = false)
@Component(//
		name = EvcsPricing.SINGLETON_SERVICE_PID, //
		immediate = true, //
		property = { //
				"id=" + EvcsPricing.SINGLETON_COMPONENT_ID, //
				"enabled=true" //
		})
public class EvcsPricingCoreImpl extends AbstractOpenemsComponent implements EvcsPricing, OpenemsComponent {

	public EvcsPricingCoreImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				EvcsPricing.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public String debugLog() {
		var price = this.getPrice().asOptional();
		if (price.isPresent()) {
			return String.format("%.4f €/kWh", price.get());
		}
		return "no price set";
	}
}

