package io.openems.edge.controller.pvinverter.selltogridlimit;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.PvInverter.SellToGridLimit", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerPvInverterSellToGridLimitImpl extends AbstractOpenemsComponent
		implements ControllerPvInverterSellToGridLimit, Controller, OpenemsComponent {

	public static final double DEFAULT_MAX_ADJUSTMENT_RATE = 0.2;

	private static final int RATE_LIMIT_DEAD_ZONE_W = 100;

	@Reference
	private ComponentManager componentManager;

	private Config config;
	private int lastSetLimit = 0;

	public ControllerPvInverterSellToGridLimitImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerPvInverterSellToGridLimit.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		this.lastSetLimit = 0; // cold-start: 0 is ≤ RATE_LIMIT_DEAD_ZONE_W, so the first cycle bypasses rate limiting
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	/**
	 * Calculates the required power limit in symmetric (3-phase aggregate) mode.
	 *
	 * @param pvInverter     the ManagedSymmetricPvInverter
	 * @param meter          the ElectricityMeter
	 * @param essActivePower ESS active power (positive = discharging); 0 if no ESS configured
	 * @return the required power limit in W
	 * @throws InvalidValueException on missing channel value
	 */
	private int calculateSymmetricPower(ManagedSymmetricPvInverter pvInverter, ElectricityMeter meter,
			int essActivePower) throws InvalidValueException {
		var gridPower = meter.getActivePower().getOrError();
		return gridPower /* current buy-from/sell-to grid */
				+ pvInverter.getActivePower().getOrError() /* current production */
				+ this.config.maximumSellToGridPower() /* the configured limit */
				+ Math.max(0, essActivePower); /* ESS discharge offsets the limit (charging ignored) */
	}

	/**
	 * Calculates the required power limit in asymmetric (per-phase minimum) mode.
	 *
	 * <p>Uses the minimum phase power multiplied by 3 to guard against the most
	 * constrained phase, and triples the configured sell-to-grid limit accordingly.
	 *
	 * <p>TODO: Optimize for Single-Phase PV-Inverter
	 *
	 * @param pvInverter     the ManagedSymmetricPvInverter
	 * @param meter          the ElectricityMeter
	 * @param essActivePower ESS active power (positive = discharging); 0 if no ESS configured
	 * @return the required power limit in W
	 * @throws InvalidValueException on missing channel value
	 */
	private int calculateAsymmetricPower(ManagedSymmetricPvInverter pvInverter, ElectricityMeter meter,
			int essActivePower) throws InvalidValueException {
		var gridPowerL1 = meter.getActivePowerL1().getOrError();
		var gridPowerL2 = meter.getActivePowerL2().getOrError();
		var gridPowerL3 = meter.getActivePowerL3().getOrError();

		var minPowerOnPhase = Math.min(Math.min(gridPowerL1, gridPowerL2), gridPowerL3);
		var gridPower = minPowerOnPhase * 3;
		var maximumSellToGridPower = this.config.maximumSellToGridPower() * 3;

		return gridPower /* current buy-from/sell-to grid */
				+ pvInverter.getActivePower().getOrError() /* current production */
				+ maximumSellToGridPower /* the configured limit (scaled to 3-phase) */
				+ Math.max(0, essActivePower); /* ESS discharge offsets the limit (charging ignored) */
	}

	/**
	 * Returns the ESS active power (positive = discharging) or 0 if no ESS is configured.
	 *
	 * @return ESS active power in W, or 0
	 * @throws OpenemsNamedException if the configured ESS component cannot be found
	 */
	private int resolveEssActivePower() throws OpenemsNamedException {
		var essId = this.config.ess_id();
		if (essId == null || essId.isBlank()) {
			return 0;
		}
		SymmetricEss ess = this.componentManager.getComponent(essId);
		return ess.getActivePower().orElse(0);
	}

	/**
	 * Clamps {@code targetPower} so that the step change from the last set limit
	 * does not exceed {@link #DEFAULT_MAX_ADJUSTMENT_RATE} per cycle.
	 *
	 * <p>The dead-zone ({@link #RATE_LIMIT_DEAD_ZONE_W}) prevents rate-limiting
	 * near zero where the percentage-based threshold would be meaninglessly small.
	 *
	 * @param targetPower the unclamped target power in W
	 * @return the rate-limited power in W (stored as the new {@link #lastSetLimit})
	 */
	private int applyRateLimit(int targetPower) {
		var maxStep = (int) Math.abs(this.lastSetLimit * DEFAULT_MAX_ADJUSTMENT_RATE);
		if (Math.abs(this.lastSetLimit) > RATE_LIMIT_DEAD_ZONE_W
				&& Math.abs(targetPower) > RATE_LIMIT_DEAD_ZONE_W
				&& Math.abs(this.lastSetLimit - targetPower) > maxStep) {
			if (this.lastSetLimit > targetPower) {
				targetPower = this.lastSetLimit - maxStep;
			} else {
				targetPower = this.lastSetLimit + maxStep;
			}
		}
		this.lastSetLimit = targetPower;
		return targetPower;
	}

	@Override
	public void run() throws OpenemsNamedException {
		ManagedSymmetricPvInverter pvInverter = this.componentManager.getComponent(this.config.pvInverter_id());
		ElectricityMeter meter = this.componentManager.getComponent(this.config.meter_id());

		var essActivePower = this.resolveEssActivePower();

		// Calculates required charge/discharge power
		var calculatedPower = this.config.asymmetricMode()
				? this.calculateAsymmetricPower(pvInverter, meter, essActivePower)
				: this.calculateSymmetricPower(pvInverter, meter, essActivePower);

		pvInverter.setActivePowerLimit(this.applyRateLimit(calculatedPower));
	}
}
