package io.openems.edge.evcs.pricing.eaaze;

import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface EvcsPricingEaaze extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		HTTP_STATUS_CODE(Doc.of(OpenemsType.INTEGER) //
				.text("The HTTP status code of the last export attempt")), //

		EXPORT_FAILED(Doc.of(Level.FAULT) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Price export to Eaaze failed after all retries")), //
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

	/**
	 * Gets the Channel for {@link ChannelId#HTTP_STATUS_CODE}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getHttpStatusCodeChannel() {
		return this.channel(ChannelId.HTTP_STATUS_CODE);
	}

	/**
	 * Gets the HttpStatusCode. See {@link ChannelId#HTTP_STATUS_CODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getHttpStatusCode() {
		return this.getHttpStatusCodeChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#HTTP_STATUS_CODE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setHttpStatusCode(Integer value) {
		this.getHttpStatusCodeChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#EXPORT_FAILED}.
	 *
	 * @return the StateChannel
	 */
	public default StateChannel getExportFailedChannel() {
		return this.channel(ChannelId.EXPORT_FAILED);
	}

	/**
	 * Gets the ExportFailed state. See {@link ChannelId#EXPORT_FAILED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getExportFailed() {
		return this.getExportFailedChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#EXPORT_FAILED}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setExportFailed(boolean value) {
		this.getExportFailedChannel().setNextValue(value);
	}
}



