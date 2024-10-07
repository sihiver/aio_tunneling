package com.sihiver.aiotunneling.ui.home

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.sihiver.aiotunneling.ui.account.Profile
import com.sihiver.aiotunneling.ui.account.loadProfilesFromSharedPreferences
import com.sihiver.aiotunneling.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class)
@Composable
fun HomeScreen() {
    val pagerState = rememberPagerState()
    val coroutineScope = rememberCoroutineScope()
    val tabs = listOf("SSH", "LOG")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(title) }
                )
            }
        }
        HorizontalPager(
            count = tabs.size,
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) { page ->
            when (page) {
                0 -> SSHContent()
                1 -> LogContent()
            }
        }
    }
}

@SuppressLint("UseOfNonLambdaOffsetOverload")
@Composable
fun SSHContent() {
    val context = LocalContext.current
    var sshState by remember { mutableStateOf(loadSSHState(context)) }
    val profiles = remember { mutableStateListOf<Profile>() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        profiles.addAll(loadProfilesFromSharedPreferences(context))
    }

    val selectedProfile = profiles.find { it.id == sshState.selectedProfileId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.background)
    ) {
        var expanded by remember { mutableStateOf(false) }
        val dropdownOffset by animateFloatAsState(
            targetValue = if (expanded) 4f else 0f,
            animationSpec = tween(durationMillis = 300), label = ""
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .offset(y = dropdownOffset.dp)
        ) {
            Text(
                text = selectedProfile?.profileName ?: "Select Server Profile",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(vertical = 4.dp)
            ) {
                profiles.forEach { profile ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                profile.profileName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = {
                            sshState = sshState.copy(selectedProfileId = profile.id)
                            saveSSHState(context, sshState)
                            expanded = false
                            LogManager.addLog("Profile selected: ${profile.profileName}")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        CheckboxItem(
            text = "Use Pyload SSL",
            checked = sshState.usePyloadSSL,
            onCheckedChange = {
                sshState = sshState.copy(usePyloadSSL = it)
                saveSSHState(context, sshState)
            }
        )

        CheckboxItem(
            text = "SlowDNS",
            checked = sshState.slowDNS,
            onCheckedChange = {
                sshState = sshState.copy(slowDNS = it)
                saveSSHState(context, sshState)
            }
        )

        CheckboxItem(
            text = "V2Ray",
            checked = sshState.v2ray,
            onCheckedChange = {
                sshState = sshState.copy(v2ray = it)
                saveSSHState(context, sshState)
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    if (sshState.connectionState == "Connected") {
                        stopSSHTunnel()
                        sshState = sshState.copy(connectionState = "Disconnected")
                    } else {
                        sshState = sshState.copy(connectionState = "Connecting")
                        val result = startSSHTunnel(selectedProfile, sshState)
                        sshState = sshState.copy(connectionState = if (result) "Connected" else "Failed")
                    }
                    saveSSHState(context, sshState)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(text = if (sshState.connectionState == "Connected") "Disconnect" else "Connect", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun ProfileDropdown(
    selectedProfile: Profile?,
    profiles: List<Profile>,
    onProfileSelected: (Profile) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = selectedProfile?.profileName ?: "Select Server Profile",
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(16.dp)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            profiles.forEach { profile ->
                DropdownMenuItem(
                    text = { Text(profile.profileName) },
                    onClick = {
                        onProfileSelected(profile)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun CheckboxItem(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text)
    }
}

@Composable
fun LogContent() {
    val logs by LogManager.logs.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(logs) { log ->
            Text(text = log)
        }
    }
}

private suspend fun startSSHTunnel(profile: Profile?, sshState: SSHState): Boolean {
    if (profile == null) {
        LogManager.addLog("Tidak ada profil yang dipilih")
        return false
    }
    LogManager.addLog("Memulai SSH Tunnel dengan profil: ${profile.profileName}")
    Connection.startTunnel(profile, sshState)
    return true
}

private suspend fun stopSSHTunnel() {
    LogManager.addLog("Menghentikan SSH Tunnel")
    Connection.stopTunnel()
}

data class SSHState(
    val selectedProfileId: String,
    val usePyloadSSL: Boolean,
    val slowDNS: Boolean,
    val v2ray: Boolean,
    val connectionState: String
)