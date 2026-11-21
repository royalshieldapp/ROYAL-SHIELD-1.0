# Lifetime Plans Reference Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the Royal Shield lifetime-plan paywall to match the supplied premium three-card reference while preserving truthful Google Play prices and the existing verified `INAPP` purchase flow.

**Architecture:** Extract plan display metadata and purchase-enablement rules into a pure Kotlin presentation model, then render a native Compose hierarchy from real `ProductInfo` values. `PaywallViewModel` continues to own catalog, selection, purchase, restore, and success state, but stops modifying Google Play's formatted prices locally.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, StateFlow/ViewModel, Google Play Billing `ProductDetails`, JUnit 4.

## Global Constraints

- Products remain `lifetime_starter`, `lifetime_gold`, and `lifetime_ultimate`, queried as Google Play `INAPP`.
- Display only the localized `ProductInfo.price` returned from Google Play; never hardcode a currency value.
- Label every plan `one-time purchase`; never display `/month`, `monthly`, or `subscription` for these products.
- Do not calculate a local discount that differs from the amount Google Play will charge.
- Preserve purchase, cancellation, restore, server verification, success navigation, and `Continue with Limited Features`.
- Golden is selected by default only after all three real products load.
- Registration, Splash, Dashboard, Cyber, Billing product IDs/type/verification, and unrelated screens must not change.
- Do not commit, push, deploy, edit Play Console, or initiate a charge.

---

## File structure

- Create `app/src/main/java/com/royalshield/app/ui/screens/PaywallPlanPresentation.kt`: pure plan metadata, product-to-plan mapping, price integrity, and action-enablement rules.
- Create `app/src/test/java/com/royalshield/app/ui/screens/PaywallPlanPresentationTest.kt`: mapping, price, ordering, one-time labels, and button-state tests.
- Modify `app/src/main/java/com/royalshield/app/ui/viewmodels/PaywallViewModel.kt`: remove local display-price mutation and preserve the real catalog price.
- Modify `app/src/main/java/com/royalshield/app/ui/screens/PremiumEliteScreen.kt`: native Compose implementation of the approved visual hierarchy and interaction states.
- Create `app/src/test/java/com/royalshield/app/ui/screens/PremiumEliteScreenContractTest.kt`: focused source/resource contract for forbidden monthly/fallback copy and required actions.

### Task 1: Truthful plan presentation model

**Files:**
- Create: `app/src/main/java/com/royalshield/app/ui/screens/PaywallPlanPresentation.kt`
- Create: `app/src/test/java/com/royalshield/app/ui/screens/PaywallPlanPresentationTest.kt`

**Interfaces:**
- Consumes: `ProductInfo`, `BillingManager.PRODUCT_STARTER`, `PRODUCT_GOLD`, and `PRODUCT_ULTIMATE`.
- Produces: `PaywallPlanStyle`, `PaywallPlanPresentation`, `toPaywallPlanPresentations(products)`, and `isPaywallContinueEnabled(selectedProductId, purchasing, plans)`.

- [ ] **Step 1: Write failing mapping and truthfulness tests**

```kotlin
class PaywallPlanPresentationTest {
    private fun product(id: String, price: String) = ProductInfo(
        id = id,
        title = id,
        description = "from Play",
        price = price,
    )

    @Test fun maps_complete_catalog_in_reference_order_without_changing_prices() {
        val plans = toPaywallPlanPresentations(listOf(
            product(BillingManager.PRODUCT_ULTIMATE, "US$39.49"),
            product(BillingManager.PRODUCT_STARTER, "US$9.99"),
            product(BillingManager.PRODUCT_GOLD, "US$19.99"),
        ))
        assertEquals(listOf("STARTER", "GOLDEN", "ULTIMATE"), plans.map { it.title })
        assertEquals(listOf("US$9.99", "US$19.99", "US$39.49"), plans.map { it.formattedPrice })
        assertTrue(plans.all { it.purchaseLabel == "one-time purchase" })
    }

    @Test fun continue_requires_real_selected_plan_and_non_purchasing_state() {
        val plans = toPaywallPlanPresentations(listOf(product(BillingManager.PRODUCT_GOLD, "€19.99")))
        assertTrue(isPaywallContinueEnabled(BillingManager.PRODUCT_GOLD, false, plans))
        assertFalse(isPaywallContinueEnabled(BillingManager.PRODUCT_GOLD, true, plans))
        assertFalse(isPaywallContinueEnabled("missing", false, plans))
    }
}
```

