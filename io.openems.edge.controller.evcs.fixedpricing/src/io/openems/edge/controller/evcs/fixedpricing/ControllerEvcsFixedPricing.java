package io.openems.edge.controller.evcs.fixedpricing;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

/**
 * Controller that sets a fixed EVCS price to the global EvcsPricing singleton.
 */
public interface ControllerEvcsFixedPricing extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		// No additional channels for this controller
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
}