// @ts-strict-ignore
import { Component } from "@angular/core";
import { FormControl, FormGroup } from "@angular/forms";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { ChannelAddress, CurrentData } from "src/app/shared/shared";

@Component({
  templateUrl: "./modal.html",
  standalone: false,
})
export class ModalComponent extends AbstractModal {

  private static readonly EVCS_PRICING_ID = "_evcsPricing";

  public currentPrice: number | null = null;

  protected override getChannelAddresses(): ChannelAddress[] {
    return [
      new ChannelAddress(ModalComponent.EVCS_PRICING_ID, "Price"),
    ];
  }

  protected override onCurrentData(currentData: CurrentData) {
    this.currentPrice = currentData.allComponents[ModalComponent.EVCS_PRICING_ID + "/Price"];
  }

  protected override getFormGroup(): FormGroup {
    return this.formBuilder.group({
      lowSocThreshold: new FormControl(this.component.properties.lowSocThreshold),
      highSocThreshold: new FormControl(this.component.properties.highSocThreshold),
      lowSocFloorPrice: new FormControl(this.component.properties.lowSocFloorPrice),
      highSocCeilPrice: new FormControl(this.component.properties.highSocCeilPrice),
      fullCeilPrice: new FormControl(this.component.properties.fullCeilPrice),
      dataCollectionWindowMinutes: new FormControl(this.component.properties.dataCollectionWindowMinutes),
    });
  }
}
