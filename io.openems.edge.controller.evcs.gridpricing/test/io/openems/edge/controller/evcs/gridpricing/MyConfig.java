package io.openems.edge.controller.evcs.gridpricing;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String alias;
		private boolean enabled = true;
		private double priceThreshold = 0.0;
		private double ceilingPrice = 5.0;

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

		public Builder setPriceThreshold(double priceThreshold) {
			this.priceThreshold = priceThreshold;
			return this;
		}

		public Builder setCeilingPrice(double ceilingPrice) {
			this.ceilingPrice = ceilingPrice;
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
	public double priceThreshold() {
		return this.builder.priceThreshold;
	}

	@Override
	public double ceilingPrice() {
		return this.builder.ceilingPrice;
	}

	@Override
	public String webconsole_configurationFactory_nameHint() {
		return "Controller Evcs Grid Pricing [" + this.builder.id + "]";
	}
}
