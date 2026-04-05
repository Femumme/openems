package io.openems.edge.controller.evcs.batterypricing;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.evcs.pricing.EvcsPricing;
import io.openems.edge.evcs.pricing.EvcsPricingController;
import io.openems.edge.evcs.pricing.EvcsPricingUtils;
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

	private volatile Config config;
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
		if (!config.enabled()) {
			this.evcsPricing.removeConstraint(this.id());
		}
	}

	private void applyConfig(Config config) {
		if (this.config == null || config.dataCollectionWindowMinutes() != this.config.dataCollectionWindowMinutes()) {
			this.socAverage = new RollingAverage(config.dataCollectionWindowMinutes());
		}
		this.config = config;
	}

	@Override
	@Deactivate
	protected void deactivate() {
		this.evcsPricing.removeConstraint(this.id());
		super.deactivate();
	}

	@Override
	public void run() {
		Integer soc;
		try {
			soc = this.sum.getEssSoc().getOrError();
		} catch (InvalidValueException e) {
			this.clearConstraintChannels();
			return;
		}
		this.socAverage.add(soc);

		var avgSoc = this.socAverage.getAverage();
		if (avgSoc.isEmpty()) {
			this.clearConstraintChannels();
			return;
		}

		var avg = avgSoc.getAsDouble();

		if (avg < this.config.lowSocThreshold()) {
			var price = EvcsPricingUtils.roundPrice(this.config.lowSocFloorPrice());
			this.evcsPricing.addPriceFloor(this.id(), price);
			this._setActiveFloor(price);
			this._setActiveCeiling(null);
			this._setActiveOverride(null);

		} else if (avg >= this.config.highSocThreshold()) {
			var ceiling = EvcsPricingUtils.linearInterpolate(avg, this.config.highSocThreshold(),
					100.0, this.config.highSocCeilPrice(), this.config.fullCeilPrice());
			var price = EvcsPricingUtils.roundPrice(ceiling);
			this.evcsPricing.addPriceCeiling(this.id(), price);
			this._setActiveCeiling(price);
			this._setActiveFloor(null);
			this._setActiveOverride(null);

		} else {
			this.clearConstraintChannels();
		}
	}
}
