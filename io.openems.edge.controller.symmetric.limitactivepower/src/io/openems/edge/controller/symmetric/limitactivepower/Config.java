package io.openems.edge.controller.symmetric.limitactivepower;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Limit Active Power Symmetric", //
		description = "Defines charge and discharge limits for a symmetric energy storage system.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlLimitActivePower0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id();

	@AttributeDefinition(name = "Max Charge Power [W]", description = "Positive value describing the maximum Charge Power.")
	int maxChargePower();

	@AttributeDefinition(name = "Max Discharge Power [W]", description = "Positive value describing the maximum Discharge Power.")
	int maxDischargePower();

	@AttributeDefinition(name = "(Optional) Start Time", description = "Start time when the limit should be applied; ISO 8601 format", required = false)
	String startTime() default "";

	@AttributeDefinition(name = "(Optional) End Time", description = "End time when the limit should be applied; ISO 8601 format", required = false)
	String endTime() default "";

	@AttributeDefinition(name = "Mode", description = "Mode, can be on or off")
	Mode mode() default Mode.MANUAL_ON;

	@AttributeDefinition(name = "(Optional) Enables UI Widget", description = "Enables UI Widget to change the limit and timeframe.")
	boolean uiEnabled() default false;

	@AttributeDefinition(name = "Validate applied power Constraints", description = "If this property is 'false' the limitation is not validated. " //
			+ "Only disable if you know what you are doing. This can break the system!")
	boolean validatePowerConstraints() default true;

	String webconsole_configurationFactory_nameHint() default "Controller Limit Active Power Symmetric [{id}]";
}