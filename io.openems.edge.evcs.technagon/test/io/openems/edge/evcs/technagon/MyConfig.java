package io.openems.edge.evcs.technagon;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	public static class Builder {
		private String id = "evcs0";
		private String modbusId = "modbus0";
		private int modbusUnitId = 1;
		private ChargePoint chargePoint = ChargePoint.CP1;
		private int minHwCurrent = 6000;
		private int maxHwCurrent = 32000;
		private int fallbackCurrent;
		private int fallbackTimeout = 30;
		private boolean readEnergyRegister = true;
		private boolean debugMode;

		public Builder setChargePoint(ChargePoint chargePoint) {
			this.chargePoint = chargePoint;
			return this;
		}

		public Builder setReadEnergyRegister(boolean readEnergyRegister) {
			this.readEnergyRegister = readEnergyRegister;
			return this;
		}

		public Builder setMinHwCurrent(int minHwCurrent) {
			this.minHwCurrent = minHwCurrent;
			return this;
		}

		public Builder setMaxHwCurrent(int maxHwCurrent) {
			this.maxHwCurrent = maxHwCurrent;
			return this;
		}

		public Builder setFallbackCurrent(int fallbackCurrent) {
			this.fallbackCurrent = fallbackCurrent;
			return this;
		}

		public Builder setFallbackTimeout(int fallbackTimeout) {
			this.fallbackTimeout = fallbackTimeout;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this);
		}
	}

	/**
	 * Creates config builder.
	 *
	 * @return config builder
	 */
	public static Builder create() {
		return new Builder();
	}

	private final Builder builder;

	private MyConfig(Builder builder) {
		super(Config.class, builder.id);
		this.builder = builder;
	}

	@Override
	public boolean debugMode() {
		return this.builder.debugMode;
	}

	@Override
	public String modbus_id() {
		return this.builder.modbusId;
	}

	@Override
	public int modbusUnitId() {
		return this.builder.modbusUnitId;
	}

	@Override
	public ChargePoint chargePoint() {
		return this.builder.chargePoint;
	}

	@Override
	public int minHwCurrent() {
		return this.builder.minHwCurrent;
	}

	@Override
	public int maxHwCurrent() {
		return this.builder.maxHwCurrent;
	}

	@Override
	public int fallbackCurrent() {
		return this.builder.fallbackCurrent;
	}

	@Override
	public int fallbackTimeout() {
		return this.builder.fallbackTimeout;
	}

	@Override
	public boolean readEnergyRegister() {
		return this.builder.readEnergyRegister;
	}

	@Override
	public String Modbus_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.modbus_id());
	}
}
