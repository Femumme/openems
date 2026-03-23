package io.openems.edge.controller.evcs.fixedpricing;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Evcs Fixed Pricing", //
		description = "Provides a fixed EVCS price in €/kWh.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlEvcsFixedPricing0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Mode", description = "Set the type of mode.")
	Mode mode() default Mode.MANUAL_ON;

	@AttributeDefinition(name = "Price [€/kWh]", description = "Fixed EVCS price in Euro per kWh (e.g. 0.35).")
	double priceEurPerKwh() default 0.35;

	String webconsole_configurationFactory_nameHint() default "Controller Evcs Fixed Pricing [{id}]";
}



