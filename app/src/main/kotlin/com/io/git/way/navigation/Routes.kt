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

/** All screens in the Git Way flow, per PRD "App Screens" section. */
sealed class Routes(val route: String) {
    data object Splash : Routes("splash")
    data object Token : Routes("token")
    data object RepositoryList : Routes("repository_list")
    data object Profile : Routes("profile")
    data object RepositoryBrowser : Routes("repository_browser")
    data object FolderSelection : Routes("folder_selection")
    data object Analysis : Routes("analysis")
    data object Confirmation : Routes("confirmation")
    data object UploadProgress : Routes("upload_progress")
    data object Completion : Routes("completion")
    data object Actions : Routes("actions")
    data object PullRequests : Routes("pull_requests")
    data object Issues : Routes("issues")
    data object Releases : Routes("releases")
}
