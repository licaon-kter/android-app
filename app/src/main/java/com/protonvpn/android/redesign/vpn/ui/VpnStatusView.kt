/*
 * Copyright (c) 2023. Proton AG
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

package com.protonvpn.android.redesign.vpn.ui

import android.text.BidiFormatter
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.protonvpn.android.R
import com.protonvpn.android.netshield.NetShieldActions
import com.protonvpn.android.netshield.NetShieldBottomComposable
import com.protonvpn.android.netshield.NetShieldProtocol
import com.protonvpn.android.netshield.NetShieldStats
import com.protonvpn.android.netshield.NetShieldView
import com.protonvpn.android.netshield.NetShieldViewState
import com.protonvpn.android.netshield.UpgradeNetShieldBusiness
import com.protonvpn.android.netshield.UpgradeNetShieldFree
import com.protonvpn.android.netshield.UpgradePromo
import com.protonvpn.android.redesign.base.ui.vpnGreen
import kotlinx.coroutines.delay
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultStrongNorm
import me.proton.core.compose.theme.defaultWeak
import me.proton.core.presentation.R as CoreR

sealed class VpnStatusViewState {
    data class Connected(
        val isSecureCoreServer: Boolean,
        val banner: StatusBanner,
    ) : VpnStatusViewState()

    data class WaitingForNetwork(
        val locationText: LocationText? = null
    ) : VpnStatusViewState()

    data class Connecting(
        val locationText: LocationText? = null
    ) : VpnStatusViewState()

    data class Disabled(
        val locationText: LocationText? = null
    ) : VpnStatusViewState()

    object Loading : VpnStatusViewState()
}

sealed class StatusBanner {
    data class NetShieldBanner(
        val netShieldState: NetShieldViewState,
    ) : StatusBanner()

    object UpgradePlus : StatusBanner()
    object UpgradeBusiness : StatusBanner()
    object UnwantedCountry : StatusBanner()
}

data class LocationText(
    val country: String,
    val ip: String,
)

fun Modifier.vpnStatusOverlayBackground(
    state: VpnStatusViewState,
): Modifier = composed {
    val targetColor = when (state) {
        is VpnStatusViewState.Connected -> ProtonTheme.colors.vpnGreen
        is VpnStatusViewState.Connecting, is VpnStatusViewState.WaitingForNetwork -> ProtonTheme.colors.shade100
        is VpnStatusViewState.Disabled -> ProtonTheme.colors.notificationError
        is VpnStatusViewState.Loading -> Color.Transparent
    }
    val gradientColor = animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 500), label = "Gradient Animation"
    )

    background(
        Brush.verticalGradient(
            colors = listOf(
                gradientColor.value.copy(alpha = 0.5F),
                gradientColor.value.copy(alpha = 0.0F)
            )
        )
    )
}

@Composable
fun rememberVpnStateAnimationProgress(
    state: VpnStatusViewState,
): State<Float> {
    val transition = updateTransition(targetState = state, label = "connecting -> connected")
    return transition.animateFloat(
        transitionSpec = {
            if (initialState == VpnStatusViewState.Loading ||
                initialState is VpnStatusViewState.Connecting && targetState is VpnStatusViewState.Connected
            ) {
                tween(durationMillis = 500)
            } else {
                snap()
            }
        },
        label = "progress"
    ) { targetState ->
        when (targetState) {
            is VpnStatusViewState.Connected -> 1f
            else -> 0f
        }
    }
}

@Composable
fun VpnStatusTop(
    state: VpnStatusViewState,
    transitionValue: () -> Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
    ) {
        val contentModifier = Modifier
            .align(Alignment.Center)
            .padding(8.dp)
        when (state) {
            is VpnStatusViewState.Connected ->
                VpnConnectedViewTop(state.isSecureCoreServer, transitionValue, contentModifier)

            is VpnStatusViewState.Connecting, is VpnStatusViewState.WaitingForNetwork ->
                CircularProgressIndicator(
                    color = ProtonTheme.colors.iconNorm,
                    strokeWidth = 2.dp,
                    modifier = contentModifier.size(24.dp)
                )

            is VpnStatusViewState.Disabled -> {
                Icon(
                    painter = painterResource(id = CoreR.drawable.ic_proton_lock_open_filled_2),
                    contentDescription = null,
                    tint = ProtonTheme.colors.notificationError,
                    modifier = contentModifier
                )
            }
            is VpnStatusViewState.Loading -> {}
        }
    }
}

@Composable
fun VpnStatusBottom(
    state: VpnStatusViewState,
    transitionValue: () -> Float,
    netShieldActions: NetShieldActions,
    modifier: Modifier = Modifier
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            when (state) {
                is VpnStatusViewState.Connected -> {
                    VpnConnectedView(state, netShieldActions, transitionValue)
                }

                is VpnStatusViewState.Connecting -> {
                    VpnConnectingView(state)
                }

                is VpnStatusViewState.Disabled -> {
                    VpnDisabledView(state)
                }

                is VpnStatusViewState.WaitingForNetwork -> {
                    VpnWaitingForNetwork()
                }

                VpnStatusViewState.Loading -> {}
            }
        }
    }
}

@Composable
private fun VpnConnectedViewTop(
    isSecureCoreServer: Boolean,
    transitionValue: () -> Float,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .graphicsLayer { this.alpha = transitionValue() }
    ) {
        Icon(
            painter = painterResource(
                id = when {
                    isSecureCoreServer -> CoreR.drawable.ic_proton_locks_filled
                    else -> CoreR.drawable.ic_proton_lock_filled
                }
            ),
            tint = ProtonTheme.colors.vpnGreen,
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = stringResource(R.string.vpn_status_connected),
            style = ProtonTheme.typography.defaultStrongNorm,
            color = ProtonTheme.colors.vpnGreen,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VpnConnectedView(
    connectedState: VpnStatusViewState.Connected,
    netShieldActions: NetShieldActions,
    transitionValue: () -> Float
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isModalVisible by remember { mutableStateOf(false) }
    Surface(
        color = ProtonTheme.colors.backgroundNorm.copy(alpha = 0.7F),
        shape = ProtonTheme.shapes.large,
        modifier = Modifier
            .offset { IntOffset(x = 0, y = (transitionValue() * 16.dp.toPx()).toInt()) }
            .graphicsLayer {
                this.alpha = transitionValue()
            }
            .padding(top = 8.dp)
    ) {
        when (connectedState.banner) {
            is StatusBanner.NetShieldBanner -> {
                NetShieldView(
                    state = connectedState.banner.netShieldState,
                    onNavigateToSubsetting = { isModalVisible = !isModalVisible }
                )
            }

            StatusBanner.UnwantedCountry -> {
                UpgradePromo(
                    titleRes = R.string.not_wanted_country_title,
                    descriptionRes = R.string.not_wanted_country_description,
                    iconRes = R.drawable.upsell_worldwide_cover_exclamation,
                    navigateToUpgrade = netShieldActions.onChangeServerPromoUpgrade
                )
            }

            StatusBanner.UpgradeBusiness -> UpgradeNetShieldBusiness()
            StatusBanner.UpgradePlus -> UpgradeNetShieldFree(netShieldActions.onUpgradeNetShield)
        }
    }
    if (isModalVisible && connectedState.banner is StatusBanner.NetShieldBanner) {
        ModalBottomSheet(
            sheetState = bottomSheetState,
            content = {
                NetShieldBottomComposable(
                    currentNetShield = connectedState.banner.netShieldState.protocol,
                    onNetShieldLearnMore = netShieldActions.onNetShieldLearnMore,
                    onValueChanged = netShieldActions.onNetShieldValueChanged
                )
            },
            windowInsets = WindowInsets.navigationBars,
            onDismissRequest = {
                isModalVisible = !isModalVisible
            })
    }
}

@Composable
private fun VpnConnectingView(state: VpnStatusViewState.Connecting) {
    Text(
        text = stringResource(R.string.vpn_status_connecting),
        style = ProtonTheme.typography.defaultStrongNorm,
        modifier = Modifier.padding(8.dp)
    )

    state.locationText?.let {
        LocationTextAnimated(locationText = it)
    }
}

@Composable
private fun VpnWaitingForNetwork() {
    Text(
        text = stringResource(R.string.error_vpn_waiting_for_network),
        style = ProtonTheme.typography.defaultStrongNorm,
        modifier = Modifier.padding(8.dp)
    )
}

@Composable
private fun LocationTextAnimated(locationText: LocationText) {
    Surface(
        color = ProtonTheme.colors.backgroundNorm.copy(alpha = 0.4f),
        shape = ProtonTheme.shapes.medium,
    ) {
        AnimateText(
            targetText = stringResource(
                R.string.vpn_status_disabled_location,
                BidiFormatter.getInstance().unicodeWrap(locationText.country),
                locationText.ip
            ),
            targetCharacter = '*',
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun VpnDisabledView(state: VpnStatusViewState.Disabled) {
    Text(
        text = stringResource(R.string.vpn_status_disabled),
        style = ProtonTheme.typography.defaultStrongNorm,
        modifier = Modifier.padding(8.dp)
    )
    Surface(
        color = ProtonTheme.colors.backgroundNorm.copy(alpha = 0.4F),
        shape = ProtonTheme.shapes.medium,
    ) {
        Text(
            text = state.locationText?.let {
                stringResource(
                    R.string.vpn_status_disabled_location,
                    BidiFormatter.getInstance().unicodeWrap(it.country),
                    it.ip
                )
            } ?: stringResource(R.string.stateFragmentUnknownIp),
            style = ProtonTheme.typography.defaultWeak,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}


@Composable
private fun AnimateText(
    modifier: Modifier = Modifier,
    targetText: String,
    duration: Int = 50,
    targetCharacter: Char = '*',
    preserveCharacters: CharArray = charArrayOf('.', ' ', '-')
) {
    val displayText = remember { mutableStateOf(targetText) }
    val fixedWidth = remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(targetText) {
        fixedWidth.value?.let {
            val indicesToReplace = targetText.indices
                .filter { !preserveCharacters.contains(targetText[it]) }
                .shuffled()

            indicesToReplace.forEachIndexed { _, replacementIndex ->
                delay(duration.toLong())
                displayText.value = displayText.value.replaceRange(
                    replacementIndex,
                    replacementIndex + 1,
                    targetCharacter.toString()
                )
            }
        }
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Layout(
            content = {
                Text(
                    displayText.value,
                    style = ProtonTheme.typography.defaultWeak,
                    modifier = Modifier.onGloballyPositioned {
                        if (fixedWidth.value == null) {
                            fixedWidth.value = it.size.width
                        }
                    },
                )
            },
            modifier = modifier,
            measurePolicy = { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                val width = fixedWidth.value ?: placeable.width
                val offsetX = (width - placeable.width) / 2
                layout(width, placeable.height) {
                    placeable.placeRelative(offsetX, 0)
                }
            }
        )
    }
}

@Preview
@Composable
private fun PreviewVpnDisabledState() {
    PreviewHelper(
        state = VpnStatusViewState.Disabled(
            LocationText(
                "Europe",
                "192.1.1.1.1"
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
}

@Preview
@Composable
private fun PreviewVpnConnectingState() {
    PreviewHelper(
        state = VpnStatusViewState.Connecting(
            LocationText(
                "Europe",
                "192.1.1.1.1"
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
}

@Preview
@Composable
private fun PreviewVpnDisabledStateWithRTLSymbols() {
    PreviewHelper(
        state = VpnStatusViewState.Disabled(
            LocationText(
                "اغلب برامجا",
                "192.1.1.1.1"
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
}

@Preview
@Composable
private fun PreviewVpnConnectedState() {
    PreviewHelper(
        state = VpnStatusViewState.Connected(
            true, banner = StatusBanner.NetShieldBanner(
                NetShieldViewState(
                    protocol = NetShieldProtocol.ENABLED_EXTENDED,
                    netShieldStats = NetShieldStats(
                        adsBlocked = 3,
                        trackersBlocked = 0,
                        savedBytes = 2000
                    )
                )
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
}

@Preview
@Composable
private fun PreviewVpnConnectedSecureCoreState() {
    PreviewHelper(
        state = VpnStatusViewState.Connected(
            true, banner = StatusBanner.NetShieldBanner(
                NetShieldViewState(
                    protocol = NetShieldProtocol.ENABLED_EXTENDED,
                    netShieldStats = NetShieldStats(
                        adsBlocked = 3,
                        trackersBlocked = 0,
                        savedBytes = 2000
                    )
                )
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
}

@Composable
private fun PreviewHelper(state: VpnStatusViewState, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        VpnStatusTop(state = state, transitionValue = { 1f })
        VpnStatusBottom(state = state, transitionValue = { 1f }, NetShieldActions({}, {}, {}, {}))
    }
}
