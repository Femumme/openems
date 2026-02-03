package io.openems.edge.evcs.pricing.eaaze;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Evcs Pricing Eaaze Exporter", //
		description = "Exports the global EVCS price to Eaaze backend via GraphQL.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "evcsPricingEaaze0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Export interval [s]", description = "Re-export price to Eaaze at this interval as fallback, even if price unchanged.")
	int exportIntervalSeconds() default 1800; // 30 minutes

	@AttributeDefinition(name = "GraphQL URL", description = "Eaaze GraphQL endpoint URL.")
	String graphqlUrl() default "https://eaaze.cloud/query";

	@AttributeDefinition(name = "M2M API Token", description = "Eaaze M2M authentication token.")
	String apiToken() default "";

	@AttributeDefinition(name = "Tariff ID", description = "UUID of the CPO tariff to update (e.g. 'ab8fe88e-8db3-48a8-b99a-6dc09fc6bfb4').")
	String tariffId() default "";

	@AttributeDefinition(name = "Tenant ID", description = "Eaaze tenant ID (e.g. 'dynamic-pricing-test-tenant').")
	String tenantId() default "";

	@AttributeDefinition(name = "Tariff Name", description = "Name of the tariff (e.g. 'RFID Tag Tariff (AC)').")
	String tariffName() default "Dynamic Energy Tariff";

	@AttributeDefinition(name = "Tax Rate", description = "VAT tax rate as decimal (e.g. 0.19 for 19%).")
	double taxRate() default 0.19;

	String webconsole_configurationFactory_nameHint() default "Evcs Pricing Eaaze Exporter [{id}]";
}