package io.openems.edge.evcs.pricing.core;

import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.pricing.EvcsPricing;
import io.openems.edge.evcs.pricing.util.CronExpression;

/**
 * Core singleton that resolves the global EVCS price.
 *
 * <p>
 * Controllers submit constraints (ceilings/floors) or overrides each cycle.
 * Constraint-based prices are locked at a configurable interval. Overrides
 * bypass the interval and take effect immediately. The final price is clamped to
 * {@code [absoluteMinPrice, absoluteMaxPrice]}.
 */
@Designate(ocd = Config.class, factory = false)
@Component(//
		name = EvcsPricing.SINGLETON_SERVICE_PID, //
		immediate = true, //
		property = { //
				"id=" + EvcsPricing.SINGLETON_COMPONENT_ID, //
				"enabled=true" //
		})
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS, //
})
public class EvcsPricingCoreImpl extends AbstractOpenemsComponent
		implements EvcsPricing, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(EvcsPricingCoreImpl.class);

	private final Map<String, Double> ceilings = new HashMap<>();
	private final Map<String, Double> floors = new HashMap<>();
	private final Map<String, Double> overrides = new HashMap<>();

	private CronExpression cronExpression = new CronExpression("0 0 * * * *");
	private double absoluteMinPrice = 0.00;
	private double absoluteMaxPrice = 9.99;

	private Instant nextIntervalTick = Instant.MIN;
	private Double lastOverrideValue = null;

	public EvcsPricingCoreImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				EvcsPricing.ChannelId.values() //
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
		this.cronExpression = new CronExpression(config.cronExpression());
		this.absoluteMinPrice = config.absoluteMinPrice();
		this.absoluteMaxPrice = config.absoluteMaxPrice();
		this.nextIntervalTick = this.cronExpression.nextTick(Instant.now(), ZoneId.systemDefault());
		this._setNextPriceChange(this.nextIntervalTick.toEpochMilli());
		this.log.info("EVCS Pricing Core: cron={}, absolute=[{}, {}]",
				config.cronExpression(), this.absoluteMinPrice, this.absoluteMaxPrice);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS:
			this.ceilings.clear();
			this.floors.clear();
			break;
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS:
			this.resolvePrice();
			break;
		}
	}

	@Override
	public void addPriceCeiling(String source, double maxPrice) {
		this.ceilings.put(source, maxPrice);
	}

	@Override
	public void addPriceFloor(String source, double minPrice) {
		this.floors.put(source, minPrice);
	}

	@Override
	public void setOverride(String source, double price) {
		this.overrides.put(source, price);
	}

	@Override
	public void removeOverride(String source) {
		this.overrides.remove(source);
	}

	@Override
	public void removeConstraint(String source) {
		this.ceilings.remove(source);
		this.floors.remove(source);
	}

	private void resolvePrice() {
		var overridePrice = this.resolveActiveOverride();
		var resolvedPrice = clamp(this.computePrice(overridePrice), this.absoluteMinPrice, this.absoluteMaxPrice);

		this._setNextIntervalPrice(resolvedPrice);
		this.lockPriceIfNeeded(resolvedPrice, overridePrice);
		this.lastOverrideValue = overridePrice;
	}

	private Double resolveActiveOverride() {
		if (this.overrides.isEmpty()) {
			this._setActiveOverrideSource(null);
			this._setActiveOverrideValue(null);
			return null;
		}
		var entry = this.overrides.entrySet().iterator().next();
		this._setActiveOverrideSource(entry.getKey());
		this._setActiveOverrideValue(entry.getValue());
		return entry.getValue();
	}

	private double computePrice(Double overridePrice) {
		return overridePrice != null ? overridePrice : this.resolveConstraints();
	}

	private void lockPriceIfNeeded(double resolvedPrice, Double overridePrice) {
		boolean overrideChanged = !equalsNullable(overridePrice, this.lastOverrideValue);
		var now = Instant.now();
		boolean intervalReached = !now.isBefore(this.nextIntervalTick);

		if (overridePrice != null || overrideChanged || intervalReached) {
			this._setPrice(resolvedPrice);

			if (intervalReached) {
				this.nextIntervalTick = this.cronExpression.nextTick(now, ZoneId.systemDefault());
				this._setNextPriceChange(this.nextIntervalTick.toEpochMilli());
			}
		}
	}

	private double resolveConstraints() {
		var ceiling = this.ceilings.values().stream().min(Double::compareTo);
		var floor = this.floors.values().stream().max(Double::compareTo);

		if (ceiling.isEmpty() && floor.isEmpty()) {
			// No constraints — keep the current locked price unchanged
			return this.getPrice().asOptional().orElse(0.0);
		}
		if (ceiling.isEmpty()) {
			// Floor only — price must meet the floor
			return floor.get();
		}
		// Ceiling present — enforce both: price = max(floor, ceiling)
		// (floor > ceiling is allowed; the floor wins to protect minimum charging revenue)
		return Math.max(floor.orElse(0.0), ceiling.get());
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}

	private static boolean equalsNullable(Double a, Double b) {
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		return a.equals(b);
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
