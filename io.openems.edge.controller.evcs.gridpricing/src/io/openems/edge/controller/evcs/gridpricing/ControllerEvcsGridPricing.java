package io.openems.edge.controller.evcs.gridpricing;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.evcs.pricing.EvcsPricingController;

public interface ControllerEvcsGridPricing extends Controller, OpenemsComponent, EvcsPricingController {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * The rolling average of the current grid price used for threshold comparison.
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

	public default Channel<Double> getAverageGridPriceChannel() {
		return this.channel(ChannelId.AVERAGE_GRID_PRICE);
	}

	public default Value<Double> getAverageGridPrice() {
		return this.getAverageGridPriceChannel().value();
	}

	/**
	 * Internal method to set the average grid price channel value.
	 *
	 * @param value the average grid price in ct/kWh, or null if unavailable
	 */
	public default void _setAverageGridPrice(Double value) {
		this.getAverageGridPriceChannel().setNextValue(value);
	}
}
