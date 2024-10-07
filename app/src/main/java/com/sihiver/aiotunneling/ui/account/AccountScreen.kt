package com.sihiver.aiotunneling.ui.account

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import java.util.UUID
import com.sihiver.aiotunneling.ui.account.Profile

@Composable
fun AccountScreen() {
    var showDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var profileToEdit by remember { mutableStateOf<Profile?>(null) }
    var profileName by remember { mutableStateOf("") }
    var server by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    val profiles = remember { mutableStateListOf<Profile>() }

    LaunchedEffect(Unit) {
        profiles.addAll(loadProfilesFromSharedPreferences(context))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(profiles) { profile ->
                ProfileItem(
                    profile = profile,
                    onDelete = {
                        profiles.remove(profile)
                        deleteProfileFromSharedPreferences(context, profile)
                    },
                    onEdit = {
                        profileToEdit = profile
                        profileName = profile.profileName
                        server = profile.server
                        port = profile.port.toString()  // Convert to String for editing
                        username = profile.username
                        password = profile.password
                        isEditing = true
                        showDialog = true
                    }
                )
            }
        }
        FloatingActionButton(
            onClick = {
                profileToEdit = null
                profileName = ""
                server = ""
                port = ""
                username = ""
                password = ""
                isEditing = false
                showDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .shadow(8.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (isEditing) "Edit Server Profile" else "Add Server Profile") },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = profileName,
                            onValueChange = { profileName = it },
                            label = { Text("Profile") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = server,
                            onValueChange = { server = it },
                            label = { Text("Server") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = port,
                            onValueChange = { port = it },
                            label = { Text("Port") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (isEditing && profileToEdit != null) {
                        val updatedProfile = profileToEdit!!.copy(
                            profileName = profileName,
                            server = server,
                            port = port.toInt(),  // Convert to Int for saving
                            username = username,
                            password = password
                        )
                        profiles[profiles.indexOf(profileToEdit!!)] = updatedProfile
                        saveToSharedPreferences(context, updatedProfile)
                    } else {
                        val newProfile = Profile(id = UUID.randomUUID().toString(), profileName, server, port.toInt(), username, password)
                        profiles.add(newProfile)
                        saveToSharedPreferences(context, newProfile)
                    }
                    showDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ProfileItem(profile: Profile, onDelete: () -> Unit, onEdit: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium)
            .padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = profile.profileName.firstOrNull()?.toString() ?: "",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Profile: ${profile.profileName}")
                Text(text = "Username: ${profile.username}")
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

fun saveToSharedPreferences(context: Context, profile: Profile) {
    val sharedPreferences = context.getSharedPreferences("ServerProfile", Context.MODE_PRIVATE)
    with(sharedPreferences.edit()) {
        putString("id_${profile.profileName}", profile.id)
        putString("profile_${profile.profileName}", profile.profileName)
        putString("server_${profile.profileName}", profile.server)
        putInt("port_${profile.profileName}", profile.port)  // Save as Int
        putString("username_${profile.profileName}", profile.username)
        putString("password_${profile.profileName}", profile.password)
        apply()
    }
}

fun loadProfilesFromSharedPreferences(context: Context): List<Profile> {
    val sharedPreferences = context.getSharedPreferences("ServerProfile", Context.MODE_PRIVATE)
    val profiles = mutableListOf<Profile>()

    sharedPreferences.all.forEach { (key, value) ->
        if (key.startsWith("profile_")) {
            val profileName = value as String
            val id = sharedPreferences.getString("id_$profileName", "") ?: ""
            val server = sharedPreferences.getString("server_$profileName", "") ?: ""
            val port = try {
                sharedPreferences.getInt("port_$profileName", 0)
            } catch (e: ClassCastException) {
                sharedPreferences.getString("port_$profileName", "0")?.toInt() ?: 0
            }
            val username = sharedPreferences.getString("username_$profileName", "") ?: ""
            val password = sharedPreferences.getString("password_$profileName", "") ?: ""
            profiles.add(Profile(id, profileName, server, port, username, password))
        }
    }

    return profiles
}

fun deleteProfileFromSharedPreferences(context: Context, profile: Profile) {
    val sharedPreferences = context.getSharedPreferences("ServerProfile", Context.MODE_PRIVATE)
    with(sharedPreferences.edit()) {
        remove("id_${profile.profileName}")
        remove("profile_${profile.profileName}")
        remove("server_${profile.profileName}")
        remove("port_${profile.profileName}")
        remove("username_${profile.profileName}")
        remove("password_${profile.profileName}")
        apply()
    }
}