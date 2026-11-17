package com.parkmate.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.parkmate.app.R
import com.parkmate.app.core.ui.theme.ParkMateThemeTokens
import com.parkmate.app.feature.camera.CameraCaptureScreen
import com.parkmate.app.feature.camera.CameraFlowViewModel
import com.parkmate.app.feature.camera.CameraPermissionScreen
import com.parkmate.app.feature.camera.PhotoReviewScreen
import com.parkmate.app.feature.camera.PhotoViewerScreen
import com.parkmate.app.feature.details.AddDetailsScreen
import com.parkmate.app.feature.details.SpotDetailsViewModel
import com.parkmate.app.feature.details.SpotReadScreen
import com.parkmate.app.feature.guide.GuideMeScreen
import com.parkmate.app.feature.guide.GuideMeViewModel
import com.parkmate.app.feature.home.HomeScreen
import com.parkmate.app.feature.home.HomeViewModel
import com.parkmate.app.feature.home.QuickSaveScreen
import com.parkmate.app.feature.history.HistoryScreen
import com.parkmate.app.feature.history.HistoryViewModel
import com.parkmate.app.feature.settings.SettingsScreen
import com.parkmate.app.feature.settings.SettingsViewModel

private data class TopLevelTab(
    val route: ParkMateRoute,
    val icon: ImageVector,
    val labelRes: Int
)

private val TOP_LEVEL_TABS = listOf(
    TopLevelTab(ParkMateRoute.MapHome, Icons.Rounded.Map, R.string.nav_map),
    TopLevelTab(ParkMateRoute.SpotsHistory, Icons.Rounded.History, R.string.nav_spots),
    TopLevelTab(ParkMateRoute.Settings, Icons.Rounded.Settings, R.string.nav_settings)
)

