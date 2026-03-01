package io.openems.edge.evcs.pricing.mumaxdisplay;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Evcs Pricing Mumax Display Exporter", //
		description = "Exports the global EVCS price to a Mumax display via HTTP POST.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "evcsPricingMumaxDisplay0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Display URL", description = "HTTP endpoint to POST the price to.")
	String url() default "https://www.mumax-wesel.de/display/update/";

	@AttributeDefinition(name = "Bearer API Token", description = "Bearer token for Authorization header.")
	String apiToken() default "";

	@AttributeDefinition(name = "Export interval [s]", description = "Re-export price at this interval as fallback, even if price unchanged.")
	int exportIntervalSeconds() default 1800; // 30 minutes

	@AttributeDefinition(name = "Retry Backoff [s]", description = "Base backoff delay between retries in seconds. "
			+ "Actual delay is attempt * backoff (e.g. 30s, 60s, 90s for 3 retries).")
	int retryBackoffSeconds() default 30;

	String webconsole_configurationFactory_nameHint() default "Evcs Pricing Mumax Display Exporter [{id}]";
}
