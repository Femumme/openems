package io.openems.edge.evcs.pricing.core;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.evcs.pricing.EvcsPricing;

@ObjectClassDefinition(//
		name = "Core EVCS Pricing", //
		description = "Global EVCS pricing component. Controllers set the price, exporters read and export it.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default EvcsPricing.SINGLETON_COMPONENT_ID;

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "EVCS Pricing";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	String webconsole_configurationFactory_nameHint() default "Core EVCS Pricing";
}




