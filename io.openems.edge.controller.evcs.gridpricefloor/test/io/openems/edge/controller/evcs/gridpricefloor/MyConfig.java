package io.openems.edge.controller.evcs.gridpricefloor;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String alias;
		private boolean enabled = true;
		private double margin = 0.0;

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

		public Builder setMargin(double margin) {
			this.margin = margin;
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
	public double margin() {
		return this.builder.margin;
	}

	@Override
	public String webconsole_configurationFactory_nameHint() {
		return "Controller Evcs Grid Price Floor [" + this.builder.id + "]";
	}
}
