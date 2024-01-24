/*
 * Copyright (c) 2023 Proton AG
 *
 * This file is part of ProtonVPN.
 *
 * ProtonVPN is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonVPN is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonVPN.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.protonvpn.android.redesign.home_screen.ui

import android.content.Context
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.protonvpn.android.R
import com.protonvpn.android.netshield.NetShieldActions
import com.protonvpn.android.redesign.base.ui.LocalVpnUiDelegate
import com.protonvpn.android.redesign.base.ui.ProtonAlert
import com.protonvpn.android.redesign.base.ui.collectAsEffect
import com.protonvpn.android.redesign.base.ui.getPaddingForWindowWidthClass
import com.protonvpn.android.redesign.home_screen.ui.HomeViewModel.DialogState
import com.protonvpn.android.redesign.main_screen.ui.MainScreenViewModel
import com.protonvpn.android.redesign.recents.ui.RecentItemViewState
import com.protonvpn.android.redesign.recents.ui.RecentsList
import com.protonvpn.android.redesign.recents.ui.rememberRecentsExpandState
import com.protonvpn.android.redesign.vpn.ui.VpnStatusBottom
import com.protonvpn.android.redesign.vpn.ui.VpnStatusTop
import com.protonvpn.android.redesign.vpn.ui.VpnStatusViewState
import com.protonvpn.android.redesign.vpn.ui.rememberVpnStateAnimationProgress
import com.protonvpn.android.redesign.vpn.ui.vpnStatusOverlayBackground
import com.protonvpn.android.ui.home.vpn.ChangeServerButton
import com.protonvpn.android.ui.planupgrade.UpgradeDialogActivity
import com.protonvpn.android.ui.planupgrade.UpgradeNetShieldHighlightsFragment
import com.protonvpn.android.ui.planupgrade.UpgradePlusCountriesHighlightsFragment
import com.protonvpn.android.utils.Constants
import com.protonvpn.android.utils.openUrl
import com.protonvpn.android.vpn.VpnErrorUIManager
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import me.proton.core.compose.theme.ProtonTheme
import kotlin.math.roundToInt

@Composable
fun HomeRoute(
    mainScreenViewModel: MainScreenViewModel,
    onConnectionCardClick: () -> Unit
) {
    HomeView(
        mainScreenViewModel.eventCollapseRecents,
        { mainScreenViewModel.consumeEventCollapseRecents() },
        onConnectionCardClick
    )
}

private val ListBgGradientHeightBasic = 100.dp
private val ListBgGradientHeightExpanded = 200.dp

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun HomeView(
    eventCollapseRecents: SharedFlow<Unit>,
    onEventCollapseRecentsConsumed: () -> Unit,
    onConnectionCardClick: () -> Unit
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val recentsViewState = viewModel.recentsViewState.collectAsStateWithLifecycle(null).value
    val vpnState = viewModel.vpnStateViewFlow.collectAsStateWithLifecycle(VpnStatusViewState.Loading).value
    val mapState = viewModel.mapHighlightState.collectAsStateWithLifecycle(initialValue = null).value
    val dialogState = viewModel.dialogStateFlow.collectAsStateWithLifecycle().value
    val changeServerState = viewModel.changeServerViewState.collectAsStateWithLifecycle(null).value
    val vpnStateTransitionProgress = rememberVpnStateAnimationProgress(vpnState)
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current
    LaunchedEffect(key1 = Unit) {
        viewModel.eventNavigateToUpgrade.collect {
            UpgradeDialogActivity.launch<UpgradePlusCountriesHighlightsFragment>(context)
        }
    }
    // Not using material3 snackbar because of inability to show multiline correctly
    val snackbarHostState = remember { androidx.compose.material.SnackbarHostState() }
    val snackError = viewModel.snackbarErrorFlow.collectAsStateWithLifecycle().value
    if (snackError != null) {
        LaunchedEffect(snackError) {
            handleSnackbarError(context, snackbarHostState, snackError)
            viewModel.consumeErrorMessage()
        }
    }

    val changeServerButton: (@Composable ColumnScope.() -> Unit)? = changeServerState?.let { state ->
        @Composable {
            val uiDelegate = LocalVpnUiDelegate.current
            ChangeServerButton(
                state,
                onChangeServerClick = { viewModel.changeServer(uiDelegate) },
                onUpgradeButtonShown = viewModel::onChangeServerUpgradeButtonShown,
            )
        }
    }

    ConstraintLayout(
        modifier = Modifier.fillMaxSize()
    ) {
        val (vpnStatusTop, vpnStatusBottom, map) = createRefs()
        if (mapState != null) {
            HomeMap(
                modifier = Modifier.constrainAs(map) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                },
                coroutineScope,
                mapState,
                viewModel.elapsedRealtimeClock,
            )
        }
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .vpnStatusOverlayBackground(vpnState)
        )
        val netShieldActions = remember {
            NetShieldActions(
                onChangeServerPromoUpgrade = { UpgradeDialogActivity.launch<UpgradePlusCountriesHighlightsFragment>(context) },
                onNetShieldValueChanged = { viewModel.setNetShieldProtocol(it) },
                onUpgradeNetShield = { UpgradeDialogActivity.launch<UpgradeNetShieldHighlightsFragment>(context) },
                onNetShieldLearnMore = { context.openUrl(Constants.URL_NETSHIELD_LEARN_MORE) },
            )
        }

        VpnStatusBottom(
            state = vpnState,
            transitionValue = { vpnStateTransitionProgress.value },
            netShieldActions = netShieldActions,
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .constrainAs(vpnStatusBottom) {
                    top.linkTo(vpnStatusTop.bottom)
                    centerHorizontallyTo(parent)
                }
        )

        val vpnUiDelegate = LocalVpnUiDelegate.current
        val connectAction = remember<() -> Unit>(vpnUiDelegate) {
            { coroutineScope.launch { viewModel.connect(vpnUiDelegate) } }
        }
        val recentClickedAction = remember<(RecentItemViewState) -> Unit>(vpnUiDelegate) {
            { item -> coroutineScope.launch { viewModel.onRecentClicked(item, vpnUiDelegate) } }
        }
        val listBgColor = ProtonTheme.colors.backgroundNorm
        val listBgGradientColors = listOf(Color.Transparent, listBgColor)
        val recentsExpandState = rememberRecentsExpandState()
        eventCollapseRecents.collectAsEffect {
            onEventCollapseRecentsConsumed()
            recentsExpandState.collapse()
        }
        BoxWithConstraints {
            val viewportSize = DpSize(maxWidth, maxHeight)
            val widthSizeClass = remember(viewportSize) {
                // Normally the window size class should be computed from the screen size (e.g. in activity).
                // The Home view can however be displayed side-by-side with the connection panel, so compute its
                // size class here to take that into account.
                WindowSizeClass.calculateFromSize(viewportSize).widthSizeClass
            }
            if (recentsViewState != null) {
                val maxHeightPx = LocalDensity.current.run { maxHeight.toPx() }
                recentsExpandState.setMaxHeight(maxHeightPx.roundToInt())
                val horizontalPadding = ProtonTheme.getPaddingForWindowWidthClass(widthSizeClass)

                val listBgGradientHeight =
                    if (widthSizeClass == WindowWidthSizeClass.Compact) ListBgGradientHeightBasic else ListBgGradientHeightExpanded
                val listBgGradientOffset =
                    if (widthSizeClass == WindowWidthSizeClass.Compact) 0.dp else ListBgGradientHeightExpanded / 2
                val listContentPadding =
                    PaddingValues(top = listBgGradientOffset, start = horizontalPadding, end = horizontalPadding)
                RecentsList(
                    viewState = recentsViewState,
                    expandState = recentsExpandState,
                    changeServerButton = changeServerButton,
                    onConnectClicked = connectAction,
                    onDisconnectClicked = viewModel::disconnect,
                    onOpenConnectionPanelClicked = onConnectionCardClick,
                    onRecentClicked = recentClickedAction,
                    onRecentPinToggle = viewModel::togglePinned,
                    onRecentRemove = viewModel::removeRecent,
                    contentPadding = listContentPadding,
                    errorSnackBar = snackbarHostState,
                    modifier = Modifier
                        .offset { IntOffset(0, recentsExpandState.listOffsetPx) }
                        .drawBehind {
                            val gradientBottom = listBgGradientHeight.toPx()
                            drawRect(
                                brush = Brush.linearGradient(
                                    listBgGradientColors,
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, gradientBottom)
                                )
                            )
                            drawRect(listBgColor, topLeft = Offset(0f, gradientBottom))
                        }
                )
            }
        }

        val vpnStatusTopMinHeight = 48.dp
        val fullCoverThresholdPx = LocalDensity.current.run {
            (ListBgGradientHeightBasic - vpnStatusTopMinHeight).toPx()
        }
        val coverAlpha = remember(fullCoverThresholdPx) {
            derivedStateOf { calculateOverlayAlpha(recentsExpandState.listOffsetPx, fullCoverThresholdPx) }
        }
        VpnStatusTop(
            vpnState,
            transitionValue = { vpnStateTransitionProgress.value },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = vpnStatusTopMinHeight)
                .recentsScrollOverlayBackground(coverAlpha)
                .windowInsetsPadding(WindowInsets.statusBars)
                .constrainAs(vpnStatusTop) {}
        )
    }

    HomeDialog(dialogState, onDismiss = viewModel::dismissDialog)
}

private suspend fun handleSnackbarError(
    context: Context,
    snackbarHostState: androidx.compose.material.SnackbarHostState,
    snackError: VpnErrorUIManager.SnackError,
) {
    val snackbarResult = snackbarHostState.showSnackbar(
        message = context.getString(snackError.errorRes, snackError.additionalDetails),
        actionLabel = snackError.helpUrl?.actionTitleRes?.let { actionRes ->
            context.getString(
                actionRes
            )
        },
        duration = if (snackError.helpUrl == null) androidx.compose.material.SnackbarDuration.Long else androidx.compose.material.SnackbarDuration.Short
    )

    if (snackError.helpUrl != null && snackbarResult == androidx.compose.material.SnackbarResult.ActionPerformed) {
        context.openUrl(snackError.helpUrl.actionUrl)
    }
}

@Composable
private fun HomeDialog(dialog: DialogState?, onDismiss: () -> Unit) {
    if (dialog != null) {
        val textId = when (dialog) {
            DialogState.CountryInMaintenance -> R.string.message_country_servers_in_maintenance
            DialogState.CityInMaintenance -> R.string.message_city_servers_in_maintenance
            DialogState.ServerInMaintenance -> R.string.message_server_in_maintenance
            DialogState.GatewayInMaintenance -> R.string.message_gateway_in_maintenance
            DialogState.ServerNotAvailable -> R.string.message_server_not_available
        }
        ProtonAlert(
            title = null,
            text = stringResource(textId),
            confirmLabel = stringResource(id = R.string.ok),
            onConfirm = { onDismiss() },
            onDismissRequest = onDismiss
        )
    }
}


private fun Modifier.recentsScrollOverlayBackground(
    alpha: State<Float>
): Modifier = composed {
    val separatorHeight = 1.dp
    val backgroundColor = ProtonTheme.colors.backgroundNorm
    val separatorColor = ProtonTheme.colors.separatorNorm
    drawBehind {
        drawRect(color = backgroundColor, alpha = alpha.value)
        drawRect(
            color = separatorColor,
            topLeft = Offset(0f, size.height - separatorHeight.toPx()),
            size = Size(size.width, separatorHeight.toPx()),
            alpha = alpha.value
        )
    }
}

private fun calculateOverlayAlpha(offset: Int, fullCoverPx: Float): Float {
    return when {
        offset >= 0 && offset < fullCoverPx -> 1f - offset.toFloat() / fullCoverPx
        else -> 0f
    }
}
