# Remove PvPricing and BatteryPricing UI Widgets — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the Angular flat-tile and modal UI components for `Controller.Evcs.PvPricing` and `Controller.Evcs.BatteryPricing` from the OpenEMS live dashboard; configuration of these controllers moves exclusively to OSGi component settings.

**Architecture:** Delete 10 Angular component files (NgModule, flat tile, modal for each controller) and strip all references from the 5 cross-cutting files that register and render them. The `PricingDashboard` aggregate view and `FixedPricing` widget are untouched. No new code is introduced — this is a pure deletion task.

**Tech Stack:** Angular 17, Ionic, TypeScript; build via `ng build`, lint via `ng lint`, test via Karma/Jasmine.

---

## Files Map

### Files to delete (10)

| File | Role |
|---|---|
| `openems/ui/src/app/edge/live/Controller/Evcs/PvPricing/Evcs_PvPricing.ts` | NgModule that declares flat + modal |
| `openems/ui/src/app/edge/live/Controller/Evcs/PvPricing/flat/flat.ts` | Flat tile component |
| `openems/ui/src/app/edge/live/Controller/Evcs/PvPricing/flat/flat.html` | Flat tile template |
| `openems/ui/src/app/edge/live/Controller/Evcs/PvPricing/modal/modal.ts` | Modal component |
| `openems/ui/src/app/edge/live/Controller/Evcs/PvPricing/modal/modal.html` | Modal template |
| `openems/ui/src/app/edge/live/Controller/Evcs/BatteryPricing/Evcs_BatteryPricing.ts` | NgModule that declares flat + modal |
| `openems/ui/src/app/edge/live/Controller/Evcs/BatteryPricing/flat/flat.ts` | Flat tile component |
| `openems/ui/src/app/edge/live/Controller/Evcs/BatteryPricing/flat/flat.html` | Flat tile template |
| `openems/ui/src/app/edge/live/Controller/Evcs/BatteryPricing/modal/modal.ts` | Modal component |
| `openems/ui/src/app/edge/live/Controller/Evcs/BatteryPricing/modal/modal.html` | Modal template |

### Files to update (5)

| File | Change |
|---|---|
| `openems/ui/src/app/edge/live/live.module.ts` | Remove 2 TS imports + 2 `imports[]` entries |
| `openems/ui/src/app/edge/live/live.component.html` | Remove 2 `<ion-col *ngSwitchCase>` blocks |
| `openems/ui/src/app/shared/type/widget.ts` | Remove 2 `WidgetFactory` enum entries |
| `openems/ui/src/assets/i18n/en.json` | Remove `BatteryPricing` and `PvPricing` translation subtrees |
| `openems/ui/src/assets/i18n/de.json` | Remove `BatteryPricing` and `PvPricing` translation subtrees |

### Do NOT touch

- `Controller/Evcs/FixedPricing/` — kept, still in use
- `Controller/Evcs/PricingDashboard/` (`Core.EvcsPricing`) — kept, still in use
- `Controller/Evcs/evcs-pricing.constants.ts` — shared by FixedPricing + PricingDashboard
- `PricingDashboard/modal/modal.ts` — its `PRICING_FACTORIES` intentionally retains `"Controller.Evcs.PvPricing"` and `"Controller.Evcs.BatteryPricing"` to display aggregate data from those OSGi controllers

---

### Task 1: Delete PvPricing UI files

**Files to delete (5):**
- `openems/ui/src/app/edge/live/Controller/Evcs/PvPricing/Evcs_PvPricing.ts`
- `openems/ui/src/app/edge/live/Controller/Evcs/PvPricing/flat/flat.ts`
- `openems/ui/src/app/edge/live/Controller/Evcs/PvPricing/flat/flat.html`
- `openems/ui/src/app/edge/live/Controller/Evcs/PvPricing/modal/modal.ts`
- `openems/ui/src/app/edge/live/Controller/Evcs/PvPricing/modal/modal.html`

- [ ] **Step 1: Delete all 5 PvPricing files**

Run from repo root:
```bash
rm openems/ui/src/app/edge/live/Controller/Evcs/PvPricing/Evcs_PvPricing.ts
rm openems/ui/src/app/edge/live/Controller/Evcs/PvPricing/flat/flat.ts
rm openems/ui/src/app/edge/live/Controller/Evcs/PvPricing/flat/flat.html
rm openems/ui/src/app/edge/live/Controller/Evcs/PvPricing/modal/modal.ts
rm openems/ui/src/app/edge/live/Controller/Evcs/PvPricing/modal/modal.html
```