- [ ] **Step 2: Run the new test and confirm RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.royalshield.app.ui.screens.PaywallPlanPresentationTest" --console=plain
```

Expected: compilation failure because the presentation types/functions do not exist.

- [ ] **Step 3: Implement the pure presentation model**

```kotlin
enum class PaywallPlanStyle { STARTER, GOLDEN, ULTIMATE }

data class PaywallPlanPresentation(
    val productId: String,
    val title: String,
    val description: String,
    val formattedPrice: String,
    val purchaseLabel: String = "one-time purchase",
    val style: PaywallPlanStyle,
    val mostPopular: Boolean = false,
)

fun toPaywallPlanPresentations(products: List<ProductInfo>): List<PaywallPlanPresentation> {
    val byId = products.associateBy(ProductInfo::id)
    return listOfNotNull(
        byId[BillingManager.PRODUCT_STARTER]?.let { PaywallPlanPresentation(it.id, "STARTER", "Essential protection", it.price, style = PaywallPlanStyle.STARTER) },
        byId[BillingManager.PRODUCT_GOLD]?.let { PaywallPlanPresentation(it.id, "GOLDEN", "Advanced protection", it.price, style = PaywallPlanStyle.GOLDEN, mostPopular = true) },
        byId[BillingManager.PRODUCT_ULTIMATE]?.let { PaywallPlanPresentation(it.id, "ULTIMATE", "Complete protection", it.price, style = PaywallPlanStyle.ULTIMATE) },
    )
}

fun isPaywallContinueEnabled(
    selectedProductId: String?,
    purchasing: Boolean,
    plans: List<PaywallPlanPresentation>,
): Boolean = !purchasing && plans.any { it.productId == selectedProductId }
```

- [ ] **Step 4: Run the focused test and confirm GREEN**

Run the Step 2 command. Expected: all `PaywallPlanPresentationTest` tests pass.

### Task 2: Preserve Google Play prices in ViewModel state

**Files:**
- Modify: `app/src/main/java/com/royalshield/app/ui/viewmodels/PaywallViewModel.kt`
- Modify: `app/src/test/java/com/royalshield/app/ui/screens/PremiumEliteScreenContractTest.kt` after creating it in this task.

**Interfaces:**
- Consumes: real `ProductInfo` objects returned by `BillingManager.queryProducts()`.
- Produces: `ProductsReady` with those same formatted price strings and Golden as the default selected ID.

- [ ] **Step 1: Add a failing source contract test for local price mutation**

```kotlin
@Test fun view_model_does_not_calculate_a_display_only_discount() {
    val source = projectFile("app/src/main/java/com/royalshield/app/ui/viewmodels/PaywallViewModel.kt").readText()
    assertFalse(source.contains("applyDiscountToPriceString"))
    assertFalse(source.contains("originalPrice * 0.90"))
}
```

The test helper resolves the repository root by walking upward from `user.dir` until `settings.gradle.kts` exists.

- [ ] **Step 2: Run the contract test and confirm RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.royalshield.app.ui.screens.PremiumEliteScreenContractTest.view_model_does_not_calculate_a_display_only_discount" --console=plain
```

Expected: FAIL because the current ViewModel contains both strings.

- [ ] **Step 3: Remove local price rewriting**

Delete `applyDiscountToPriceString` and `applyDiscountIfEligible`. Construct `ProductsReady` with the exact products returned by Play:

```kotlin
_uiState.value = PaywallUiState.ProductsReady(
    starter = starter,
    gold = gold,
    ultimate = ultimate,
)
_selectedProductId.value = BillingManager.PRODUCT_GOLD
```

Keep the QR-discount preference out of displayed billing price unless Play later supplies a matching offer.

