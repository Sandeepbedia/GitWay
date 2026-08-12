package com.io.git.way.ui.screens.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.io.git.way.GitWayApp
import com.io.git.way.ui.common.GitWayViewModelFactory
import com.io.git.way.ui.theme.DiffAddedGreen
import com.io.git.way.ui.theme.GlassCard
import com.io.git.way.ui.theme.GlassPrimaryButton
import com.io.git.way.ui.theme.GlassScaffold
import com.io.git.way.ui.theme.GlassSecondaryButton

/** The scopes Git Way actually needs. Classic PAT: tick all three. Fine-grained: give
 * the equivalent permissions (repository contents read/write, workflows, repo delete). */
private val REQUIRED_SCOPES = listOf(
    "repo" to "Access to all repositories",
    "workflow" to "Workflow updates & runs",
    "delete_repo" to "Delete repositories"
)

/** Screen 2: GitHub Personal Access Token input, validated live against the GitHub API. */
@Composable
fun TokenScreen(
    onConnected: () -> Unit,
    viewModel: AuthViewModel = viewModel(
        factory = GitWayViewModelFactory(
            (LocalContext.current.applicationContext as GitWayApp).container.gitHubRepository
        )
    )
) {
    val state = viewModel.uiState

    GlassScaffold(title = "Connect GitHub") { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Paste a GitHub Personal Access Token (classic or fine-grained). " +
                        "It's encrypted on-device and never leaves this app except to " +
                        "authenticate with GitHub.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Required scopes:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                REQUIRED_SCOPES.forEach { (scope, label) ->
                    ScopeRow(
                        label = label,
                        status = scopeStatus(scope, state)
                    )
                }
                if (state.authScopeWarning != null) {
                    Text(
                        text = state.authScopeWarning,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
            OutlinedTextField(
                value = state.token,
                onValueChange = viewModel::onTokenChange,
                label = { Text("Personal Access Token") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                isError = state.errorMessage != null,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            )
            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (state.missingScopes.isNotEmpty()) {
                Text(
                    text = "In token me in scopes ki kami hai: ${state.missingScopes.joinToString(", ")} — " +
                        "bina inke ye features kaam nahi karenge. Token update karo ya aage badho.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            GlassPrimaryButton(
                text = "Connect",
                onClick = { viewModel.connect(onConnected) },
                enabled = state.token.isNotBlank() && !state.isLoading,
                loading = state.isLoading,
                modifier = Modifier.padding(top = 16.dp)
            )
            if (state.missingScopes.isNotEmpty()) {
                GlassSecondaryButton(
                    text = "Continue anyway",
                    onClick = { viewModel.connectAnyway(onConnected) },
                    enabled = !state.isLoading,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }
}

private enum class ScopeStatus { GRANTED, MISSING, UNKNOWN }

private fun scopeStatus(scope: String, state: AuthUiState): ScopeStatus = when {
    !state.scopeCheckVisible || state.grantedScopes.isEmpty() -> ScopeStatus.UNKNOWN
    scope in state.grantedScopes -> ScopeStatus.GRANTED
    else -> ScopeStatus.MISSING
}

@Composable
private fun ScopeRow(label: String, status: ScopeStatus) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        when (status) {
            ScopeStatus.GRANTED -> Icon(
                Icons.Filled.CheckCircle, contentDescription = "Granted",
                tint = DiffAddedGreen, modifier = Modifier.size(16.dp)
            )
            ScopeStatus.MISSING -> Icon(
                Icons.Filled.Cancel, contentDescription = "Missing",
                tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)
            )
            ScopeStatus.UNKNOWN -> Icon(
                Icons.Filled.HelpOutline, contentDescription = "Unknown",
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = when (status) {
                ScopeStatus.GRANTED -> MaterialTheme.colorScheme.onSurface
                ScopeStatus.MISSING -> MaterialTheme.colorScheme.error
                ScopeStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
