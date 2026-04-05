package io.openems.edge.controller.evcs.gridpricefloor;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Evcs Grid Price Floor", //
		description = "Sets the forecasted grid electricity price as a price floor in EvcsPricing.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlEvcsGridPriceFloor0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Margin [ct/kWh]", //
			description = "Added on top of the average grid price to cover taxes, wear, and distribution costs.")
	double margin() default 0.0;

	String webconsole_configurationFactory_nameHint() default "Controller Evcs Grid Price Floor [{id}]";
}