- [ ] **Step 4: Run the contract and billing policy tests**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.royalshield.app.ui.screens.PremiumEliteScreenContractTest" --tests "com.royalshield.app.billing.BillingCatalogPolicyTest" --console=plain
```

Expected: pass.

### Task 3: Implement the approved Compose paywall

**Files:**
- Modify: `app/src/main/java/com/royalshield/app/ui/screens/PremiumEliteScreen.kt`
- Modify: `app/src/test/java/com/royalshield/app/ui/screens/PremiumEliteScreenContractTest.kt`

**Interfaces:**
- Consumes: `PaywallUiState`, `selectedProductId`, `toPaywallPlanPresentations`, existing shield drawables, `viewModel.selectProduct`, `purchaseSelected`, `restorePurchases`, `onContinue`, and `onPurchaseSuccess`.
- Produces: `LifetimePlanCard`, `PaywallBottomActions`, and the approved screen hierarchy.

- [ ] **Step 1: Add failing UI source-contract tests**

```kotlin
@Test fun paywall_uses_reference_hierarchy_and_one_time_copy() {
    val source = projectFile("app/src/main/java/com/royalshield/app/ui/screens/PremiumEliteScreen.kt").readText()
    assertTrue(source.contains("CHOOSE YOUR PROTECTION"))
    assertTrue(source.contains("Select the plan that fits your security needs."))
    assertTrue(source.contains("Restore purchases"))
    assertTrue(source.contains("Continue with Limited Features"))
    assertFalse(source.contains("/month"))
    assertFalse(Regex("\\$\\d+\\.\\d{2}").containsMatchIn(source))
}
```

- [ ] **Step 2: Run the UI contract test and confirm RED**

Run the Task 2 Step 4 command. Expected: FAIL because the approved header and casing are not present and fallback prices remain.

- [ ] **Step 3: Replace the current card hierarchy with native Compose**

Implement:

```kotlin
@Composable
private fun LifetimePlanCard(
    plan: PaywallPlanPresentation,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
)
```

Use a black rounded card, style-specific metallic border, existing shield drawable on the left, central title/real-price/`one-time purchase` block, description below, a radio indicator on the right, and `MOST POPULAR` on Golden. Use `Modifier.selectable(..., role = Role.RadioButton)` and merge semantics with selected state.

Map assets exactly:

```kotlin
PaywallPlanStyle.STARTER -> R.drawable.starter_emblem_screen_subscripcion
PaywallPlanStyle.GOLDEN -> R.drawable.emblem_golden_screen_subscription
PaywallPlanStyle.ULTIMATE -> R.drawable.emblem_ultimate_red_screen_subscription
```

- [ ] **Step 4: Implement loading, purchasing, and unavailable states truthfully**

Loading shows only branded header/background and progress. `ProductsReady` converts the three real products via `toPaywallPlanPresentations`. Purchasing disables selection/action and displays `Opening Google Play…`. Error displays sanitized text and `Retry`, while preserving Restore and limited-feature continuation.

- [ ] **Step 5: Implement protected bottom actions**

Create:

```kotlin
@Composable
private fun PaywallBottomActions(
    continueEnabled: Boolean,
    purchasing: Boolean,
    onContinuePurchase: () -> Unit,
    onRestore: () -> Unit,
    onLimitedFeatures: () -> Unit,
)
```

Render `CONTINUE`, `Restore purchases`, and `Continue with Limited Features` over a black vertical gradient. Ensure the scroll content has bottom padding larger than the action area's measured height so Ultimate is not obscured.

- [ ] **Step 6: Run focused tests**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.royalshield.app.ui.screens.Paywall*Test" --tests "com.royalshield.app.billing.BillingCatalogPolicyTest" --tests "com.royalshield.app.billing.BillingVerificationDecisionTest" --console=plain
```

Expected: pass.

### Task 4: Build and device verification

**Files:**
- No production file changes expected.

**Interfaces:**
- Consumes: completed Tasks 1-3.
- Produces: build evidence and non-charge physical visual evidence.

- [ ] **Step 1: Compile the debug APK**

```powershell
.\gradlew.bat :app:assembleDebug --console=plain
```

Expected: `BUILD SUCCESSFUL` and `app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 2: Install and launch on the authorized Samsung**

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s RFCXC0DGQ4Z install -r app\build\outputs\apk\debug\app-debug.apk
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s RFCXC0DGQ4Z shell am start -W -n com.royalshield.app/.MainActivity
```

Expected: install success and `MainActivity` launch success. Do not clear app data.

- [ ] **Step 3: Verify visual behavior without charging**

Open the paywall through an existing safe navigation path. Confirm header, three cards, Golden selection, actual Play-returned price or truthful unavailable state, one-time labels, Continue state, Restore, limited-feature action, no overlap, and folded/unfolded usability. Stop before confirming a Google Play purchase.

- [ ] **Step 4: Record remaining external proof**

Report local tests/build/device layout separately from Play Billing E2E. Keep purchase/restore/entitlement as `NO VERIFICADA` until the user explicitly authorizes a licensed Play test and potential charge.
