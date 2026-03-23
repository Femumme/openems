package io.openems.edge.controller.evcs.batterypricing;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Evcs Battery Pricing", //
		description = "Sets EVCS price constraints based on battery state of charge.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlEvcsBatteryPricing0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Low SoC Threshold [%]", //
			description = "SoC below which a price floor is set to protect the battery.")
	int lowSocThreshold() default 30;

	@AttributeDefinition(name = "High SoC Threshold [%]", //
			description = "SoC above which a price ceiling is set to encourage EV charging.")
	int highSocThreshold() default 80;

	@AttributeDefinition(name = "Low SoC Floor Price [EUR/kWh]", //
			description = "Price floor when SoC is below the low threshold.")
	double lowSocFloorPrice() default 0.45;

	@AttributeDefinition(name = "High SoC Ceiling Price [EUR/kWh]", //
			description = "Ceiling price at the high SoC threshold.")
	double highSocCeilPrice() default 0.55;

	@AttributeDefinition(name = "Full SoC Ceiling Price [EUR/kWh]", //
			description = "Ceiling price at 100% SoC (lowest ceiling).")
	double fullCeilPrice() default 0.35;

	@AttributeDefinition(name = "Data Collection Window [minutes]", //
			description = "Rolling average window for SoC smoothing.")
	int dataCollectionWindowMinutes() default 15;

	String webconsole_configurationFactory_nameHint() default "Controller Evcs Battery Pricing [{id}]";
}
