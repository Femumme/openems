// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { ChannelAddress, CurrentData } from "src/app/shared/shared";

import { ModalComponent } from "../modal/modal";

@Component({
  selector: "Controller_Evcs_PvPricing",
  templateUrl: "./flat.html",
  standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {

  private static readonly EVCS_PRICING_ID = "_evcsPricing";

  public currentPrice: number | null = null;

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
      new ChannelAddress(FlatComponent.EVCS_PRICING_ID, "Price"),
    ];
  }

  protected override onCurrentData(currentData: CurrentData) {
    this.currentPrice = currentData.allComponents[FlatComponent.EVCS_PRICING_ID + "/Price"];
  }

}
