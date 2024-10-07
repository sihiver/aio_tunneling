package com.sihiver.aiotunneling

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sihiver.aiotunneling.ui.account.AccountScreen
import com.sihiver.aiotunneling.ui.home.HomeScreen

@Composable
fun NavGraph(navController: NavHostController = rememberNavController(), startDestination: String = "home") {
    NavHost(navController = navController, startDestination = startDestination) {
        composable("home") {
            HomeScreen()
        }
        composable("account") {
            AccountScreen()
        }
    }
}