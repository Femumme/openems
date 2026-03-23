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

	@AttributeDefinition(name = "Cron Expression", //
			description = "When the resolved price is locked, as a 6-field cron expression "
					+ "(seconds minutes hours day-of-month month day-of-week). "
					+ "Default \"0 0 * * * *\" fires at the top of every hour.")
	String cronExpression() default "0 0 * * * *";

	@AttributeDefinition(name = "Absolute Minimum Price [EUR/kWh]", //
			description = "Hard floor: no price will ever go below this value, not even overrides.")
	double absoluteMinPrice() default 0.00;

	@AttributeDefinition(name = "Absolute Maximum Price [EUR/kWh]", //
			description = "Hard ceiling: no price will ever go above this value, not even overrides.")
	double absoluteMaxPrice() default 9.99;

	String webconsole_configurationFactory_nameHint() default "Core EVCS Pricing";
}
