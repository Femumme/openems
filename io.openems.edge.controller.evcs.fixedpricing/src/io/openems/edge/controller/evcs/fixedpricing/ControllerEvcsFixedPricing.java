package io.openems.edge.controller.evcs.fixedpricing;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.evcs.pricing.EvcsPricingController;

/**
 * Controller that sets a fixed EVCS price override to the global EvcsPricing
 * singleton.
 */
public interface ControllerEvcsFixedPricing extends Controller, OpenemsComponent, EvcsPricingController {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
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
