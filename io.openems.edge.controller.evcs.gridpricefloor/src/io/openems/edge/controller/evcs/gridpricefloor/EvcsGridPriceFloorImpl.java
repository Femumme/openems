package io.openems.edge.controller.evcs.gridpricefloor;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;

import java.time.ZonedDateTime;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.evcs.pricing.EvcsPricing;
import io.openems.edge.evcs.pricing.EvcsPricingController;
import io.openems.edge.evcs.pricing.EvcsPricingUtils;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

/**
 * Sets the forecasted average grid electricity price as a price floor in EvcsPricing.
 *
 * <p>
 * Prices are sourced from an optional {@link TimeOfUseTariff} reference and
 * averaged over the lookahead window (now → next price change). The configured
 * {@code margin} (ct/kWh) is added to the average before submission to
 * {@link EvcsPricing}.
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Evcs.GridPriceFloor", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class EvcsGridPriceFloorImpl extends AbstractOpenemsComponent
		implements EvcsGridPriceFloor, Controller, OpenemsComponent, EvcsPricingController {

	@Reference(target = "(id=" + EvcsPricing.SINGLETON_COMPONENT_ID + ")")
	private EvcsPricing evcsPricing;

	@Reference
	private ComponentManager componentManager;

	@Reference(policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.OPTIONAL)
	private volatile TimeOfUseTariff timeOfUseTariff;

	private volatile Config config;

	public EvcsGridPriceFloorImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				EvcsGridPriceFloor.ChannelId.values(), //
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
		var now = ZonedDateTime.now(this.componentManager.getClock());
		var nowRounded = roundDownToQuarter(now);
		var avgOpt = EvcsPricingUtils.computeAverageCtKwh(this.timeOfUseTariff, nowRounded, this.evcsPricing);

		if (avgOpt.isEmpty()) {
			this.clearChannels();
			return;
		}

		var avgCtKwh = avgOpt.getAsDouble();
		this._setAverageGridPrice(avgCtKwh);

		this.applyFloor(avgCtKwh);
	}

	private void applyFloor(double avgCtKwh) {
		var floorCtKwh = avgCtKwh + this.config.margin();
		var floorEurKwh = EvcsPricingUtils.roundPrice(floorCtKwh / 100.0);
		this.evcsPricing.addPriceFloor(this.id(), floorEurKwh);
		this._setActiveFloor(floorEurKwh);
		this._setActiveCeiling(null);
		this._setActiveOverride(null);
	}

	private void clearChannels() {
		this.clearConstraintChannels();
		this._setAverageGridPrice(null);
	}

}
