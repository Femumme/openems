package io.openems.edge.evcs.technagon;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "EVCS Technagon", description = "Implements one Technagon charge point via Modbus TCP.")
@interface Config {

	@AttributeDefinition(name = "Component-Id", description = "Unique ID for this EVCS.")
	String id() default "evcs0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this EVCS.")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Modbus-ID", description = "ID of Modbus bridge.")
	String modbus_id() default "modbus0";

	@AttributeDefinition(name = "Modbus Unit-ID", description = "Unit-ID of Modbus device.", min = "0", max = "255")
	int modbusUnitId() default 1;

	@AttributeDefinition(name = "Charge Point", description = "Charge point represented by this component.")
	ChargePoint chargePoint() default ChargePoint.CP1;

	@AttributeDefinition(name = "Minimum Current", description = "Minimum charging current in mA.", min = "0", max = "65535")
	int minHwCurrent() default 6000;

	@AttributeDefinition(name = "Maximum Current", description = "Maximum charging current in mA.", min = "0", max = "65535")
	int maxHwCurrent() default 32000;

	@AttributeDefinition(name = "Fallback Current", description = "Fallback current in mA.", min = "0", max = "65535")
	int fallbackCurrent() default 0;

	@AttributeDefinition(name = "Fallback Timeout", description = "Fallback timeout in seconds.", min = "0", max = "65535")
	int fallbackTimeout() default 30;

	@AttributeDefinition(name = "Read Energy Register", description = "Read cumulative 64-bit energy register.")
	boolean readEnergyRegister() default true;

	@AttributeDefinition(name = "Debug Mode", description = "Enable detailed command logging.")
	boolean debugMode() default false;

	@AttributeDefinition(name = "Modbus target filter", description = "Auto-generated from Modbus-ID.")
	String Modbus_target() default "(enabled=true)";

	String webconsole_configurationFactory_nameHint() default "EVCS Technagon [{id}]";
}
