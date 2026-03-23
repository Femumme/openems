package io.openems.edge.controller.evcs.batterypricing;

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
 * Sets EVCS price constraints based on battery state of charge.
 *
 * <ul>
 * <li>SoC below {@code lowSocThreshold}: sets a price floor (protect battery)
 * <li>SoC above {@code highSocThreshold}: sets a price ceiling that decreases
 * linearly towards 100% (encourage EV charging)
 * <li>SoC between thresholds: no constraint
 * </ul>
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Evcs.BatteryPricing", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerEvcsBatteryPricingImpl extends AbstractOpenemsComponent
		implements ControllerEvcsBatteryPricing, Controller, OpenemsComponent, EvcsPricingController {

	@Reference(target = "(id=" + EvcsPricing.SINGLETON_COMPONENT_ID + ")")
	private EvcsPricing evcsPricing;

	@Reference(target = "(id=" + Sum.SINGLETON_COMPONENT_ID + ")")
	private Sum sum;

	private Config config;
	private RollingAverage socAverage;

	public ControllerEvcsBatteryPricingImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerEvcsBatteryPricing.ChannelId.values(), //
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
		this.socAverage = new RollingAverage(config.dataCollectionWindowMinutes());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() {
		Integer soc;
		try {
			soc = this.sum.getEssSoc().getOrError();
		} catch (OpenemsNamedException e) {
			this.clearChannels();
			return;
		}
		this.socAverage.add(soc);

		var avgSoc = this.socAverage.getAverage();
		if (avgSoc.isEmpty()) {
			this.clearChannels();
			return;
		}

		var avg = avgSoc.getAsDouble();

		if (avg < this.config.lowSocThreshold()) {
			var price = roundPrice(this.config.lowSocFloorPrice());
			this.evcsPricing.addPriceFloor(this.id(), price);
			this._setActiveFloor(price);
			this._setActiveCeiling(null);
			this._setActiveOverride(null);

		} else if (avg >= this.config.highSocThreshold()) {
			var range = 100.0 - this.config.highSocThreshold();
			var ratio = Math.min(1.0, (avg - this.config.highSocThreshold()) / range);
			var ceiling = this.config.highSocCeilPrice()
					- ratio * (this.config.highSocCeilPrice() - this.config.fullCeilPrice());
			var price = roundPrice(ceiling);
			this.evcsPricing.addPriceCeiling(this.id(), price);
			this._setActiveCeiling(price);
			this._setActiveFloor(null);
			this._setActiveOverride(null);

		} else {
			this.clearChannels();
		}
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
