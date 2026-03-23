package io.openems.edge.controller.evcs.batterypricing;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String alias;
		private boolean enabled = true;
		private int lowSocThreshold = 30;
		private int highSocThreshold = 80;
		private double lowSocFloorPrice = 0.45;
		private double highSocCeilPrice = 0.55;
		private double fullCeilPrice = 0.35;
		private int dataCollectionWindowMinutes = 1;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setAlias(String alias) {
			this.alias = alias;
			return this;
		}

		public Builder setEnabled(boolean enabled) {
			this.enabled = enabled;
			return this;
		}

		public Builder setLowSocThreshold(int lowSocThreshold) {
			this.lowSocThreshold = lowSocThreshold;
			return this;
		}

		public Builder setHighSocThreshold(int highSocThreshold) {
			this.highSocThreshold = highSocThreshold;
			return this;
		}

		public Builder setLowSocFloorPrice(double lowSocFloorPrice) {
			this.lowSocFloorPrice = lowSocFloorPrice;
			return this;
		}

		public Builder setHighSocCeilPrice(double highSocCeilPrice) {
			this.highSocCeilPrice = highSocCeilPrice;
			return this;
		}

		public Builder setFullCeilPrice(double fullCeilPrice) {
			this.fullCeilPrice = fullCeilPrice;
			return this;
		}

		public Builder setDataCollectionWindowMinutes(int dataCollectionWindowMinutes) {
			this.dataCollectionWindowMinutes = dataCollectionWindowMinutes;
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
	public String alias() {
		return this.builder.alias != null ? this.builder.alias : this.builder.id;
	}

	@Override
	public boolean enabled() {
		return this.builder.enabled;
	}

	@Override
	public int lowSocThreshold() {
		return this.builder.lowSocThreshold;
	}

	@Override
	public int highSocThreshold() {
		return this.builder.highSocThreshold;
	}

	@Override
	public double lowSocFloorPrice() {
		return this.builder.lowSocFloorPrice;
	}

	@Override
	public double highSocCeilPrice() {
		return this.builder.highSocCeilPrice;
	}

	@Override
	public double fullCeilPrice() {
		return this.builder.fullCeilPrice;
	}

	@Override
	public int dataCollectionWindowMinutes() {
		return this.builder.dataCollectionWindowMinutes;
	}

	@Override
	public String webconsole_configurationFactory_nameHint() {
		return "Controller Evcs Battery Pricing [" + this.builder.id + "]";
	}
}