- [ ] **Step 2: Verify files are gone**

Run:
```bash
ls openems/ui/src/app/edge/live/Controller/Evcs/PvPricing/
```
Expected: `ls: cannot access ... No such file or directory` (or empty directory listing showing only empty subdirs `flat/` and `modal/` containing nothing). The directory itself can remain empty — the build won't pick it up.

- [ ] **Step 3: Commit**

```bash
git add -A openems/ui/src/app/edge/live/Controller/Evcs/PvPricing/
git commit -m "feat(ui): delete PvPricing UI component files"
```

---

### Task 2: Delete BatteryPricing UI files

**Files to delete (5):**
- `openems/ui/src/app/edge/live/Controller/Evcs/BatteryPricing/Evcs_BatteryPricing.ts`
- `openems/ui/src/app/edge/live/Controller/Evcs/BatteryPricing/flat/flat.ts`
- `openems/ui/src/app/edge/live/Controller/Evcs/BatteryPricing/flat/flat.html`
- `openems/ui/src/app/edge/live/Controller/Evcs/BatteryPricing/modal/modal.ts`
- `openems/ui/src/app/edge/live/Controller/Evcs/BatteryPricing/modal/modal.html`

- [ ] **Step 1: Delete all 5 BatteryPricing files**

Run from repo root:
```bash
rm openems/ui/src/app/edge/live/Controller/Evcs/BatteryPricing/Evcs_BatteryPricing.ts
rm openems/ui/src/app/edge/live/Controller/Evcs/BatteryPricing/flat/flat.ts
rm openems/ui/src/app/edge/live/Controller/Evcs/BatteryPricing/flat/flat.html
rm openems/ui/src/app/edge/live/Controller/Evcs/BatteryPricing/modal/modal.ts
rm openems/ui/src/app/edge/live/Controller/Evcs/BatteryPricing/modal/modal.html
```

- [ ] **Step 2: Verify files are gone**

Run:
```bash
ls openems/ui/src/app/edge/live/Controller/Evcs/BatteryPricing/
```
Expected: empty directory listing (or `No such file or directory`).

- [ ] **Step 3: Commit**

```bash
git add -A openems/ui/src/app/edge/live/Controller/Evcs/BatteryPricing/
git commit -m "feat(ui): delete BatteryPricing UI component files"
```

---

### Task 3: Update live.module.ts

**File:** `openems/ui/src/app/edge/live/live.module.ts`

- [ ] **Step 1: Remove the two import lines**

Open `openems/ui/src/app/edge/live/live.module.ts`. Delete these two lines (currently lines 25 and 28):

```ts
import { Controller_Evcs_BatteryPricing } from "./Controller/Evcs/BatteryPricing/Evcs_BatteryPricing";
import { Controller_Evcs_PvPricing } from "./Controller/Evcs/PvPricing/Evcs_PvPricing";
```

- [ ] **Step 2: Remove the two entries from the `imports[]` array**

In the same file, inside `@NgModule({ imports: [ ... ] })`, delete these two lines (currently lines 86 and 88):

```ts
    Controller_Evcs_BatteryPricing,
    Controller_Evcs_PvPricing,
```

After editing, the relevant portion of `imports[]` should look like:

```ts
    Controller_Evcs,
    Controller_Evcs_FixedPricing,
    Core_EvcsPricing,
```

- [ ] **Step 3: Verify no dangling references**

Run:
```bash
grep -n "BatteryPricing\|PvPricing" openems/ui/src/app/edge/live/live.module.ts
```
Expected: no output (zero matches).

- [ ] **Step 4: Commit**

```bash
git add openems/ui/src/app/edge/live/live.module.ts
git commit -m "feat(ui): remove BatteryPricing and PvPricing from live.module.ts"
```

---

### Task 4: Update live.component.html

**File:** `openems/ui/src/app/edge/live/live.component.html`

- [ ] **Step 1: Delete the BatteryPricing `<ion-col>` switch block**

Find and delete this block in its entirety:

```html
<ion-col size="12" *ngSwitchCase="'Controller.Evcs.BatteryPricing'" size-lg="6" class="ion-no-padding">
  <Controller_Evcs_BatteryPricing [componentId]="widget.componentId">
  </Controller_Evcs_BatteryPricing>
</ion-col>
```

- [ ] **Step 2: Delete the PvPricing `<ion-col>` switch block**

Find and delete this block in its entirety:

