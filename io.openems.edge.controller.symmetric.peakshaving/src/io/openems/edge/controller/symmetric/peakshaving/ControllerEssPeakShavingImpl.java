package io.openems.edge.controller.symmetric.peakshaving;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.meter.api.ElectricityMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
        name = "Controller.Symmetric.PeakShaving", //
        immediate = true, //
        configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerEssPeakShavingImpl extends AbstractOpenemsComponent
        implements ControllerEssPeakShaving, Controller, OpenemsComponent {

    public static final double DEFAULT_MAX_ADJUSTMENT_RATE = 0.2;

    private final Logger log = LoggerFactory.getLogger(ControllerEssPeakShavingImpl.class);

    @Reference
    private ComponentManager componentManager;

    private Config config;

    public ControllerEssPeakShavingImpl() {
        super(//
                OpenemsComponent.ChannelId.values(), //
                Controller.ChannelId.values(), //
                ControllerEssPeakShaving.ChannelId.values() //
        );
    }

    @Activate
    private void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.config = config;
    }

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void run() throws OpenemsNamedException {
        ManagedSymmetricEss ess = this.componentManager.getComponent(this.config.ess_id());
        /*
         * Check that the SoC is in the defined range
         */
        var currentSoC = ess.getSoc().getOrError();
        if (currentSoC < this.config.socInfimum() || currentSoC > this.config.socSupremum()) {
            return;
        }

        /*
         * Check that we are On-Grid (and warn on undefined Grid-Mode)
         */
        var gridMode = ess.getGridMode();
        if (gridMode.isUndefined()) {
            this.logWarn(this.log, "Grid-Mode is [UNDEFINED]");
        }
        switch (gridMode) {
            case ON_GRID:
            case UNDEFINED:
                break;
            case OFF_GRID:
                return;
        }

        ElectricityMeter meter = this.componentManager.getComponent(this.config.meter_id());

        // Calculate 'real' grid-power (without current ESS charge/discharge)
        var gridPower = meter.getActivePower().getOrError()/* current buy-from/sell-to grid */
                + ess.getActivePower().getOrError() /* current charge/discharge Ess */;

        if (gridPower >= this.config.peakShavingPower()) {
            /*
             * Peak-Shaving
             */
            ess.setActivePowerLessOrEquals(gridPower - this.config.peakShavingPower());

        } else if (gridPower <= this.config.rechargePower()) {
            /*
             * Recharge
             */
            ess.setActivePowerGreaterOrEquals(gridPower - this.config.rechargePower());
        } else {
            /*
             * Battery On Hold
             */
            ess.setActivePowerEquals(0);
        }
    }
}
