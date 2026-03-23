// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { ChannelAddress, CurrentData, Utils } from "src/app/shared/shared";
import { DefaultTypes } from "src/app/shared/type/defaulttypes";

import { ModalComponent } from "../modal/modal";

@Component({
  selector: "Controller_Evcs_FixedPricing",
  templateUrl: "./flat.html",
  standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {

  private static readonly EVCS_PRICING_ID = "_evcsPricing";

  public currentPrice: number | null = null;
  public propertyMode: DefaultTypes.ManualOnOff | null = null;
  public readonly CONVERT_MANUAL_ON_OFF = Utils.CONVERT_MANUAL_ON_OFF(this.translate);

  async presentModal() {
    if (!this.isInitialized) {
      return;
    }
    const modal = await this.modalController.create({
      component: ModalComponent,
      componentProps: {
        component: this.component,
      },
    });
    return await modal.present();
  }

  protected override getChannelAddresses(): ChannelAddress[] {
    return [
      // Read price from the singleton Core.EvcsPricing component
      new ChannelAddress(FlatComponent.EVCS_PRICING_ID, "Price"),
      new ChannelAddress(this.component.id, "_PropertyMode"),
    ];
  }

  protected override onCurrentData(currentData: CurrentData) {
    this.currentPrice = currentData.allComponents[FlatComponent.EVCS_PRICING_ID + "/Price"];
    this.propertyMode = currentData.allComponents[this.component.id + "/_PropertyMode"];
  }

}

