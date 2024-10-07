package com.sihiver.aiotunneling.util

import android.content.Context
import com.sihiver.aiotunneling.ui.home.SSHState

private const val PREFS_NAME = "ssh_prefs"
private const val KEY_SELECTED_PROFILE_ID = "selected_profile_id"
private const val KEY_USE_PYLOAD_SSL = "use_pyload_ssl"
private const val KEY_SLOW_DNS = "slow_dns"
private const val KEY_V2RAY = "v2ray"
private const val KEY_CONNECTION_STATE = "connection_state"

fun saveSSHState(context: Context, state: SSHState) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    with(prefs.edit()) {
        putString(KEY_SELECTED_PROFILE_ID, state.selectedProfileId)
        putBoolean(KEY_USE_PYLOAD_SSL, state.usePyloadSSL)
        putBoolean(KEY_SLOW_DNS, state.slowDNS)
        putBoolean(KEY_V2RAY, state.v2ray)
        putString(KEY_CONNECTION_STATE, state.connectionState)
        apply()
    }
}

fun loadSSHState(context: Context): SSHState {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return SSHState(
        selectedProfileId = prefs.getString(KEY_SELECTED_PROFILE_ID, "") ?: "",
        usePyloadSSL = prefs.getBoolean(KEY_USE_PYLOAD_SSL, false),
        slowDNS = prefs.getBoolean(KEY_SLOW_DNS, false),
        v2ray = prefs.getBoolean(KEY_V2RAY, false),
        connectionState = prefs.getString(KEY_CONNECTION_STATE, "Disconnected") ?: "Disconnected"
    )
}