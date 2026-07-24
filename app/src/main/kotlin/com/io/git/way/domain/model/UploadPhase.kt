package com.io.git.way.domain.model

/**
 * Fine-grained phases of [com.io.git.way.domain.repository.GitHubRepository.syncChanges]
 * (PRD "Push HTTP 422 Fix" §11 Progress UI). Nothing is written to the branch until
 * UPDATING_BRANCH, so every earlier phase can be retried/aborted with zero remote
 * side effects.
 */
enum class UploadPhase {
    IDLE,
    VALIDATING,
    PREPARING,
    CREATING_BLOBS,
    CREATING_TREE,
    CREATING_COMMIT,
    UPDATING_BRANCH,
    VERIFYING,
    DONE
}
