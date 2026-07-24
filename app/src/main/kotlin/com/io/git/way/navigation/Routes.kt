package com.io.git.way.navigation

/** All screens in the Git Way flow, per PRD "App Screens" section. */
sealed class Routes(val route: String) {
    data object Splash : Routes("splash")
    data object Token : Routes("token")
    data object RepositoryList : Routes("repository_list")
    data object FolderSelection : Routes("folder_selection")
    data object Analysis : Routes("analysis")
    data object Confirmation : Routes("confirmation")
    data object UploadProgress : Routes("upload_progress")
    data object Completion : Routes("completion")
}
