package io.openems.edge.controller.evcs.gridpricing;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
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
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

/**
 * Sets EVCS price ceiling when average grid price is below a configured
 * threshold.
 *
 * <p>
 * Prices are sourced from an optional {@link TimeOfUseTariff} reference and
 * averaged over the lookahead window (now → next price change). When the
 * average falls below {@code priceThreshold} (ct/kWh), a ceiling of
 * {@code ceilingPrice} (ct/kWh) is submitted to {@link EvcsPricing}.
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Evcs.GridPricing", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerEvcsGridPricingImpl extends AbstractOpenemsComponent
		implements ControllerEvcsGridPricing, Controller, OpenemsComponent, EvcsPricingController {

	@Reference(target = "(id=" + EvcsPricing.SINGLETON_COMPONENT_ID + ")")
	private EvcsPricing evcsPricing;

	@Reference
	private ComponentManager componentManager;

	// Dynamic optional — may disappear at runtime when the tariff bundle stops
	@Reference(policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.OPTIONAL)
	private volatile TimeOfUseTariff timeOfUseTariff;

	private Config config;

	public ControllerEvcsGridPricingImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerEvcsGridPricing.ChannelId.values(), //
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
		// Remove stale constraint immediately when the component is disabled so the
		// pricing core does not carry it over to the next cycle
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
		var tariff = this.timeOfUseTariff;
		if (tariff == null) {
			this.clearChannels();
			return;
		}

		var prices = tariff.getPrices();
		if (prices.isEmpty()) {
			this.clearChannels();
			return;
		}

		var now = ZonedDateTime.now(this.componentManager.getClock());
		var nowRounded = roundDownToQuarter(now);
		var nextTick = this.resolveNextTick(nowRounded);

		var avgOpt = prices.getBetween(nowRounded, nextTick) //
				.mapToDouble(Double::doubleValue) //
				.average();

		if (avgOpt.isEmpty()) {
			this.clearChannels();
			return;
		}

		// TimeOfUseTariff prices are in Currency/MWh; divide by 10 to get ct/kWh
		var avgCtKwh = avgOpt.getAsDouble() / 10.0;
		this._setAverageGridPrice(avgCtKwh);

		if (avgCtKwh < this.config.priceThreshold()) {
			this.applyCeiling();
		} else {
			this.clearConstraintChannels();
		}
	}

	/**
	 * Determines the upper bound of the lookahead window.
	 *
	 * <p>
	 * Uses the next price-change timestamp from {@link EvcsPricing} when available,
	 * but always guarantees at least one 15-minute quarter forward so
	 * {@link io.openems.edge.common.type.QuarterlyValues#getBetween} returns a
	 * non-empty stream.
	 *
	 * @param nowRounded the current time truncated to the nearest quarter-hour
	 * @return the next tick timestamp to use as the lookahead window upper bound
	 */
	private ZonedDateTime resolveNextTick(ZonedDateTime nowRounded) {
		var nextPriceChangeMs = this.evcsPricing.getNextPriceChange().orElse(null);
		var minimumNextTick = nowRounded.plusMinutes(15);

		if (nextPriceChangeMs == null) {
			return minimumNextTick;
		}

		var nextTick = Instant.ofEpochMilli(nextPriceChangeMs).atZone(nowRounded.getZone());
		return nextTick.isAfter(nowRounded) ? nextTick : minimumNextTick;
	}

	/**
	 * Submits a ceiling price and updates the controller's own channels.
	 *
	 * <p>
	 * Converts ct/kWh → EUR/kWh before submission.
	 */
	private void applyCeiling() {
		// ct/kWh ÷ 100 → EUR/kWh
		var priceEurKwh = roundPrice(this.config.ceilingPrice() / 100.0);
		this.evcsPricing.addPriceCeiling(this.id(), priceEurKwh);
		this._setActiveCeiling(priceEurKwh);
		this._setActiveFloor(null);
		this._setActiveOverride(null);
	}

	/**
	 * Nulls the constraint channels without touching AVERAGE_GRID_PRICE.
	 *
	 * <p>
	 * Called when no constraint is being set but prices were still computed.
	 */
	private void clearConstraintChannels() {
		this._setActiveCeiling(null);
		this._setActiveFloor(null);
		this._setActiveOverride(null);
	}

	/**
	 * Nulls all channels including AVERAGE_GRID_PRICE.
	 *
	 * <p>
	 * Called when no prices are available at all.
	 */
	private void clearChannels() {
		this.clearConstraintChannels();
		this._setAverageGridPrice(null);
	}

	// Consistent with ControllerEvcsBatteryPricingImpl.roundPrice
	private static double roundPrice(double price) {
		return BigDecimal.valueOf(price) //
				.setScale(4, RoundingMode.HALF_UP) //
				.doubleValue();
	}
}
