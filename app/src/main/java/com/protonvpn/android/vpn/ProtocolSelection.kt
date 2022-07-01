/*
 * Copyright (c) 2022 Proton AG
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

package com.protonvpn.android.vpn

import com.protonvpn.android.R
import com.protonvpn.android.models.config.TransmissionProtocol
import com.protonvpn.android.models.config.VpnProtocol
import java.io.Serializable

data class ProtocolSelection private constructor(
    val vpn: VpnProtocol,
    val transmission: TransmissionProtocol?
) : Serializable {

    fun localAgentEnabled() = vpn.localAgentEnabled()

    val displayName: Int get() = when (vpn) {
        VpnProtocol.Smart -> R.string.settingsProtocolNameSmart
        VpnProtocol.WireGuard -> when (transmission) {
            TransmissionProtocol.TCP -> R.string.settingsProtocolNameWireguardTCP
            TransmissionProtocol.TLS -> R.string.settingsProtocolNameWireguardTLS
            else -> R.string.settingsProtocolNameWireguard
        }
        VpnProtocol.IKEv2 -> R.string.settingsProtocolNameIkeV2
        VpnProtocol.OpenVPN -> when (transmission) {
            TransmissionProtocol.TCP -> R.string.settingsProtocolNameOpenVpnTcp
            else -> R.string.settingsProtocolNameOpenVpnUdp
        }
    }

    companion object {
        @JvmStatic
        operator fun invoke(
            vpn: VpnProtocol,
            transmission: TransmissionProtocol? = null
        ) = ProtocolSelection(
            vpn,
            if (vpn == VpnProtocol.Smart)
                null
            else
                transmission ?: TransmissionProtocol.UDP
        )
    }
}
