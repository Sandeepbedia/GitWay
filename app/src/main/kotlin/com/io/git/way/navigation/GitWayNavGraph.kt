package com.io.git.way.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.io.git.way.GitWayApp
import com.io.git.way.ui.common.GitWaySessionViewModel
import com.io.git.way.ui.common.GitWayViewModelFactory
import com.io.git.way.ui.screens.actions.ActionsScreen
import com.io.git.way.ui.screens.analysis.AnalysisScreen
import com.io.git.way.ui.screens.auth.TokenScreen
import com.io.git.way.ui.screens.browser.RepositoryBrowserScreen
import com.io.git.way.ui.screens.complete.CompletionScreen
import com.io.git.way.ui.screens.confirm.ConfirmationScreen
import com.io.git.way.ui.screens.folder.FolderSelectionScreen
import com.io.git.way.ui.screens.issues.IssuesScreen
import com.io.git.way.ui.screens.overview.OverviewScreen
import com.io.git.way.ui.screens.profile.ProfileScreen
import com.io.git.way.ui.screens.pulls.PullRequestsScreen
import com.io.git.way.ui.screens.releases.ReleasesScreen
import com.io.git.way.ui.screens.repos.RepositoryListScreen
import com.io.git.way.ui.screens.splash.SplashScreen
import com.io.git.way.ui.screens.upload.UploadProgressScreen
import com.io.git.way.ui.theme.AppThemeMode

/**
 * Wires up the screen flow described in the PRDs:
 * Splash -> Token -> Repository List -> Folder Selection -> Analysis -> Confirmation -> Upload -> Completion.
 * Splash routes straight past Token when a valid token is already stored.
 *
 * [GitWaySessionViewModel] is scoped to the hosting Activity (not to any single back stack
 * entry) so the selected repo, scanned files, diff, and in-flight upload all survive
 * navigation and configuration changes across this whole flow (PRD2 §4).
 */
@Composable
fun GitWayNavGraph(
    navController: NavHostController,
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit
) {
    val gitHubRepository = (LocalContext.current.applicationContext as GitWayApp).container.gitHubRepository
    val viewModelFactory = GitWayViewModelFactory(gitHubRepository)
    val activity = LocalContext.current as ComponentActivity
    val sessionViewModel: GitWaySessionViewModel = viewModel(factory = viewModelFactory, viewModelStoreOwner = activity)

    NavHost(navController = navController, startDestination = Routes.Splash.route) {
        composable(Routes.Splash.route) {
            SplashScreen(
                onFinished = {
                    val destination = if (gitHubRepository.hasToken()) {
                        Routes.RepositoryList.route
                    } else {
                        Routes.Token.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Routes.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.Token.route) {
            TokenScreen(
                onConnected = {
                    navController.navigate(Routes.RepositoryList.route) {
                        popUpTo(Routes.Token.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.RepositoryList.route) {
            RepositoryListScreen(
                onRepositorySelected = { repo ->
                    sessionViewModel.selectRepository(repo)
                    navController.navigate(Routes.RepositoryBrowser.route)
                },
                onDisconnect = {
                    navController.navigate(Routes.Token.route) {
                        popUpTo(Routes.RepositoryList.route) { inclusive = true }
                    }
                },
                onNavigateOverview = { navigateToTab(navController, Routes.Overview.route) },
                onNavigateProfile = { navigateToTab(navController, Routes.Profile.route) },
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange
            )
        }
        composable(Routes.Overview.route) {
            OverviewScreen(
                sessionViewModel = sessionViewModel,
                onNavigateRepositories = { navigateToTab(navController, Routes.RepositoryList.route) },
                onNavigateProfile = { navigateToTab(navController, Routes.Profile.route) },
                onOpenRepository = { repo ->
                    sessionViewModel.selectRepository(repo)
                    navController.navigate(Routes.RepositoryBrowser.route)
                }
            )
        }
        composable(Routes.Profile.route) {
            ProfileScreen(
                sessionViewModel = sessionViewModel,
                onNavigateOverview = { navigateToTab(navController, Routes.Overview.route) },
                onNavigateRepositories = { navigateToTab(navController, Routes.RepositoryList.route) },
                onDisconnect = {
                    navController.navigate(Routes.Token.route) {
                        popUpTo(Routes.Profile.route) { inclusive = true }
                    }
                },
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange
            )
        }
        composable(Routes.RepositoryBrowser.route) {
            RepositoryBrowserScreen(
                sessionViewModel = sessionViewModel,
                onBack = { navController.popBackStack() },
                onSyncFromDevice = { navController.navigate(Routes.FolderSelection.route) },
                onOpenActions = { navController.navigate(Routes.Actions.route) },
                onOpenPullRequests = { navController.navigate(Routes.PullRequests.route) },
                onOpenIssues = { navController.navigate(Routes.Issues.route) },
                onOpenReleases = { navController.navigate(Routes.Releases.route) }
            )
        }
        composable(Routes.Actions.route) {
            ActionsScreen(
                sessionViewModel = sessionViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.PullRequests.route) {
            PullRequestsScreen(
                sessionViewModel = sessionViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.Issues.route) {
            IssuesScreen(
                sessionViewModel = sessionViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.Releases.route) {
            ReleasesScreen(
                sessionViewModel = sessionViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.FolderSelection.route) {
            FolderSelectionScreen(
                sessionViewModel = sessionViewModel,
                onContinue = { navController.navigate(Routes.Analysis.route) }
            )
        }
        composable(Routes.Analysis.route) {
            AnalysisScreen(
                sessionViewModel = sessionViewModel,
                onContinue = { navController.navigate(Routes.Confirmation.route) }
            )
        }
        composable(Routes.Confirmation.route) {
            ConfirmationScreen(
                sessionViewModel = sessionViewModel,
                onConfirmUpload = { navController.navigate(Routes.UploadProgress.route) }
            )
        }
        composable(Routes.UploadProgress.route) {
            UploadProgressScreen(
                sessionViewModel = sessionViewModel,
                onUploadFinished = {
                    navController.navigate(Routes.Completion.route) {
                        popUpTo(Routes.Splash.route) { inclusive = false }
                    }
                }
            )
        }
        composable(Routes.Completion.route) {
            CompletionScreen(
                sessionViewModel = sessionViewModel,
                onDone = {
                    navController.navigate(Routes.RepositoryList.route) {
                        popUpTo(Routes.RepositoryList.route) { inclusive = true }
                    }
                }
            )
        }
    }
}

/** Standard bottom-nav navigation: avoids stacking duplicate destinations and restores each tab's state. */
private fun navigateToTab(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(Routes.RepositoryList.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
