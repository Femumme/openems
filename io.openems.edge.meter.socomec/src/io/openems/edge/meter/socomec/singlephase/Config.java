package io.openems.edge.meter.socomec.singlephase;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.types.MeterType;
import io.openems.edge.meter.api.SinglePhase;

@ObjectClassDefinition(//
		name = "Meter Socomec Singlephase", //
		description = "Implements a singlephase Socomec meter. Actual type is identified automatically.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "meter0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Phase", description = "Which Phase is measured by this Meter?")
	SinglePhase phase() default SinglePhase.L1;

	@AttributeDefinition(name = "Meter-Type", description = "What is measured by this Meter?")
	MeterType type() default MeterType.PRODUCTION;

	@AttributeDefinition(name = "Modbus-ID", description = "ID of Modbus bridge.")
	String modbus_id() default "modbus0";

	@AttributeDefinition(name = "Modbus Unit-ID", description = "The Unit-ID of the Modbus device.")
	int modbusUnitId();

	@AttributeDefinition(name = "Invert Power", description = "Inverts all Power values, inverts current values, swaps production and consumptioon energy, i.e. Power is multiplied with -1.")
	boolean invert() default false;

	@AttributeDefinition(name = "Modbus target filter", description = "This is auto-generated by 'Modbus-ID'.")
	String Modbus_target() default "(enabled=true)";

	String webconsole_configurationFactory_nameHint() default "Meter Socomec Singlephase [{id}]";
}