@Composable
fun ParkMateNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination

    // Show bottom/rail nav on map, spots, settings, and current spot details (matching App.tsx showBottomNav)
    val showMainNavigation = currentDest?.let { dest ->
        dest.hasRoute<ParkMateRoute.MapHome>() ||
                dest.hasRoute<ParkMateRoute.SpotsHistory>() ||
                dest.hasRoute<ParkMateRoute.Settings>() ||
                dest.hasRoute<ParkMateRoute.CurrentSpotDetails>()
    } ?: true

    val activeTabRoute: ParkMateRoute = when {
        currentDest?.hasRoute<ParkMateRoute.SpotsHistory>() == true -> ParkMateRoute.SpotsHistory
        currentDest?.hasRoute<ParkMateRoute.Settings>() == true -> ParkMateRoute.Settings
        else -> ParkMateRoute.MapHome
    }

    val navigateToTopLevel = { target: ParkMateRoute ->
        navController.navigate(target) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp
        val colors = ParkMateThemeTokens.colors

        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavigationRail && showMainNavigation) {
                NavigationRail(
                    containerColor = colors.surface,
                    contentColor = colors.text,
                    modifier = Modifier
                        .fillMaxHeight()
                        .statusBarsPadding()
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TOP_LEVEL_TABS.forEach { tab ->
                        val selected = activeTabRoute == tab.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateToTopLevel(tab.route) },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = stringResource(tab.labelRes)
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(tab.labelRes),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = colors.blue,
                                selectedTextColor = colors.blue,
                                indicatorColor = colors.blueSoft,
                                unselectedIconColor = colors.muted,
                                unselectedTextColor = colors.muted
                            )
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = colors.bg,
                bottomBar = {
                    if (!useNavigationRail && showMainNavigation) {
                        ParkMateBottomBar(
                            activeRoute = activeTabRoute,
                            onSelectTab = navigateToTopLevel
                        )
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = ParkMateRoute.MapHome,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = if (!useNavigationRail && showMainNavigation) innerPadding.calculateBottomPadding() else 0.dp)
                ) {
                    composable<ParkMateRoute.MapHome> {
                        val homeViewModel: HomeViewModel = hiltViewModel()
                        HomeScreen(
                            viewModel = homeViewModel,
                            onOpenCurrentSpotDetails = { spotId ->
                                navController.navigate(ParkMateRoute.CurrentSpotDetails(spotId))
                            },
                            onNavigateQuickSave = { spotId ->
                                navController.navigate(ParkMateRoute.QuickSave(spotId))
                            },
                            onNavigateGuideMe = { spotId ->
                                navController.navigate(ParkMateRoute.GuideMe(spotId))
                            }
                        )
                    }

                    composable<ParkMateRoute.SpotsHistory> {
                        val historyViewModel: HistoryViewModel = hiltViewModel()
                        HistoryScreen(
                            viewModel = historyViewModel,
                            onOpenSpot = { spotId ->
                                navController.navigate(ParkMateRoute.SpotRead(spotId))
                            }
                        )
                    }

                    composable<ParkMateRoute.Settings> {
                        val settingsViewModel: SettingsViewModel = hiltViewModel()
                        SettingsScreen(viewModel = settingsViewModel)
                    }

                    composable<ParkMateRoute.CurrentSpotDetails> { entry ->
                        val route = entry.toRoute<ParkMateRoute.CurrentSpotDetails>()
                        val detailsViewModel: SpotDetailsViewModel = hiltViewModel()
                        SpotReadScreen(
                            spotId = route.spotId,
                            viewModel = detailsViewModel,
                            onBack = { navController.popBackStack() },
                            onOpenPhotoViewer = { index ->
                                navController.navigate(ParkMateRoute.PhotoViewer(route.spotId, index))
                            },
                            onAddPhoto = {
                                navController.navigate(
                                    ParkMateRoute.CameraPermission(
                                        spotId = route.spotId,
                                        returnToRead = true
                                    )
                                )
                            },
                            onEditSpot = {
                                navController.navigate(ParkMateRoute.AddDetails(route.spotId))
                            },
                            onGuideMe = {
                                navController.navigate(ParkMateRoute.GuideMe(route.spotId))
                            },
                            onDeleted = {
                                navController.popBackStack(ParkMateRoute.MapHome, inclusive = false)
                            },
                            showInlineEditableNoteAndTimer = true
                        )
                    }

                    composable<ParkMateRoute.QuickSave> { entry ->
                        val route = entry.toRoute<ParkMateRoute.QuickSave>()
                        val homeViewModel: HomeViewModel = hiltViewModel()
                        QuickSaveScreen(
                            viewModel = homeViewModel,
                            onAddPhoto = {
                                navController.navigate(
                                    ParkMateRoute.CameraPermission(
                                        spotId = route.spotId,
                                        returnToRead = false
                                    )
                                )
                            },
                            onAddNote = {
                                navController.navigate(
                                    ParkMateRoute.AddDetails(
                                        spotId = route.spotId,
                                        fromPhotoReview = false
                                    )
                                )
                            },
                            onSkip = {
                                navController.navigate(ParkMateRoute.SpotRead(route.spotId)) {
                                    popUpTo<ParkMateRoute.MapHome>()
                                }
                            },
                            onClose = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable<ParkMateRoute.CameraPermission> { entry ->
                        val route = entry.toRoute<ParkMateRoute.CameraPermission>()
                        CameraPermissionScreen(
                            onAllowProceedToCamera = {
                                navController.navigate(
                                    ParkMateRoute.CameraCapture(
                                        spotId = route.spotId,
                                        returnToRead = route.returnToRead
                                    )
                                ) {
                                    popUpTo<ParkMateRoute.CameraPermission> { inclusive = true }
                                }
                            },
                            onNotNow = {
                                navController.navigate(
                                    ParkMateRoute.AddDetails(
                                        spotId = route.spotId,
                                        fromPhotoReview = false
                                    )
                                ) {
                                    popUpTo<ParkMateRoute.CameraPermission> { inclusive = true }
                                }
                            },
                            onClose = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable<ParkMateRoute.CameraCapture> { entry ->
                        val route = entry.toRoute<ParkMateRoute.CameraCapture>()
                        val cameraViewModel: CameraFlowViewModel = hiltViewModel()
                        CameraCaptureScreen(
                            viewModel = cameraViewModel,
                            onClose = { navController.popBackStack() },
                            onPhotoCaptured = { capturedPath ->
                                navController.navigate(
                                    ParkMateRoute.PhotoReview(
                                        spotId = route.spotId,
                                        photoPath = capturedPath,
                                        returnToRead = route.returnToRead
                                    )
                                )
                            }
                        )
                    }

                    composable<ParkMateRoute.PhotoReview> { entry ->
                        val route = entry.toRoute<ParkMateRoute.PhotoReview>()
                        val cameraViewModel: CameraFlowViewModel = hiltViewModel()
                        PhotoReviewScreen(
                            spotId = route.spotId,
                            photoPath = route.photoPath,
                            viewModel = cameraViewModel,
                            onRetake = { navController.popBackStack() },
                            onPhotoSaved = {
                                navController.navigate(
                                    ParkMateRoute.AddDetails(
                                        spotId = route.spotId,
                                        fromPhotoReview = true
                                    )
                                ) {
                                    popUpTo<ParkMateRoute.CameraCapture> { inclusive = true }
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<ParkMateRoute.AddDetails> { entry ->
                        val route = entry.toRoute<ParkMateRoute.AddDetails>()
                        val detailsViewModel: SpotDetailsViewModel = hiltViewModel()
                        AddDetailsScreen(
                            spotId = route.spotId,
                            viewModel = detailsViewModel,
                            onBack = { navController.popBackStack() },
                            onAddPhoto = {
                                navController.navigate(
                                    ParkMateRoute.CameraPermission(
                                        spotId = route.spotId,
                                        returnToRead = false
                                    )
                                )
                            },
                            onSaved = {
                                navController.navigate(ParkMateRoute.SpotRead(route.spotId)) {
                                    popUpTo<ParkMateRoute.MapHome>()
                                }
                            }
                        )
                    }

                    composable<ParkMateRoute.SpotRead> { entry ->
                        val route = entry.toRoute<ParkMateRoute.SpotRead>()
                        val detailsViewModel: SpotDetailsViewModel = hiltViewModel()
                        SpotReadScreen(
                            spotId = route.spotId,
                            viewModel = detailsViewModel,
                            onBack = {
                                if (!navController.popBackStack()) {
                                    navController.navigate(ParkMateRoute.MapHome)
                                }
                            },
                            onOpenPhotoViewer = { index ->
                                navController.navigate(ParkMateRoute.PhotoViewer(route.spotId, index))
                            },
                            onAddPhoto = {
                                navController.navigate(
                                    ParkMateRoute.CameraPermission(
                                        spotId = route.spotId,
                                        returnToRead = true
                                    )
                                )
                            },
                            onEditSpot = {
                                navController.navigate(ParkMateRoute.AddDetails(route.spotId))
                            },
                            onGuideMe = {
                                navController.navigate(ParkMateRoute.GuideMe(route.spotId))
                            },
                            onDeleted = {
                                navController.popBackStack(ParkMateRoute.MapHome, inclusive = false)
                            },
                            showInlineEditableNoteAndTimer = false
                        )
                    }

                    composable<ParkMateRoute.PhotoViewer> { entry ->
                        val route = entry.toRoute<ParkMateRoute.PhotoViewer>()
                        val detailsViewModel: SpotDetailsViewModel = hiltViewModel()
                        val state by detailsViewModel.uiState.collectAsStateWithLifecycle()
                        androidx.compose.runtime.LaunchedEffect(route.spotId) {
                            detailsViewModel.loadSpot(route.spotId)
                        }
                        PhotoViewerScreen(
                            photoPaths = state.spot?.photoPaths.orEmpty(),
                            initialIndex = route.initialIndex,
                            onClose = { navController.popBackStack() }
                        )
                    }

                    composable<ParkMateRoute.GuideMe> { entry ->
                        val route = entry.toRoute<ParkMateRoute.GuideMe>()
                        val guideViewModel: GuideMeViewModel = hiltViewModel()
                        GuideMeScreen(
                            spotId = route.spotId,
                            viewModel = guideViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParkMateBottomBar(
    activeRoute: ParkMateRoute,
    onSelectTab: (ParkMateRoute) -> Unit
) {
    val colors = ParkMateThemeTokens.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface.copy(alpha = 0.97f))
            .navigationBarsPadding()
    ) {
        HorizontalDivider(color = colors.line)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 28.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TOP_LEVEL_TABS.forEach { tab ->
                val isSelected = activeRoute == tab.route
                Column(
                    modifier = Modifier
                        .width(76.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectTab(tab.route) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(54.dp)
                            .height(31.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSelected) colors.blueSoft else androidx.compose.ui.graphics.Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = stringResource(tab.labelRes),
                            tint = if (isSelected) colors.blue else colors.muted,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(tab.labelRes),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) colors.blue else colors.muted
                    )
                }
            }
        }
    }
}
