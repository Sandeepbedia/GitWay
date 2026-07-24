package com.io.git.way.domain.model

/**
 * The two visible phases of [com.io.git.way.domain.repository.GitHubRepository.syncChanges]
 * (PRD2 "Upload Progress Screen"). The repo is only touched once FINALIZING reaches the
 * final ref update, so PREPARING can be retried/aborted with zero remote side effects.
 */
enum class UploadPhase {
    IDLE,
    PREPARING,
    FINALIZING,
    DONE
}
