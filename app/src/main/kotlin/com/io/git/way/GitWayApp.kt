package com.io.git.way

import android.app.Application

/** Application class for Git Way. Holds the process-wide [AppContainer]. */
class GitWayApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
