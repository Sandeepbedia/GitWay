package com.io.git.way.ui.screens.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.io.git.way.GitWayApp
import com.io.git.way.ui.common.GitWayViewModelFactory
import com.io.git.way.ui.theme.GlassCard
import com.io.git.way.ui.theme.GlassPrimaryButton
import com.io.git.way.ui.theme.GlassScaffold

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
                    text = "Paste a GitHub Personal Access Token (classic or fine-grained) with " +
                        "repo access. It's encrypted on-device and never leaves this app except " +
                        "to authenticate with GitHub.",
                    style = MaterialTheme.typography.bodyMedium
                )
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
            GlassPrimaryButton(
                text = "Connect",
                onClick = { viewModel.connect(onConnected) },
                enabled = state.token.isNotBlank() && !state.isLoading,
                loading = state.isLoading,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
