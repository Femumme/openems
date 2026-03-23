package io.openems.edge.controller.evcs.pvpricing;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Evcs PV Pricing", //
		description = "Reduces the EVCS price based on PV production.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlEvcsPvPricing0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Max Ceiling [EUR/kWh]", //
			description = "Ceiling price at PV threshold (highest ceiling).")
	double maxCeiling() default 0.55;

	@AttributeDefinition(name = "Min Ceiling [EUR/kWh]", //
			description = "Ceiling price at full PV production (lowest ceiling).")
	double minCeiling() default 0.25;

	@AttributeDefinition(name = "PV Threshold [W]", //
			description = "Minimum PV production in watts to start setting a ceiling.")
	int pvThreshold() default 500;

	@AttributeDefinition(name = "PV Full Production [W]", //
			description = "PV production in watts at which the minimum ceiling applies.")
	int pvFullProduction() default 8000;

	@AttributeDefinition(name = "Data Collection Window [minutes]", //
			description = "Rolling average window for PV production smoothing.")
	int dataCollectionWindowMinutes() default 15;

	String webconsole_configurationFactory_nameHint() default "Controller Evcs PV Pricing [{id}]";
}
