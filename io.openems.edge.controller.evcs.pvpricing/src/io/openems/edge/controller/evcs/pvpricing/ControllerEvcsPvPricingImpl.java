package io.openems.edge.controller.evcs.pvpricing;

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
import io.openems.edge.evcs.pricing.EvcsPricingUtils;
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

	private volatile Config config;
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
		if (!config.enabled()) {
			this.evcsPricing.removeConstraint(this.id());
		}
	}

	private void applyConfig(Config config) {
		if (this.config == null || config.dataCollectionWindowMinutes() != this.config.dataCollectionWindowMinutes()) {
			this.pvAverage = new RollingAverage(config.dataCollectionWindowMinutes());
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
	public void run() throws OpenemsNamedException {
		Integer productionPower;
		try {
			productionPower = this.sum.getProductionActivePower().getOrError();
		} catch (OpenemsNamedException e) {
			this.clearConstraintChannels();
			return;
		}
		this.pvAverage.add(productionPower);

		var avgPv = this.pvAverage.getAverage();
		if (avgPv.isEmpty()) {
			this.clearConstraintChannels();
			return;
		}

		var avg = avgPv.getAsDouble();

		if (avg < this.config.pvThreshold()) {
			this.clearConstraintChannels();
			return;
		}

		var ceiling = EvcsPricingUtils.linearInterpolate(avg, this.config.pvThreshold(),
				this.config.pvFullProduction(), this.config.maxCeiling(), this.config.minCeiling());
		var price = EvcsPricingUtils.roundPrice(ceiling);

		this.evcsPricing.addPriceCeiling(this.id(), price);
		this._setActiveCeiling(price);
		this._setActiveFloor(null);
		this._setActiveOverride(null);
	}
}
