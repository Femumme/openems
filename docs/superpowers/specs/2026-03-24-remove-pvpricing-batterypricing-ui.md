# Spec: Remove PvPricing & BatteryPricing UI Widgets

**Date:** 2026-03-24  
**Status:** Ready to implement  
**Scope:** Frontend only — Angular UI (`openems/ui/`)

## Goal

Delete all live-dashboard UI for `ctrlEvcsPvPricing` and `ctrlEvcsBatteryPricing`. Configuration moves exclusively to OSGi (Apache Felix Web Console / config files).

## Files to Delete (10)

All paths relative to `openems/ui/src/app/edge/live/`:

| File |
|---|
| `Controller/Evcs/PvPricing/Evcs_PvPricing.ts` |
| `Controller/Evcs/PvPricing/flat/flat.ts` |
| `Controller/Evcs/PvPricing/flat/flat.html` |
| `Controller/Evcs/PvPricing/modal/modal.ts` |
| `Controller/Evcs/PvPricing/modal/modal.html` |
| `Controller/Evcs/BatteryPricing/Evcs_BatteryPricing.ts` |
| `Controller/Evcs/BatteryPricing/flat/flat.ts` |
| `Controller/Evcs/BatteryPricing/flat/flat.html` |
| `Controller/Evcs/BatteryPricing/modal/modal.ts` |
| `Controller/Evcs/BatteryPricing/modal/modal.html` |

## Files to Update (5)

### 1. `live.module.ts`

Remove 2 TypeScript imports and 2 entries from `imports[]`:

```ts
// DELETE these import lines:
import { Controller_Evcs_BatteryPricing } from './Controller/Evcs/BatteryPricing/Evcs_BatteryPricing';
import { Controller_Evcs_PvPricing } from './Controller/Evcs/PvPricing/Evcs_PvPricing';

// DELETE from imports[]:
Controller_Evcs_BatteryPricing,
Controller_Evcs_PvPricing,
```

### 2. `live.component.html`

Remove the 2 `*ngSwitchCase` blocks:

```html
<!-- DELETE: -->
<ion-col size="12" *ngSwitchCase="'Controller.Evcs.BatteryPricing'" size-lg="6" class="ion-no-padding">
  <Controller_Evcs_BatteryPricing [componentId]="widget.componentId">
  </Controller_Evcs_BatteryPricing>
</ion-col>

<ion-col size="12" *ngSwitchCase="'Controller.Evcs.PvPricing'" size-lg="6" class="ion-no-padding">
  <Controller_Evcs_PvPricing [componentId]="widget.componentId">
  </Controller_Evcs_PvPricing>
</ion-col>
```

### 3. `openems/ui/src/app/shared/type/widget.ts`

Remove 2 `WidgetFactory` enum entries:

```ts
// DELETE:
"Controller.Evcs.BatteryPricing",
"Controller.Evcs.PvPricing",
```

### 4. `openems/ui/src/assets/i18n/en.json`

Remove translation subtree under:

```json
"Edge": { "Index": { "Widgets": { "EVCS": {
  "BatteryPricing": { ... },  // DELETE
  "PvPricing": { ... }        // DELETE
}}}}
```

### 5. `openems/ui/src/assets/i18n/de.json`

Same removal as `en.json` for German translations.

## Out of Scope (do NOT touch)

| Path | Reason |
|---|---|
| `Controller/Evcs/FixedPricing/` | Kept — still in use |
| `Controller/Evcs/PricingDashboard/` (`Core.EvcsPricing`) | Kept — still in use |
| `Controller/Evcs/evcs-pricing.constants.ts` | Shared by FixedPricing + PricingDashboard |
| `PricingDashboard/modal/modal.ts` | `PRICING_FACTORIES` intentionally retains `"Controller.Evcs.PvPricing"` and `"Controller.Evcs.BatteryPricing"` so the aggregate dashboard continues to display data from those OSGi controllers. |

## Verification

Run from `openems/ui/`:

```sh
# 1. TypeScript/template compile check
node_modules/.bin/ng build -c openems-edge-dev

# 2. Lint
node_modules/.bin/ng lint

# 3. Tests
npm run test -- --no-watch --browsers=ChromeHeadlessCI
```

All three must pass with no errors.
