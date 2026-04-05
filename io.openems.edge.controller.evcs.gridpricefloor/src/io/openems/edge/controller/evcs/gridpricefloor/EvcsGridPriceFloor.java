package io.openems.edge.controller.evcs.gridpricefloor;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.evcs.pricing.EvcsPricingController;

/**
 * EVCS Grid Price Floor controller.
 *
 * <p>
 * Computes a forecasted average grid price over a configurable lookahead window
 * and publishes it as a price floor via {@link EvcsPricingController}. The
 * aggregated pricing layer uses {@code ACTIVE_FLOOR} (inherited from
 * {@link EvcsPricingController}) to ensure the minimum charging price tracks
 * the grid cost so the operator never sells below their procurement price.
 */
public interface EvcsGridPriceFloor extends Controller, OpenemsComponent, EvcsPricingController {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * The most recently computed average grid price over the lookahead window, in
		 * ct/kWh. Null if no price data is available.
		 */
		AVERAGE_GRID_PRICE(Doc.of(OpenemsType.DOUBLE));

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
	 * Returns the {@link Channel} for {@link ChannelId#AVERAGE_GRID_PRICE}.
	 *
	 * @return the channel
	 */
	public default Channel<Double> getAverageGridPriceChannel() {
		return this.channel(ChannelId.AVERAGE_GRID_PRICE);
	}

	/**
	 * Returns the most recently computed average grid price over the lookahead
	 * window in ct/kWh, or null if unavailable.
	 *
	 * @return the {@link Value}
	 */
	public default Value<Double> getAverageGridPrice() {
		return this.getAverageGridPriceChannel().value();
	}

	/**
	 * Internal method to set the {@link ChannelId#AVERAGE_GRID_PRICE} channel
	 * value.
	 *
	 * @param value the average grid price in ct/kWh, or null if unavailable
	 */
	public default void _setAverageGridPrice(Double value) {
		this.getAverageGridPriceChannel().setNextValue(value);
	}
}
