package io.openems.edge.evcs.pricing;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Nature interface for EVCS pricing controllers.
 *
 * <p>
 * Each pricing controller implements this interface and exposes which constraint
 * (ceiling, floor, override) it is currently contributing. The frontend uses
 * these channels to display per-controller pricing data.
 */
public interface EvcsPricingController extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * The ceiling price this controller is currently setting, or null if inactive.
		 */
		ACTIVE_CEILING(Doc.of(OpenemsType.DOUBLE) //
				.unit(Unit.MONEY_PER_KILOWATT_HOUR)),

		/**
		 * The floor price this controller is currently setting, or null if inactive.
		 */
		ACTIVE_FLOOR(Doc.of(OpenemsType.DOUBLE) //
				.unit(Unit.MONEY_PER_KILOWATT_HOUR)),

		/**
		 * The override price this controller is currently setting, or null if inactive.
		 */
		ACTIVE_OVERRIDE(Doc.of(OpenemsType.DOUBLE) //
				.unit(Unit.MONEY_PER_KILOWATT_HOUR));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public default Channel<Double> getActiveCeilingChannel() {
		return this.channel(ChannelId.ACTIVE_CEILING);
	}

	public default Value<Double> getActiveCeiling() {
		return this.getActiveCeilingChannel().value();
	}

	/**
	 * Internal method to set the active ceiling channel value.
	 *
	 * @param value the ceiling price in EUR/kWh, or null if inactive
	 */
	public default void _setActiveCeiling(Double value) {
		this.getActiveCeilingChannel().setNextValue(value);
	}

	public default Channel<Double> getActiveFloorChannel() {
		return this.channel(ChannelId.ACTIVE_FLOOR);
	}

	public default Value<Double> getActiveFloor() {
		return this.getActiveFloorChannel().value();
	}

	/**
	 * Internal method to set the active floor channel value.
	 *
	 * @param value the floor price in EUR/kWh, or null if inactive
	 */
	public default void _setActiveFloor(Double value) {
		this.getActiveFloorChannel().setNextValue(value);
	}

	public default Channel<Double> getActiveOverrideChannel() {
		return this.channel(ChannelId.ACTIVE_OVERRIDE);
	}

	public default Value<Double> getActiveOverride() {
		return this.getActiveOverrideChannel().value();
	}

	/**
	 * Internal method to set the active override channel value.
	 *
	 * @param value the override price in EUR/kWh, or null if inactive
	 */
	public default void _setActiveOverride(Double value) {
		this.getActiveOverrideChannel().setNextValue(value);
	}
}
