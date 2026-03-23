package io.openems.edge.evcs.pricing.core;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String alias;
		private boolean enabled;
		private String cronExpression;
		private double absoluteMinPrice;
		private double absoluteMaxPrice;

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

		public Builder setCronExpression(String cronExpression) {
			this.cronExpression = cronExpression;
			return this;
		}

		public Builder setAbsoluteMinPrice(double absoluteMinPrice) {
			this.absoluteMinPrice = absoluteMinPrice;
			return this;
		}

		public Builder setAbsoluteMaxPrice(double absoluteMaxPrice) {
			this.absoluteMaxPrice = absoluteMaxPrice;
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
	public String cronExpression() {
		return this.builder.cronExpression;
	}

	@Override
	public double absoluteMinPrice() {
		return this.builder.absoluteMinPrice;
	}

	@Override
	public double absoluteMaxPrice() {
		return this.builder.absoluteMaxPrice;
	}
}