```html
<ion-col size="12" *ngSwitchCase="'Controller.Evcs.PvPricing'" size-lg="6" class="ion-no-padding">
  <Controller_Evcs_PvPricing [componentId]="widget.componentId">
  </Controller_Evcs_PvPricing>
</ion-col>
```

- [ ] **Step 3: Verify remaining switch cases are intact**

Run:
```bash
grep -n "ngSwitchCase\|FixedPricing\|EvcsPricing" openems/ui/src/app/edge/live/live.component.html
```
Expected: lines for `Controller.Evcs.FixedPricing` and `Core.EvcsPricing` (and others) are still present. Lines for `BatteryPricing` and `PvPricing` do NOT appear.

- [ ] **Step 4: Verify no dangling references**

Run:
```bash
grep -n "BatteryPricing\|PvPricing" openems/ui/src/app/edge/live/live.component.html
```
Expected: no output.

- [ ] **Step 5: Commit**

```bash
git add openems/ui/src/app/edge/live/live.component.html
git commit -m "feat(ui): remove BatteryPricing and PvPricing switch cases from live template"
```

---

### Task 5: Update widget.ts

**File:** `openems/ui/src/app/shared/type/widget.ts`

- [ ] **Step 1: Remove the two `WidgetFactory` enum entries**

Open `openems/ui/src/app/shared/type/widget.ts`. Delete these two lines from the `WidgetFactory` enum (currently lines 38 and 40):

```ts
    "Controller.Evcs.BatteryPricing",
    "Controller.Evcs.PvPricing",
```

After editing, the surrounding enum entries should look like:

```ts
    "Controller.Ess.Time-Of-Use-Tariff",
    "Controller.Evcs.FixedPricing",
    "Core.EvcsPricing",
```

- [ ] **Step 2: Verify removal**

Run:
```bash
grep -n "BatteryPricing\|PvPricing" openems/ui/src/app/shared/type/widget.ts
```
Expected: no output.

- [ ] **Step 3: Commit**

```bash
git add openems/ui/src/app/shared/type/widget.ts
git commit -m "feat(ui): remove BatteryPricing and PvPricing from WidgetFactory enum"
```

---

### Task 6: Update i18n translation files

**Files:**
- `openems/ui/src/assets/i18n/en.json`
- `openems/ui/src/assets/i18n/de.json`

Both files contain a subtree under `Edge.Index.Widgets.EVCS` with `"BatteryPricing": { ... }` (8 lines including braces) and `"PvPricing": { ... }` (8 lines including braces).

#### en.json

In `en.json`, the current structure inside `"EVCS": { ... }` is (lines ~222–250):

```json
          "BatteryPricing": {
            "lowSocThreshold": "Low SoC Threshold [%]",
            "highSocThreshold": "High SoC Threshold [%]",
            "lowSocFloorPrice": "Low SoC Floor Price [€/kWh]",
            "highSocCeilPrice": "High SoC Ceiling Price [€/kWh]",
            "fullCeilPrice": "Full SoC Ceiling Price [€/kWh]",
            "dataCollectionWindow": "Data Collection Window [min]",
            "info": "Sets price constraints based on battery SoC. Low SoC raises a price floor, high SoC lowers a ceiling to encourage EV charging."
          },
          ...
          "PvPricing": {
            "maxCeiling": "Max Ceiling [€/kWh]",
            "minCeiling": "Min Ceiling [€/kWh]",
            "pvThreshold": "PV Threshold [W]",
            "pvFullProduction": "PV Full Production [W]",
            "dataCollectionWindow": "Data Collection Window [min]",
            "info": "Reduces the EVCS price based on PV production. When production exceeds the threshold, the ceiling decreases linearly from max to min."
          }
```

- [ ] **Step 1: Delete the `BatteryPricing` subtree from `en.json`**

Remove the entire `"BatteryPricing": { ... },` block (9 lines including the trailing comma).

- [ ] **Step 2: Delete the `PvPricing` subtree from `en.json`**

Remove the entire `"PvPricing": { ... }` block (8 lines). Note: this was the last entry before the closing `}` of `EVCS`, so there must be no trailing comma on the now-last entry (`"FixedPricing"` block or `"PricingDashboard"` block, whichever precedes it).

> **JSON trailing comma rule:** After deleting `PvPricing`, the entry just above it (currently `"PricingDashboard": { ... }`) must not have a trailing comma if it becomes the last entry inside `"EVCS"`. Verify the JSON is valid after editing.

- [ ] **Step 3: Validate `en.json`**

Run:
```bash
node -e "require('./openems/ui/src/assets/i18n/en.json'); console.log('valid')"
```
Expected: `valid`

