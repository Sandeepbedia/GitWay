/*
 * GitWay — an Android client for GitHub.
 *
 * This file is part of GitWay. GitWay is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * GitWay is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * GitWay. If not, see <https://www.gnu.org/licenses/>.
 */
package com.io.git.way.navigation

import androidx.activity.ComponentActivity
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import com.io.git.way.ui.screens.profile.ProfileScreen
import com.io.git.way.ui.screens.pulls.PullRequestsScreen
import com.io.git.way.ui.screens.releases.ReleasesScreen
import com.io.git.way.ui.screens.repos.RepositoryListScreen
import com.io.git.way.ui.screens.splash.SplashScreen
import com.io.git.way.ui.screens.upload.UploadProgressScreen
import com.io.git.way.ui.theme.AppThemeMode

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

    NavHost(
        navController = navController,
        startDestination = Routes.Splash.route,
        enterTransition = {
            slideInHorizontally(
                animationSpec = tween(320),
                initialOffsetX = { fullWidth -> fullWidth / 6 }
            ) + fadeIn(animationSpec = tween(260))
        },
        exitTransition = {
            slideOutHorizontally(
                animationSpec = tween(320),
                targetOffsetX = { fullWidth -> -fullWidth / 10 }
            ) + fadeOut(animationSpec = tween(200))
        },
        popEnterTransition = {
            slideInHorizontally(
                animationSpec = tween(320),
                initialOffsetX = { fullWidth -> -fullWidth / 10 }
            ) + fadeIn(animationSpec = tween(260))
        },
        popExitTransition = {
            slideOutHorizontally(
                animationSpec = tween(320),
                targetOffsetX = { fullWidth -> fullWidth / 6 }
            ) + fadeOut(animationSpec = tween(200))
        }
    ) {
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
                onNavigateProfile = { navigateToTab(navController, Routes.Profile.route) },
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange
            )
        }
        composable(Routes.Profile.route) {
            ProfileScreen(
                sessionViewModel = sessionViewModel,
                onNavigateRepositories = { navigateToTab(navController, Routes.RepositoryList.route) },
                onRepositorySelected = { repo ->
                    sessionViewModel.selectRepository(repo)
                    navController.navigate(Routes.RepositoryBrowser.route)
                },
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
                onBack = { navController.popBackStack() },
                onContinue = { navController.navigate(Routes.Analysis.route) }
            )
        }
        composable(Routes.Analysis.route) {
            AnalysisScreen(
                sessionViewModel = sessionViewModel,
                onBack = { navController.popBackStack() },
                onContinue = { navController.navigate(Routes.Confirmation.route) }
            )
        }
        composable(Routes.Confirmation.route) {
            ConfirmationScreen(
                sessionViewModel = sessionViewModel,
                onBack = { navController.popBackStack() },
                onConfirmUpload = { navController.navigate(Routes.UploadProgress.route) }
            )
        }
        composable(Routes.UploadProgress.route) {
            UploadProgressScreen(
                sessionViewModel = sessionViewModel,
                onBack = { navController.popBackStack() },
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
