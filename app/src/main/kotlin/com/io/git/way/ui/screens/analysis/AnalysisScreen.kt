package com.io.git.way.ui.screens.analysis

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.io.git.way.domain.model.ChangeType
import com.io.git.way.domain.model.FileChange
import com.io.git.way.ui.common.GitWaySessionViewModel
import com.io.git.way.ui.theme.DiffAddedGreen
import com.io.git.way.ui.theme.DiffModifiedYellow
import com.io.git.way.ui.theme.DiffRemovedRed

/** Screen 5: Added / Modified / Removed diff preview (PRD1 §3.4 Analysis Screen). */
@Composable
fun AnalysisScreen(
    sessionViewModel: GitWaySessionViewModel,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val state = sessionViewModel.state
    val changes = state.fileChanges
    val hasChanges = changes.isNotEmpty()

    Scaffold(topBar = { TopAppBar(title = { Text("Changes") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            when {
                state.isComparing -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        val progress = state.compareProgress
                        Text(
                            text = if (progress != null) "Comparing ${progress.first} of ${progress.second} files" else "Comparing with GitHub…",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }

                state.compareError != null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.compareError, color = MaterialTheme.colorScheme.error)
                        OutlinedButton(
                            onClick = { sessionViewModel.runComparison(context) },
                            modifier = Modifier.padding(top = 12.dp)
                        ) { Text("Retry") }
                    }
                }

                !hasChanges -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No changes detected", style = MaterialTheme.typography.titleMedium)
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            DiffSection(
                                title = "Added",
                                color = DiffAddedGreen,
                                items = changes.filter { it.type == ChangeType.ADDED }
                            )
                        }
                        item {
                            DiffSection(
                                title = "Modified",
                                color = DiffModifiedYellow,
                                items = changes.filter { it.type == ChangeType.MODIFIED }
                            )
                        }
                        item {
                            DiffSection(
                                title = "Removed",
                                color = DiffRemovedRed,
                                items = changes.filter { it.type == ChangeType.REMOVED }
                            )
                        }
                    }

                    Button(
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    ) { Text("Review ${changes.size} changes") }
                }
            }
        }
    }
}

@Composable
private fun DiffSection(title: String, color: Color, items: List<FileChange>) {
    var expanded by remember { mutableStateOf(true) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(color, CircleShape)
                    )
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    CountBadge(count = items.size, color = color)
                }
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    if (items.isEmpty()) {
                        Text(
                            "None",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    } else {
                        items.forEach { change ->
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Text(change.fileName, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    change.filePath,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CountBadge(count: Int, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.18f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(count.toString(), style = MaterialTheme.typography.labelSmall, color = color)
    }
}
