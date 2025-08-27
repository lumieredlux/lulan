package com.lulan.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.lulan.app.ScreenProjectionPermissionStore
import com.lulan.app.ui.screens.*

sealed class Route(val r: String) {
    data object Splash : Route("splash")
    data object Home : Route("home")
    data object Discover : Route("discover")
    data object Start : Route("start")
    data object Stats : Route("stats")
    data object Settings : Route("settings")
}

@Composable
fun LuLanNavGraph(
    onRequestProjection: () -> Unit,
    navController: NavHostController = androidx.navigation.compose.rememberNavController()
) {
    NavHost(navController, startDestination = Route.Splash.r) {
        composable(Route.Splash.r) { SplashScreen { navController.navigate(Route.Home.r) { popUpTo(Route.Splash.r){inclusive=true} } } }
        composable(Route.Home.r) { HomeScreen(
            onStart = { navController.navigate(Route.Start.r) },
            onDiscover = { navController.navigate(Route.Discover.r) },
            onStats = { navController.navigate(Route.Stats.r) },
            onSettings = { navController.navigate(Route.Settings.r) }
        ) }
        composable(Route.Discover.r) { DiscoverScreen() }
        composable(Route.Start.r) { StartStreamingScreen(onRequestProjection) }
        composable(Route.Stats.r) { StatsScreen() }
        composable(Route.Settings.r) { SettingsScreen() }
    }

    // Hook the activity result bridge to deliver to controller
    ScreenProjectionPermissionStore.register { code, data ->
        com.lulan.app.ui.screens.handleProjectionResult(code, data)
    }
}
