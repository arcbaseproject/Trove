package com.trove.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.trove.app.R
import com.trove.app.domain.model.CollectionUi
import com.trove.app.domain.model.ScreenshotUi
import com.trove.app.ui.collections.CollectionDetailScreen
import com.trove.app.ui.collections.CollectionsScreen
import com.trove.app.ui.home.HomeScreen
import com.trove.app.ui.library.LibraryScreen
import com.trove.app.ui.search.SearchScreen
import com.trove.app.ui.settings.DuplicatesScreen
import com.trove.app.ui.settings.PrivacyScreen
import com.trove.app.ui.settings.SettingsScreen
import com.trove.app.ui.viewer.ScreenshotViewerScreen

private data class BottomTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val bottomTabs = listOf(
    BottomTab(Destinations.HOME, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    BottomTab(Destinations.LIBRARY, R.string.nav_library, Icons.Filled.PhotoLibrary, Icons.Outlined.PhotoLibrary),
    BottomTab(Destinations.SEARCH, R.string.nav_search, Icons.Filled.Search, Icons.Outlined.Search),
    BottomTab(Destinations.COLLECTIONS, R.string.nav_collections, Icons.Filled.Folder, Icons.Outlined.Folder),
    BottomTab(Destinations.SETTINGS, R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings),
)

val LocalBottomBarPadding = compositionLocalOf { 0.dp }

@Composable
fun ScreenshotBrainNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination
    val showBottomBar = bottomTabs.any { tab -> currentRoute?.hierarchy?.any { it.route == tab.route } == true }

    // Screens draw their own status-bar area (header wash, viewer), so only bottom and sides here.
    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal),
    ) { padding ->
        val barInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalBottomBarPadding provides barInset + if (showBottomBar) 88.dp else 0.dp) {
        NavHost(
            navController = navController,
            startDestination = Destinations.HOME,
            // Consume what this Scaffold already padded, or inner Scaffolds add the insets again.
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .consumeWindowInsets(WindowInsets.navigationBars),
        ) {
            composable(Destinations.HOME) {
                HomeScreen(
                    onOpenSearch = { navController.navigateToTab(Destinations.SEARCH) },
                    onOpenLibrary = { navController.navigateToTab(Destinations.LIBRARY) },
                    onOpenCategory = { key -> navController.navigate(Destinations.libraryFull(key)) },
                    onOpenScreenshot = { screenshot -> navController.openViewer(ScreenshotSource.All, screenshot) },
                )
            }
            composable(Destinations.LIBRARY) {
                LibraryScreen(
                    onBack = { navController.popBackStack() },
                    onOpenScreenshot = { screenshot -> navController.openViewer(ScreenshotSource.All, screenshot) },
                )
            }
            composable(Destinations.SEARCH) {
                SearchScreen(
                    onOpenScreenshot = { query, screenshot ->
                        navController.openViewer(ScreenshotSource.SearchResults(query), screenshot)
                    },
                )
            }
            composable(Destinations.COLLECTIONS) {
                CollectionsScreen(
                    onOpenCollection = { collection: CollectionUi ->
                        navController.navigate(Destinations.collectionDetail(collection.id, collection.name))
                    },
                )
            }
            composable(Destinations.SETTINGS) {
                SettingsScreen(
                    onOpenPrivacy = { navController.navigate(Destinations.PRIVACY) },
                    onOpenDuplicates = { navController.navigate(Destinations.DUPLICATES) },
                )
            }
            composable(
                route = Destinations.LIBRARY_FULL,
                arguments = listOf(navArgument("category") { type = NavType.StringType; defaultValue = "_" }),
            ) { backStackEntry ->
                val category = backStackEntry.arguments?.getString("category")?.takeIf { it != "_" }
                val source = category?.let { ScreenshotSource.Category(it) } ?: ScreenshotSource.All
                LibraryScreen(
                    onBack = { navController.popBackStack() },
                    onOpenScreenshot = { screenshot -> navController.openViewer(source, screenshot) },
                )
            }
            composable(
                route = Destinations.COLLECTION_DETAIL,
                arguments = listOf(
                    navArgument("collectionId") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val collectionId = backStackEntry.arguments?.getString("collectionId")?.toLong() ?: return@composable
                CollectionDetailScreen(
                    onBack = { navController.popBackStack() },
                    onOpenScreenshot = { screenshot ->
                        navController.openViewer(ScreenshotSource.Collection(collectionId), screenshot)
                    },
                )
            }
            composable(
                route = Destinations.VIEWER,
                arguments = listOf(
                    navArgument("source") { type = NavType.StringType },
                    navArgument("screenshotId") { type = NavType.StringType },
                ),
            ) {
                ScreenshotViewerScreen(onBack = { navController.popBackStack() })
            }
            composable(Destinations.PRIVACY) {
                PrivacyScreen(onBack = { navController.popBackStack() })
            }
            composable(Destinations.DUPLICATES) {
                DuplicatesScreen(onBack = { navController.popBackStack() })
            }
        }
        }
            if (showBottomBar) Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
            ) {
                Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    bottomTabs.forEach { tab ->
                        val selected = currentRoute?.hierarchy?.any { it.route == tab.route } == true
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                                .clickable(role = Role.Tab) { navController.navigateToTab(tab.route) }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = null,
                                tint = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                stringResource(tab.labelRes),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.openViewer(source: ScreenshotSource, screenshot: ScreenshotUi) {
    navigate(Destinations.viewer(source, screenshot.id))
}
