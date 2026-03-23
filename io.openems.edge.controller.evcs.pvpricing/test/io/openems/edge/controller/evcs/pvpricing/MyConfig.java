package io.openems.edge.controller.evcs.pvpricing;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String alias;
		private boolean enabled = true;
		private double maxCeiling = 0.55;
		private double minCeiling = 0.25;
		private int pvThreshold = 500;
		private int pvFullProduction = 8000;
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

		public Builder setMaxCeiling(double maxCeiling) {
			this.maxCeiling = maxCeiling;
			return this;
		}

		public Builder setMinCeiling(double minCeiling) {
			this.minCeiling = minCeiling;
			return this;
		}

		public Builder setPvThreshold(int pvThreshold) {
			this.pvThreshold = pvThreshold;
			return this;
		}

		public Builder setPvFullProduction(int pvFullProduction) {
			this.pvFullProduction = pvFullProduction;
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
	public double maxCeiling() {
		return this.builder.maxCeiling;
	}

	@Override
	public double minCeiling() {
		return this.builder.minCeiling;
	}

	@Override
	public int pvThreshold() {
		return this.builder.pvThreshold;
	}

	@Override
	public int pvFullProduction() {
		return this.builder.pvFullProduction;
	}

	@Override
	public int dataCollectionWindowMinutes() {
		return this.builder.dataCollectionWindowMinutes;
	}

	@Override
	public String webconsole_configurationFactory_nameHint() {
		return "Controller Evcs PV Pricing [" + this.builder.id + "]";
	}
}
