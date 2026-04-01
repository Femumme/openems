package io.openems.edge.controller.evcs.gridpricing;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Evcs Grid Pricing", //
		description = "Sets EVCS price ceiling when grid prices are below a configurable threshold.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlEvcsGridPricing0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Price Threshold [ct/kWh]", //
			description = "Grid price below which the ceiling is activated.")
	double priceThreshold() default 0.0;

	@AttributeDefinition(name = "Ceiling Price [ct/kWh]", //
			description = "Price ceiling submitted to EvcsPricing when the threshold is met.")
	double ceilingPrice() default 5.0;

	String webconsole_configurationFactory_nameHint() default "Controller Evcs Grid Pricing [{id}]";
}
