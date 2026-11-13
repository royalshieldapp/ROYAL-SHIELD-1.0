package com.royalshield.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.royalshield.app.R
import com.royalshield.app.managers.BillingManager
import com.royalshield.app.ui.theme.RoyalGold
import com.royalshield.app.ui.viewmodels.PaywallUiState
import com.royalshield.app.ui.viewmodels.PaywallViewModel

private val PlansBlack = Color(0xFF030303)
private val PlansPanel = Color(0xFF0B0B0C)
private val PlansSilver = Color(0xFFE4E5E7)
private val PlansRed = Color(0xFFDD171E)

@Composable
fun PremiumEliteScreen(
    onContinue: () -> Unit,
    onPurchaseSuccess: () -> Unit,
    billingManager: BillingManager,
) {
    val context = LocalContext.current
    val model: PaywallViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = PaywallViewModel(billingManager) as T
    })
    val state by model.uiState.collectAsState()
    val selectedId by model.selectedProductId.collectAsState()
    var plans by remember { mutableStateOf(emptyList<PaywallPlanPresentation>()) }
    val ready = state as? PaywallUiState.ProductsReady
    if (ready != null) plans = toPaywallPlanPresentations(listOfNotNull(ready.starter, ready.gold, ready.ultimate))
    LaunchedEffect(state) { if (state is PaywallUiState.Success) onPurchaseSuccess() }
    val purchasing = state is PaywallUiState.Purchasing

    Box(Modifier.fillMaxSize().background(PlansBlack)) {
        Image(painterResource(R.drawable.bg_subscription_gold), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(.58f), Color.Black.copy(.86f), Color.Black))))
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 34.dp, bottom = 174.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("CHOOSE YOUR PROTECTION", color = Color(0xFFFFD86A), fontSize = 27.sp, lineHeight = 32.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text("Select the plan that fits your security needs.", color = Color.White.copy(.68f), fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(26.dp))
            when (val current = state) {
                PaywallUiState.Loading -> PlansProgress("Loading Google Play plans…")
                PaywallUiState.Purchasing -> {
                    if (plans.isNotEmpty()) PlansList(plans, selectedId, false) {}
                    Spacer(Modifier.height(12.dp)); PlansProgress("Opening Google Play…")
                }
                is PaywallUiState.ProductsReady -> PlansList(plans, selectedId, true, model::selectProduct)
                is PaywallUiState.Error -> PlansError(current.message, model::retryLoading)
                is PaywallUiState.Success -> PlansProgress("Verifying your protection…")
            }
        }
        PlansActions(
            enabled = isPaywallContinueEnabled(selectedId, purchasing, plans),
            purchasing = purchasing,
            onPurchase = { context.findPlansActivity()?.let(model::purchaseSelected) },
            onRestore = model::restorePurchases,
            onLimited = onContinue,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable private fun PlansList(plans: List<PaywallPlanPresentation>, selectedId: String?, enabled: Boolean, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().selectableGroup(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        plans.forEach { PlansCard(it, selectedId == it.productId, enabled) { onSelect(it.productId) } }
    }
}

@Composable private fun PlansCard(plan: PaywallPlanPresentation, selected: Boolean, enabled: Boolean, onSelect: () -> Unit) {
    val accent = when (plan.style) { PaywallPlanStyle.STARTER -> PlansSilver; PaywallPlanStyle.GOLDEN -> RoyalGold; PaywallPlanStyle.ULTIMATE -> PlansRed }
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().height(176.dp).shadow(if (selected) 14.dp else 3.dp, shape, ambientColor = accent, spotColor = accent)
            .clip(shape).background(Brush.horizontalGradient(listOf(PlansPanel, accent.copy(if (selected) .18f else .06f), PlansPanel)))
            .border(if (selected) 2.dp else 1.dp, accent.copy(if (selected) .95f else .55f), shape)
            .selectable(selected, enabled, Role.RadioButton, onClick = onSelect)
            .semantics(mergeDescendants = true) { contentDescription = "${plan.title}, ${plan.formattedPrice}, ${plan.purchaseLabel}, ${plan.description}" }
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val shield = when (plan.style) {
            PaywallPlanStyle.STARTER -> R.drawable.starter_emblem_screen_subscripcion
            PaywallPlanStyle.GOLDEN -> R.drawable.emblem_golden_screen_subscription
            PaywallPlanStyle.ULTIMATE -> R.drawable.emblem_ultimate_red_screen_subscription
        }
        Image(painterResource(shield), null, Modifier.size(108.dp), contentScale = ContentScale.Fit)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            if (plan.mostPopular) {
                Text("★ MOST POPULAR", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(RoyalGold).padding(horizontal = 8.dp, vertical = 4.dp))
                Spacer(Modifier.height(6.dp))
            }
            Text(plan.title, color = if (plan.style == PaywallPlanStyle.ULTIMATE) Color.White else accent, fontSize = 22.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(plan.formattedPrice, color = if (plan.style == PaywallPlanStyle.ULTIMATE) RoyalGold else Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(plan.purchaseLabel, color = Color.White.copy(.58f), fontSize = 10.sp)
            Spacer(Modifier.height(6.dp)); Text(plan.description, color = Color.White.copy(.72f), fontSize = 12.sp)
        }
        Spacer(Modifier.width(6.dp))
        Box(Modifier.size(30.dp).border(2.dp, if (selected) accent else Color.White.copy(.8f), CircleShape).padding(5.dp)) {
            if (selected) Box(Modifier.fillMaxSize().background(accent, CircleShape))
        }
    }
}

@Composable private fun PlansProgress(message: String) = Column(horizontalAlignment = Alignment.CenterHorizontally) {
    CircularProgressIndicator(color = RoyalGold); Spacer(Modifier.height(12.dp)); Text(message, color = Color.White.copy(.78f), fontSize = 13.sp)
}

@Composable private fun PlansError(message: String, retry: () -> Unit) = Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(message, color = Color.White.copy(.84f), textAlign = TextAlign.Center)
    Spacer(Modifier.height(14.dp)); Button(retry, colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = Color.Black)) { Text("RETRY", fontWeight = FontWeight.Black) }
}

@Composable private fun PlansActions(enabled: Boolean, purchasing: Boolean, onPurchase: () -> Unit, onRestore: () -> Unit, onLimited: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(.94f), Color.Black))).padding(horizontal = 24.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onPurchase, enabled = enabled, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = Color.Black, disabledContainerColor = RoyalGold.copy(.34f))) {
            Text(if (purchasing) "OPENING GOOGLE PLAY…" else "CONTINUE", fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
        Text("Restore purchases", color = RoyalGold.copy(.88f), fontSize = 13.sp, modifier = Modifier.clickable(!purchasing, onClick = onRestore).padding(7.dp))
        Text("Continue with Limited Features", color = Color.White.copy(.52f), fontSize = 11.sp, modifier = Modifier.clickable(!purchasing, onClick = onLimited).padding(5.dp))
    }
}

private tailrec fun Context.findPlansActivity(): Activity? = when (this) { is Activity -> this; is ContextWrapper -> baseContext.findPlansActivity(); else -> null }
