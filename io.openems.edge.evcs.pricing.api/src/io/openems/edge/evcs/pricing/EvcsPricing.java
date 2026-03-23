package io.openems.edge.evcs.pricing;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

@ProviderType
public interface EvcsPricing extends OpenemsComponent {

	public static final String SINGLETON_SERVICE_PID = "Core.EvcsPricing";
	public static final String SINGLETON_COMPONENT_ID = "_evcsPricing";

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Current locked EVCS price.
		 *
		 * <ul>
		 * <li>Interface: EvcsPricing
		 * <li>Readable
		 * <li>Type: Double
		 * <li>Unit: EUR/kWh
		 * </ul>
		 */
		PRICE(Doc.of(OpenemsType.DOUBLE) //
				.unit(Unit.MONEY_PER_KILOWATT_HOUR) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * Preview of the next resolved price (not yet locked).
		 *
		 * <ul>
		 * <li>Interface: EvcsPricing
		 * <li>Readable
		 * <li>Type: Double
		 * <li>Unit: EUR/kWh
		 * </ul>
		 */
		NEXT_INTERVAL_PRICE(Doc.of(OpenemsType.DOUBLE) //
				.unit(Unit.MONEY_PER_KILOWATT_HOUR) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * Epoch milliseconds of the next interval tick when the price will be locked.
		 *
		 * <ul>
		 * <li>Interface: EvcsPricing
		 * <li>Readable
		 * <li>Type: Long
		 * <li>Unit: none (epoch millis)
		 * </ul>
		 */
		NEXT_PRICE_CHANGE(Doc.of(OpenemsType.LONG) //
				.persistencePriority(PersistencePriority.LOW)),

		/**
		 * Source (component ID) of the currently active override, or null if none.
		 *
		 * <ul>
		 * <li>Interface: EvcsPricing
		 * <li>Readable
		 * <li>Type: String
		 * </ul>
		 */
		ACTIVE_OVERRIDE_SOURCE(Doc.of(OpenemsType.STRING) //
				.persistencePriority(PersistencePriority.LOW)),

		/**
		 * Value of the currently active override, or null if none.
		 *
		 * <ul>
		 * <li>Interface: EvcsPricing
		 * <li>Readable
		 * <li>Type: Double
		 * <li>Unit: EUR/kWh
		 * </ul>
		 */
		ACTIVE_OVERRIDE_VALUE(Doc.of(OpenemsType.DOUBLE) //
				.unit(Unit.MONEY_PER_KILOWATT_HOUR) //
				.persistencePriority(PersistencePriority.LOW));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public default Channel<Double> getPriceChannel() {
		return this.channel(ChannelId.PRICE);
	}

	public default Value<Double> getPrice() {
		return this.getPriceChannel().value();
	}

	/**
	 * Internal method to set the locked price channel value.
	 *
	 * @param value the locked price in EUR/kWh
	 */
	public default void _setPrice(Double value) {
		this.getPriceChannel().setNextValue(value);
	}

	public default Channel<Double> getNextIntervalPriceChannel() {
		return this.channel(ChannelId.NEXT_INTERVAL_PRICE);
	}

	public default Value<Double> getNextIntervalPrice() {
		return this.getNextIntervalPriceChannel().value();
	}

	/**
	 * Internal method to set the next interval price channel value.
	 *
	 * @param value the preview price in EUR/kWh
	 */
	public default void _setNextIntervalPrice(Double value) {
		this.getNextIntervalPriceChannel().setNextValue(value);
	}

	public default Channel<Long> getNextPriceChangeChannel() {
		return this.channel(ChannelId.NEXT_PRICE_CHANGE);
	}

	public default Value<Long> getNextPriceChange() {
		return this.getNextPriceChangeChannel().value();
	}

	/**
	 * Internal method to set the next price change timestamp channel value.
	 *
	 * @param value epoch milliseconds of the next interval tick
	 */
	public default void _setNextPriceChange(Long value) {
		this.getNextPriceChangeChannel().setNextValue(value);
	}

	public default Channel<String> getActiveOverrideSourceChannel() {
		return this.channel(ChannelId.ACTIVE_OVERRIDE_SOURCE);
	}

	public default Channel<Double> getActiveOverrideValueChannel() {
		return this.channel(ChannelId.ACTIVE_OVERRIDE_VALUE);
	}

	/**
	 * Internal method to set the active override source channel value.
	 *
	 * @param value the component ID of the active override source, or null if none
	 */
	public default void _setActiveOverrideSource(String value) {
		this.getActiveOverrideSourceChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the active override value channel.
	 *
	 * @param value the override price in EUR/kWh, or null if no override is active
	 */
	public default void _setActiveOverrideValue(Double value) {
		this.getActiveOverrideValueChannel().setNextValue(value);
	}

	/**
	 * Adds a price ceiling constraint ("price should be at most maxPrice").
	 *
	 * <p>
	 * Multiple ceilings from different controllers are resolved after all
	 * controllers have run: the lowest ceiling wins.
	 *
	 * @param source   the controller's component ID
	 * @param maxPrice the maximum price in EUR/kWh
	 */
	public void addPriceCeiling(String source, double maxPrice);

	/**
	 * Adds a price floor constraint ("price should be at least minPrice").
	 *
	 * <p>
	 * Multiple floors from different controllers are resolved after all controllers
	 * have run: the highest floor wins. Floors trump ceilings.
	 *
	 * @param source   the controller's component ID
	 * @param minPrice the minimum price in EUR/kWh
	 */
	public void addPriceFloor(String source, double minPrice);

	/**
	 * Sets an immediate price override, bypassing interval-based resolution.
	 *
	 * <p>
	 * When any override is active, constraint-based prices are ignored. The override
	 * value becomes the locked price immediately.
	 *
	 * @param source the controller's component ID
	 * @param price  the override price in EUR/kWh
	 */
	public void setOverride(String source, double price);

	/**
	 * Removes a previously set override.
	 *
	 * <p>
	 * When all overrides are removed, the system returns to constraint-based
	 * interval resolution.
	 *
	 * @param source the controller's component ID
	 */
	public void removeOverride(String source);

	/**
	 * Removes all constraints (ceiling and floor) for the given source.
	 *
	 * <p>
	 * Constraints are also cleared automatically at the start of each cycle.
	 *
	 * @param source the controller's component ID
	 */
	public void removeConstraint(String source);
}
