// @ts-strict-ignore
import { Component } from "@angular/core";
import { FormControl, FormGroup } from "@angular/forms";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { ChannelAddress, CurrentData, Utils } from "src/app/shared/shared";

@Component({
  templateUrl: "./modal.html",
  standalone: false,
})
export class ModalComponent extends AbstractModal {

  private static readonly EVCS_PRICING_ID = "_evcsPricing";

  public currentPrice: number | null = null;
  public readonly CONVERT_MANUAL_ON_OFF = Utils.CONVERT_MANUAL_ON_OFF(this.translate);

  protected override getChannelAddresses(): ChannelAddress[] {
    return [
      // Read price from the singleton Core.EvcsPricing component
      new ChannelAddress(ModalComponent.EVCS_PRICING_ID, "Price"),
    ];
  }

  protected override onCurrentData(currentData: CurrentData) {
    this.currentPrice = currentData.allComponents[ModalComponent.EVCS_PRICING_ID + "/Price"];
  }

  protected override getFormGroup(): FormGroup {
    return this.formBuilder.group({
      mode: new FormControl(this.component.properties.mode),
      priceEurPerKwh: new FormControl(this.component.properties.priceEurPerKwh),
    });
  }
}
