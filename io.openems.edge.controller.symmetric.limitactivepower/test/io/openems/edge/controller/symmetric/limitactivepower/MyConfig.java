package io.openems.edge.controller.symmetric.limitactivepower;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String essId;
		private int maxChargePower;
		private int maxDischargePower;
		private boolean validatePowerConstraints;

		private Builder() {

		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setEssId(String essId) {
			this.essId = essId;
			return this;
		}

		public Builder setMaxChargePower(int maxChargePower) {
			this.maxChargePower = maxChargePower;
			return this;
		}

		public Builder setMaxDischargePower(int maxDischargePower) {
			this.maxDischargePower = maxDischargePower;
			return this;
		}

		public Builder setValidatePowerConstraints(boolean validatePowerConstraints) {
			this.validatePowerConstraints = validatePowerConstraints;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this);
		}
	}

	/**
	 * Create a Config builder.
	 *
	 * @return a {@link Builder}
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
	public String ess_id() {
		return this.builder.essId;
	}

	@Override
	public int maxChargePower() {
		return this.builder.maxChargePower;
	}

	@Override
	public int maxDischargePower() {
		return this.builder.maxDischargePower;
	}

	@Override
	public String startTime() {
		return "";
	}

	@Override
	public String endTime() {
		return "";
	}

	@Override
	public Mode mode() {
		return Mode.MANUAL_ON;
	}

	@Override
	public boolean uiEnabled() {
		return false;
	}

	@Override
	public boolean validatePowerConstraints() {
		return this.builder.validatePowerConstraints;
	}

}