#### de.json

The German file has identical structure; the entries to remove are:

```json
          "BatteryPricing": {
            "lowSocThreshold": "Niedrig-SoC-Schwelle [%]",
            "highSocThreshold": "Hoch-SoC-Schwelle [%]",
            "lowSocFloorPrice": "Mindestpreis bei niedrigem SoC [€/kWh]",
            "highSocCeilPrice": "Höchstpreis bei hohem SoC [€/kWh]",
            "fullCeilPrice": "Höchstpreis bei vollem SoC [€/kWh]",
            "dataCollectionWindow": "Datenerfassungsfenster [min]",
            "info": "Setzt Preisbeschränkungen basierend auf dem Batterie-SoC. Niedriger SoC setzt einen Mindestpreis, hoher SoC senkt den Höchstpreis um das Laden zu fördern."
          },
          ...
          "PvPricing": {
            "maxCeiling": "Max. Höchstpreis [€/kWh]",
            "minCeiling": "Min. Höchstpreis [€/kWh]",
            "pvThreshold": "PV-Schwelle [W]",
            "pvFullProduction": "PV-Vollproduktion [W]",
            "dataCollectionWindow": "Datenerfassungsfenster [min]",
            "info": "Senkt den EVCS-Preis basierend auf PV-Produktion. Wenn die Produktion die Schwelle überschreitet, sinkt der Höchstpreis linear von Max. zu Min."
          }
```

- [ ] **Step 4: Delete the `BatteryPricing` subtree from `de.json`**

Same procedure as en.json Step 1.

- [ ] **Step 5: Delete the `PvPricing` subtree from `de.json`**

Same procedure as en.json Step 2 — ensure no trailing comma on the new last entry inside `"EVCS"`.

- [ ] **Step 6: Validate `de.json`**

Run:
```bash
node -e "require('./openems/ui/src/assets/i18n/de.json'); console.log('valid')"
```
Expected: `valid`

- [ ] **Step 7: Verify no traces remain in either file**

Run:
```bash
grep -n "BatteryPricing\|PvPricing" openems/ui/src/assets/i18n/en.json openems/ui/src/assets/i18n/de.json
```
Expected: no output.

- [ ] **Step 8: Commit**

```bash
git add openems/ui/src/assets/i18n/en.json openems/ui/src/assets/i18n/de.json
git commit -m "feat(ui): remove BatteryPricing and PvPricing i18n translation entries"
```

---

### Task 7: Build and lint verification

- [ ] **Step 1: Run the Angular build**

Run from repo root:
```bash
cd openems/ui && node_modules/.bin/ng build -c openems-edge-dev
```
Expected: build completes with `✔ Browser application bundle generation complete` and **zero errors**. TypeScript compilation errors (e.g. unresolved imports, unknown selectors) would appear here if any reference was missed.

- [ ] **Step 2: Run the linter**

Run from repo root:
```bash
cd openems/ui && node_modules/.bin/ng lint
```
Expected: `All files pass linting.` with zero errors or warnings that weren't present before this change.

- [ ] **Step 3: Fix any build or lint failures**

If either command fails:
1. Read the error output carefully — it will name the file and line.
2. Search for remaining `BatteryPricing` or `PvPricing` references in the UI source:
   ```bash
   grep -rn "BatteryPricing\|PvPricing" openems/ui/src/
   ```
3. Fix the reference and re-run the failing command.
4. Commit fixes with: `fix(ui): resolve remaining BatteryPricing/PvPricing reference`

- [ ] **Step 4: Commit (if fixes were needed)**

If no fixes were required, no additional commit is needed — Tasks 1–6 commits are sufficient.

---

### Task 8: Test verification

- [ ] **Step 1: Run the test suite**

Run from repo root:
```bash
cd openems/ui && npm run test -- --no-watch --browsers=ChromeHeadlessCI
```
Expected: all tests pass; output ends with something like `SUMMARY: X specs, 0 failures`.

- [ ] **Step 2: Fix any failing tests**

If tests fail, check whether the failure is related to this change:
- Look for test files that import from `BatteryPricing` or `PvPricing` paths:
  ```bash
  grep -rn "BatteryPricing\|PvPricing" openems/ui/src/ --include="*.spec.ts"
  ```
- If found, delete or update those test files accordingly, then re-run.
- If failures are pre-existing and unrelated to this change, document them and do not fix.

- [ ] **Step 3: Commit test fixes (if any)**

```bash
git add <changed spec files>
git commit -m "fix(ui): remove BatteryPricing/PvPricing test references"
```
