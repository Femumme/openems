package io.openems.edge.controller.pvinverter.selltogridlimit;

import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER_L1;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER_L2;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER_L3;
import static io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.meter.test.DummyElectricityMeter;
import io.openems.edge.pvinverter.test.DummyManagedSymmetricPvInverter;

public class ControllerPvInverterSellToGridLimitImplTest {

	@Test
	public void symmetricMeterTest() throws Exception {
		new ControllerTest(new ControllerPvInverterSellToGridLimitImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummyElectricityMeter("meter0")) //
				.addComponent(new DummyManagedSymmetricPvInverter("pvInverter0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setMeterId("meter0") //
						.setAsymmetricMode(false) //
						.setMaximumSellToGridPower(10_000) //
						.setPvInverterId("pvInverter0") //
						.build())
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER, -15000) //
						.input("pvInverter0", ACTIVE_POWER, 15000) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, 10000)) //
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER, -15000) //
						.input("pvInverter0", ACTIVE_POWER, 10000) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, 8000)) // 10000 * (1 - 0.2) = 8000
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER, -13000) //
						.input("pvInverter0", ACTIVE_POWER, 8000) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, 6400)) // 8000 * (1 - 0.2) = 6400
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER, -11400) //
						.input("pvInverter0", ACTIVE_POWER, 6400) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, 5120)) // 6400 * (1 - 0.2) = 5120
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER, -10120) //
						.input("pvInverter0", ACTIVE_POWER, 5120) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, 5000)) //
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER, -9000) //
						.input("pvInverter0", ACTIVE_POWER, 5000) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, 6000)) //
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER, 0) //
						.input("pvInverter0", ACTIVE_POWER, 6000) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, 7200)) // 6000 * (1 + 0.2) = 7200
				.deactivate();
	}

	@Test
	public void asymmetricMeterTest() throws Exception {
		new ControllerTest(new ControllerPvInverterSellToGridLimitImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummyElectricityMeter("meter0")) //
				.addComponent(new DummyManagedSymmetricPvInverter("pvInverter0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setMeterId("meter0") //
						.setAsymmetricMode(true) //
						.setMaximumSellToGridPower(4_000) // 12_000 in total
						.setPvInverterId("pvInverter0") //
						.build())
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER_L1, -2000) //
						.input("meter0", ACTIVE_POWER_L2, -4000) //
						.input("meter0", ACTIVE_POWER_L3, -3000) //
						.input("pvInverter0", ACTIVE_POWER, 12000) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, 12000)) //
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER_L1, -2000) //
						.input("meter0", ACTIVE_POWER_L2, -5000) //
						.input("meter0", ACTIVE_POWER_L3, -2000) //
						.input("pvInverter0", ACTIVE_POWER, 12000) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, 9600)) // 12000 * (1 - 0.2) = 9600
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER_L1, -1200) //
						.input("meter0", ACTIVE_POWER_L2, -4200) //
						.input("meter0", ACTIVE_POWER_L3, -1200) //
						.input("pvInverter0", ACTIVE_POWER, 9600) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, 9000)) //
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER_L1, -1000) //
						.input("meter0", ACTIVE_POWER_L2, -4000) //
						.input("meter0", ACTIVE_POWER_L3, -1000) //
						.input("pvInverter0", ACTIVE_POWER, 9000) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, 9000)) //
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER_L1, -1000) //
						.input("meter0", ACTIVE_POWER_L2, -3700) //
						.input("meter0", ACTIVE_POWER_L3, -1000) //
						.input("pvInverter0", ACTIVE_POWER, 9000) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, 9900)) //
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER_L1, -2000) //
						.input("meter0", ACTIVE_POWER_L2, -5000) //
						.input("meter0", ACTIVE_POWER_L3, -2000) //
						.input("pvInverter0", ACTIVE_POWER, 9900) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, 7920)) // 9900 * (1 - 0.2) = 7920
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER_L1, -2000) //
						.input("meter0", ACTIVE_POWER_L2, -5000) //
						.input("meter0", ACTIVE_POWER_L3, -2000) //
						.input("pvInverter0", ACTIVE_POWER, 7920) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, 6336)) // 7920 * (1 - 0.2) = 6336
				.deactivate();

		new ControllerTest(new ControllerPvInverterSellToGridLimitImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummyElectricityMeter("meter0")) //
				.addComponent(new DummyManagedSymmetricPvInverter("pvInverter0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setMeterId("meter0") //
						.setAsymmetricMode(true) //
						.setMaximumSellToGridPower(4_000) // 12_000 in total
						.setPvInverterId("pvInverter0") //
						.build())
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER_L1, 1000) //
						.input("meter0", ACTIVE_POWER_L2, 2000) //
						.input("meter0", ACTIVE_POWER_L3, 3000) //
						.input("pvInverter0", ACTIVE_POWER, 1000) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, 16000)) //
				.deactivate();
	}

	@Test
	public void asymmetricMinPhaseVariantsTest() throws Exception {
		// L1 is the most-negative (minimum) phase
		// min(-5000, -2000, -1000) = -5000; gridPower = 3 * (-5000) = -15000
		// raw = -15000 + 12000 + 12000 = 9000; lastSetLimit=0 (≤100) → output 9000
		new ControllerTest(new ControllerPvInverterSellToGridLimitImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummyElectricityMeter("meter0")) //
				.addComponent(new DummyManagedSymmetricPvInverter("pvInverter0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setMeterId("meter0") //
						.setAsymmetricMode(true) //
						.setMaximumSellToGridPower(4_000) // 12_000 in total
						.setPvInverterId("pvInverter0") //
						.build())
				.next(new TestCase() //
						// L1 is minimum: min(-5000, -2000, -1000) = -5000
						// gridPower = 3 * (-5000) = -15000; raw = -15000 + 12000 + 12000 = 9000
						// lastSetLimit=0 (≤100) → output 9000
						.input("meter0", ACTIVE_POWER_L1, -5000) //
						.input("meter0", ACTIVE_POWER_L2, -2000) //
						.input("meter0", ACTIVE_POWER_L3, -1000) //
						.input("pvInverter0", ACTIVE_POWER, 12000) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, 9000)) //
				.deactivate();

		// L3 is the most-negative (minimum) phase
		// min(-1000, -2000, -5000) = -5000; gridPower = 3 * (-5000) = -15000
		// raw = -15000 + 12000 + 12000 = 9000; lastSetLimit=0 (≤100) → output 9000
		new ControllerTest(new ControllerPvInverterSellToGridLimitImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummyElectricityMeter("meter0")) //
				.addComponent(new DummyManagedSymmetricPvInverter("pvInverter0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setMeterId("meter0") //
						.setAsymmetricMode(true) //
						.setMaximumSellToGridPower(4_000) // 12_000 in total
						.setPvInverterId("pvInverter0") //
						.build())
				.next(new TestCase() //
						// L3 is minimum: min(-1000, -2000, -5000) = -5000
						// gridPower = 3 * (-5000) = -15000; raw = -15000 + 12000 + 12000 = 9000
						// lastSetLimit=0 (≤100) → output 9000
						.input("meter0", ACTIVE_POWER_L1, -1000) //
						.input("meter0", ACTIVE_POWER_L2, -2000) //
						.input("meter0", ACTIVE_POWER_L3, -5000) //
						.input("pvInverter0", ACTIVE_POWER, 12000) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, 9000)) //
				.deactivate();
	}

	@Test
	public void essAwarenessTest() throws Exception {
		// Group 1: ESS discharging raises PV limit
		// raw = -10_000 + 10_000 + 10_000 + max(0, 5_000) = 15_000
		// lastSetLimit starts at 0 (≤100) → no rate limiting → output 15_000
		new ControllerTest(new ControllerPvInverterSellToGridLimitImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummyElectricityMeter("meter0")) //
				.addComponent(new DummyManagedSymmetricPvInverter("pvInverter0")) //
				.addComponent(new DummyManagedSymmetricEss("ess0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setMeterId("meter0") //
						.setAsymmetricMode(false) //
						.setMaximumSellToGridPower(10_000) //
						.setPvInverterId("pvInverter0") //
						.setEssId("ess0") //
						.build())
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER, -10_000) //
						.input("pvInverter0", ACTIVE_POWER, 10_000) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 5_000) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, 15_000)) //
				.deactivate();

		// Group 2: ESS idle then charging — ESS contribution is zero
		// Cycle 1: raw = -10_000 + 10_000 + 10_000 + max(0, 0) = 10_000 → output 10_000
		// Cycle 2: raw = -10_000 + 10_000 + 10_000 + max(0, -5_000) = 10_000 → no change → output 10_000
		new ControllerTest(new ControllerPvInverterSellToGridLimitImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummyElectricityMeter("meter0")) //
				.addComponent(new DummyManagedSymmetricPvInverter("pvInverter0")) //
				.addComponent(new DummyManagedSymmetricEss("ess0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setMeterId("meter0") //
						.setAsymmetricMode(false) //
						.setMaximumSellToGridPower(10_000) //
						.setPvInverterId("pvInverter0") //
						.setEssId("ess0") //
						.build())
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER, -10_000) //
						.input("pvInverter0", ACTIVE_POWER, 10_000) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, 10_000)) //
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER, -10_000) //
						.input("pvInverter0", ACTIVE_POWER, 10_000) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, -5_000) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, 10_000)) //
				.deactivate();

		// Group 3: ESS discharge stops, limit ramps down smoothly at 20% per cycle
		// Cycle 1: ess=+10_000 → raw=20_000; lastSetLimit=0 (≤100) → output 20_000
		// Cycle 2: ess=0 → raw=10_000; diff=10_000 > 20_000*0.2=4_000 → ramp down → 20_000 - (int)(20_000*0.2) = 16_000
		// Cycle 3: ess=0 → raw=10_000; diff=6_000 > 16_000*0.2=3_200 → ramp down → 16_000 - (int)(16_000*0.2) = 12_800
		new ControllerTest(new ControllerPvInverterSellToGridLimitImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummyElectricityMeter("meter0")) //
				.addComponent(new DummyManagedSymmetricPvInverter("pvInverter0")) //
				.addComponent(new DummyManagedSymmetricEss("ess0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setMeterId("meter0") //
						.setAsymmetricMode(false) //
						.setMaximumSellToGridPower(10_000) //
						.setPvInverterId("pvInverter0") //
						.setEssId("ess0") //
						.build())
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER, -10_000) //
						.input("pvInverter0", ACTIVE_POWER, 10_000) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 10_000) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, 20_000)) //
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER, -10_000) //
						.input("pvInverter0", ACTIVE_POWER, 10_000) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, 16_000)) // 20000 * (1 - 0.2) = 16000
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER, -10_000) //
						.input("pvInverter0", ACTIVE_POWER, 10_000) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, 12_800)) // 16000 * (1 - 0.2) = 12800
				.deactivate();

		// Group 4: No ESS configured — backward compatibility
		// raw = -15_000 + 15_000 + 10_000 = 10_000 → output 10_000
		new ControllerTest(new ControllerPvInverterSellToGridLimitImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummyElectricityMeter("meter0")) //
				.addComponent(new DummyManagedSymmetricPvInverter("pvInverter0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setMeterId("meter0") //
						.setAsymmetricMode(false) //
						.setMaximumSellToGridPower(10_000) //
						.setPvInverterId("pvInverter0") //
						.build())
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER, -15_000) //
						.input("pvInverter0", ACTIVE_POWER, 15_000) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, 10_000)) //
				.deactivate();
	}

	@Test
	public void rateLimitDeadZoneTest() throws Exception {
		// Cycle 1: settle lastSetLimit to 5_000
		// raw = -5_000 + 5_000 + 5_000 = 5_000; lastSetLimit=0 (≤100) → no rate limit → output 5_000
		// Cycle 2: calculatedPower=50 (≤100) → dead-zone bypasses rate limit → output 50
		// raw = -4_950 + 0 + 5_000 = 50
		new ControllerTest(new ControllerPvInverterSellToGridLimitImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummyElectricityMeter("meter0")) //
				.addComponent(new DummyManagedSymmetricPvInverter("pvInverter0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setMeterId("meter0") //
						.setAsymmetricMode(false) //
						.setMaximumSellToGridPower(5_000) //
						.setPvInverterId("pvInverter0") //
						.build())
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER, -5_000) //
						.input("pvInverter0", ACTIVE_POWER, 5_000) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, 5_000)) //
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER, -4_950) //
						.input("pvInverter0", ACTIVE_POWER, 0) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, 50)) //
				.deactivate();
	}

	@Test
	public void essEmptyActivePowerTest() throws Exception {
		// ESS registered but ACTIVE_POWER not set → orElse(0) → same as no-ESS
		// raw = -15_000 + 15_000 + 10_000 + max(0, 0) = 10_000 → output 10_000
		new ControllerTest(new ControllerPvInverterSellToGridLimitImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummyElectricityMeter("meter0")) //
				.addComponent(new DummyManagedSymmetricPvInverter("pvInverter0")) //
				.addComponent(new DummyManagedSymmetricEss("ess0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setMeterId("meter0") //
						.setAsymmetricMode(false) //
						.setMaximumSellToGridPower(10_000) //
						.setPvInverterId("pvInverter0") //
						.setEssId("ess0") //
						.build())
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER, -15_000) //
						.input("pvInverter0", ACTIVE_POWER, 15_000) //
						// No ACTIVE_POWER input for ess0 → Optional.empty() → treated as 0
						.output("pvInverter0", ACTIVE_POWER_LIMIT, 10_000)) //
				.deactivate();
	}

	@Test
	public void essNullIdTest() throws Exception {
		// Blank ess_id exercises the null/blank guard in resolveEssActivePower():
		// "if (essId == null || essId.isBlank()) return 0"
		// The test framework rejects a literal null config value, so "" is used —
		// it is equivalent to null for the guard and produces the same no-ESS result.
		// raw = -15_000 + 15_000 + 10_000 + max(0, 0) = 10_000 → output 10_000
		new ControllerTest(new ControllerPvInverterSellToGridLimitImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummyElectricityMeter("meter0")) //
				.addComponent(new DummyManagedSymmetricPvInverter("pvInverter0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setMeterId("meter0") //
						.setAsymmetricMode(false) //
						.setMaximumSellToGridPower(10_000) //
						.setPvInverterId("pvInverter0") //
						.setEssId("") // empty string triggers the same null/blank guard as null
						.build())
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER, -15_000) //
						.input("pvInverter0", ACTIVE_POWER, 15_000) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, 10_000)) //
				.deactivate();
	}

	@Test
	public void negativePowerRateLimitTest() throws Exception {
		// Cycle 1: raw = 5_000 + 0 + (−10_000) = −5_000; lastSetLimit=0 (≤100) → output −5_000
		// Cycle 2: raw = −5_000; no change → output −5_000
		// Cycle 3: raw = 2_000 + 0 + (−10_000) = −8_000; lastSetLimit=−5_000 → ramp: −5_000 − 1_000 = −6_000
		new ControllerTest(new ControllerPvInverterSellToGridLimitImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummyElectricityMeter("meter0")) //
				.addComponent(new DummyManagedSymmetricPvInverter("pvInverter0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setMeterId("meter0") //
						.setAsymmetricMode(false) //
						.setMaximumSellToGridPower(-10_000) //
						.setPvInverterId("pvInverter0") //
						.build())
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER, 5_000) //
						.input("pvInverter0", ACTIVE_POWER, 0) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, -5_000)) //
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER, 5_000) //
						.input("pvInverter0", ACTIVE_POWER, 0) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, -5_000)) //
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER, 2_000) //
						.input("pvInverter0", ACTIVE_POWER, 0) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, -6_000)) // -5000 - 5000*0.2 = -6000
				.deactivate();
	}
}
