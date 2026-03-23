package io.openems.edge.controller.evcs.pvpricing;

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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.evcs.pricing.EvcsPricing;
import io.openems.edge.evcs.pricing.EvcsPricingController;
import io.openems.edge.evcs.pricing.util.RollingAverage;

/**
 * Reduces the EVCS price based on PV production using linear interpolation.
 *
 * <p>
 * When PV production (rolling average) exceeds {@code pvThreshold}, a price
 * ceiling is set that decreases linearly from {@code maxCeiling} to
 * {@code minCeiling} as production increases towards {@code pvFullProduction}.
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Evcs.PvPricing", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerEvcsPvPricingImpl extends AbstractOpenemsComponent
		implements ControllerEvcsPvPricing, Controller, OpenemsComponent, EvcsPricingController {

	@Reference(target = "(id=" + EvcsPricing.SINGLETON_COMPONENT_ID + ")")
	private EvcsPricing evcsPricing;

	@Reference(target = "(id=" + Sum.SINGLETON_COMPONENT_ID + ")")
	private Sum sum;

	private Config config;
	private RollingAverage pvAverage;

	public ControllerEvcsPvPricingImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerEvcsPvPricing.ChannelId.values(), //
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

	private void applyConfig(Config config) {
		this.config = config;
		this.pvAverage = new RollingAverage(config.dataCollectionWindowMinutes());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		Integer productionPower;
		try {
			productionPower = this.sum.getProductionActivePower().getOrError();
		} catch (OpenemsNamedException e) {
			this.clearChannels();
			return;
		}
		this.pvAverage.add(productionPower);

		var avgPv = this.pvAverage.getAverage();
		if (avgPv.isEmpty()) {
			this.clearChannels();
			return;
		}

		var avg = avgPv.getAsDouble();

		if (avg < this.config.pvThreshold()) {
			this.clearChannels();
			return;
		}

		var range = this.config.pvFullProduction() - this.config.pvThreshold();
		var ratio = Math.min(1.0, (avg - this.config.pvThreshold()) / range);
		var ceiling = this.config.maxCeiling()
				- ratio * (this.config.maxCeiling() - this.config.minCeiling());
		var price = roundPrice(ceiling);

		this.evcsPricing.addPriceCeiling(this.id(), price);
		this._setActiveCeiling(price);
		this._setActiveFloor(null);
		this._setActiveOverride(null);
	}

	private void clearChannels() {
		this._setActiveCeiling(null);
		this._setActiveFloor(null);
		this._setActiveOverride(null);
	}

	private static double roundPrice(double price) {
		return BigDecimal.valueOf(price)
				.setScale(4, RoundingMode.HALF_UP)
				.doubleValue();
	}
}
