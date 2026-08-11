package io.openems.edge.evcs.technagon;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.meter.api.ElectricityMeter;

public interface EvcsTechnagon extends ElectricityMeter, Evcs, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		SERIAL_NUMBER(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY)), //
		STATION_MAX_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE).accessMode(AccessMode.READ_ONLY)), //
		CHARGE_POINT_COUNT(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)), //
		RAW_STATUS(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)), //
		MIN_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE).accessMode(AccessMode.READ_ONLY)), //
		MAX_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE).accessMode(AccessMode.READ_ONLY)), //
		OFFERED_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE).accessMode(AccessMode.READ_ONLY)), //
		POWER_FACTOR_L1(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)), //
		POWER_FACTOR_L2(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)), //
		POWER_FACTOR_L3(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)), //
		PERCENT_SETPOINT(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT).accessMode(AccessMode.WRITE_ONLY)), //
		CURRENT_SETPOINT(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE).accessMode(AccessMode.WRITE_ONLY)), //
		FALLBACK_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE).accessMode(AccessMode.WRITE_ONLY)), //
		FALLBACK_TIMEOUT(Doc.of(OpenemsType.INTEGER).unit(Unit.SECONDS).accessMode(AccessMode.WRITE_ONLY));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	default Channel<Integer> getRawStatusChannel() {
		return this.channel(ChannelId.RAW_STATUS);
	}

	default Channel<Integer> getOfferedCurrentChannel() {
		return this.channel(ChannelId.OFFERED_CURRENT);
	}

	default WriteChannel<Integer> getPercentSetpointChannel() {
		return this.channel(ChannelId.PERCENT_SETPOINT);
	}

	default WriteChannel<Integer> getCurrentSetpointChannel() {
		return this.channel(ChannelId.CURRENT_SETPOINT);
	}

	default WriteChannel<Integer> getFallbackCurrentChannel() {
		return this.channel(ChannelId.FALLBACK_CURRENT);
	}

	default WriteChannel<Integer> getFallbackTimeoutChannel() {
		return this.channel(ChannelId.FALLBACK_TIMEOUT);
	}

	default void setCurrentSetpoint(int value) throws OpenemsNamedException {
		this.getCurrentSetpointChannel().setNextWriteValue(value);
	}
}
