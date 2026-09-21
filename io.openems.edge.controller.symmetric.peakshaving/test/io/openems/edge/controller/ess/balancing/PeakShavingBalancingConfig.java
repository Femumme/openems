package io.openems.edge.controller.ess.balancing;

import io.openems.common.test.AbstractComponentConfig;

/** Configuration for the real fallback controller used by peak-shaving tests. */
public class PeakShavingBalancingConfig extends AbstractComponentConfig implements Config {

	public PeakShavingBalancingConfig() {
		super(Config.class, "ctrlFallback0");
	}

	@Override
	public String ess_id() {
		return "ess0";
	}

	@Override
	public String meter_id() {
		return "meter0";
	}

	@Override
	public int targetGridSetpoint() {
		return 0;
	}
}
