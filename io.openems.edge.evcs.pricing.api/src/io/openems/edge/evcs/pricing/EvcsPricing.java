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

	/**
	 * The singleton service PID for the Core EVCS Pricing component.
	 */
	public static final String SINGLETON_SERVICE_PID = "Core.EvcsPricing";

	/**
	 * The singleton component ID for the Core EVCS Pricing component.
	 */
	public static final String SINGLETON_COMPONENT_ID = "_evcsPricing";

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Global EVCS price.
		 *
		 * <ul>
		 * <li>Interface: EvcsPricing
		 * <li>Readable
		 * <li>Type: Double
		 * <li>Unit: €/kWh
		 * </ul>
		 */
		PRICE(Doc.of(OpenemsType.DOUBLE) //
				.unit(Unit.MONEY_PER_KILOWATT_HOUR) //
				.persistencePriority(PersistencePriority.HIGH));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the Channel for {@link ChannelId#PRICE}.
	 *
	 * @return the Channel
	 */
	public default Channel<Double> getPriceChannel() {
		return this.channel(ChannelId.PRICE);
	}

	/**
	 * Gets the current EVCS price in [€/kWh]. See {@link ChannelId#PRICE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Double> getPrice() {
		return this.getPriceChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#PRICE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setPrice(Double value) {
		this.getPriceChannel().setNextValue(value);
	}
}



