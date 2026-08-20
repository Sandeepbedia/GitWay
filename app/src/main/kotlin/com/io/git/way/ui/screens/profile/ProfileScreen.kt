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

package com.io.git.way.ui.screens.profile

import android.content.Intent
import android.net.Uri
import android.widget.Toast

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.fragment.app.FragmentActivity

import coil.compose.AsyncImage

import androidx.lifecycle.viewmodel.compose.viewModel

import com.io.git.way.GitWayApp
import com.io.git.way.domain.model.ApiRateLimit
import com.io.git.way.domain.model.GitRepository
import com.io.git.way.domain.model.GitUser
import com.io.git.way.ui.common.GitWaySessionViewModel
import com.io.git.way.ui.common.GitWayViewModelFactory
import com.io.git.way.ui.common.authenticateWithBiometrics
import com.io.git.way.ui.screens.repos.CreateRepositoryDialog
import com.io.git.way.ui.screens.repos.RepositoryListViewModel
import com.io.git.way.ui.theme.AppThemeMode
import com.io.git.way.ui.theme.BottomNavTab
import com.io.git.way.ui.theme.DiffModifiedYellow
import com.io.git.way.ui.theme.GlassBlobBlue
import com.io.git.way.ui.theme.GlassBlobPink
import com.io.git.way.ui.theme.GlassBlobPurple
import com.io.git.way.ui.theme.GlassBlobTeal
import com.io.git.way.ui.theme.GlassCard
import com.io.git.way.ui.theme.GlassChip
import com.io.git.way.ui.theme.GlassFloatingBottomNav
import com.io.git.way.ui.theme.GlassSecondaryButton
import com.io.git.way.ui.theme.LiquidGlassBackground

import kotlinx.coroutines.launch


@Composable
fun ProfileScreen(
    sessionViewModel: GitWaySessionViewModel,
    onNavigateRepositories: () -> Unit,
    onRepositorySelected: (GitRepository) -> Unit,
    onDisconnect: () -> Unit,
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    repoViewModel: RepositoryListViewModel = viewModel(
        factory = GitWayViewModelFactory(
            (LocalContext.current.applicationContext as GitWayApp)
                .container
                .gitHubRepository
        )
    )
) {
    val state = sessionViewModel.state
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val scope = rememberCoroutineScope()

    val biometricLockManager = remember {
        (context.applicationContext as GitWayApp)
            .container
            .biometricLockManager
    }

    var lockEnabled by remember {
        mutableStateOf(
            biometricLockManager.isLockEnabled()
        )
    }

    var biometricError by remember {
        mutableStateOf<String?>(null)
    }

    var showDisconnectConfirm by remember {
        mutableStateOf(false)
    }

    var showCreateRepoDialog by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        sessionViewModel.loadOverviewData()
    }

    val isRefreshing =
        state.isLoadingUser ||
            state.isLoadingOverviewRepositories

    fun toggleBiometricLock(enable: Boolean) {
        if (!biometricLockManager.isBiometricAvailable()) {
            biometricError =
                "No fingerprint or face unlock is set up on this device yet."
            return
        }

        scope.launch {
            val confirmed = activity.authenticateWithBiometrics(
                title = if (enable) {
                    "Enable app lock"
                } else {
                    "Disable app lock"
                },
                subtitle = "Confirm it's you before changing this"
            )

            if (confirmed) {
                biometricLockManager.setLockEnabled(enable)
                lockEnabled = enable
                biometricError = null
            }
        }
    }

    LiquidGlassBackground {

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { padding ->

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    sessionViewModel.refreshOverviewData()
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),

                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 8.dp,
                        end = 16.dp,
                        bottom = 110.dp
                    ),

                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    item {
                        Text(
                            text = "Profile",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }

                    item {
                        HeroHeaderCard(
                            user = state.currentUser,
                            isLoading = state.isLoadingUser,
                            errorMessage = state.userError
                        )
                    }

                    state.currentUser?.let { user ->
                        item {
                            StatsRow(
                                user = user,
                                repositories = state.overviewRepositories
                            )
                        }
                    }

                    item {
                        SectionLabel("Quick Actions")
                    }

                    item {
                        QuickActionsCard(
                            onNewRepo = {
                                showCreateRepoDialog = true
                            },
                            onRefresh = {
                                repoViewModel.loadRepositories()

                                Toast.makeText(
                                    context,
                                    "Refreshing repositories…",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }

                    if (state.overviewRepositories.isNotEmpty()) {

                        item {
                            SectionLabel("Developer Performance")
                        }

                        item {
                            LanguageBreakdownCard(
                                repositories = state.overviewRepositories
                            )
                        }

                        item {
                            RepositoryCompositionCard(
                                repositories = state.overviewRepositories
                            )
                        }
                    }

                    state.currentUser?.let { user ->

                        item {
                            SectionLabel("Achievements")
                        }

                        item {
                            AchievementsRow(
                                user = user,
                                repositories = state.overviewRepositories
                            )
                        }
                    }

                    item {
                        SectionLabel("Connected Account")
                    }

                    item {
                        ConnectedAccountCard(
                            user = state.currentUser,
                            rateLimit = state.apiRateLimit,
                            onOpenGitHub = {
                                state.currentUser
                                    ?.htmlUrl
                                    ?.takeIf { it.isNotBlank() }
                                    ?.let { url ->
                                        runCatching {
                                            context.startActivity(
                                                Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse(url)
                                                )
                                            )
                                        }
                                    }
                            }
                        )
                    }

                    item {
                        SectionLabel("Appearance")
                    }

                    item {
                        AppearanceCard(
                            themeMode = themeMode,
                            onThemeModeChange = onThemeModeChange
                        )
                    }

                    item {
                        SectionLabel("Security")
                    }

                    item {
                        SecurityCard(
                            lockEnabled = lockEnabled,
                            biometricAvailable =
                                biometricLockManager
                                    .isBiometricAvailable(),
                            errorMessage = biometricError,
                            onToggle = ::toggleBiometricLock
                        )
                    }

                    item {
                        SectionLabel("About")
                    }

                    item {
                        AboutCard()
                    }

                    item {
                        SectionLabel("Danger Zone")
                    }

                    item {
                        DangerZoneCard(
                            onDisconnect = {
                                showDisconnectConfirm = true
                            }
                        )
                    }
                }
            }
        }

        GlassFloatingBottomNav(
            selected = BottomNavTab.PROFILE,
            onSelect = { tab ->
                when (tab) {
                    BottomNavTab.REPOSITORIES ->
                        onNavigateRepositories()

                    BottomNavTab.PROFILE ->
                        Unit
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showCreateRepoDialog) {

        CreateRepositoryDialog(
            isCreating = repoViewModel.uiState.isCreatingRepo,
            errorMessage = repoViewModel.uiState.createRepoError,

            onDismiss = {
                showCreateRepoDialog = false
                repoViewModel.clearCreateRepoError()
            },

            onConfirm = { name, description, isPrivate ->

                repoViewModel.createRepository(
                    name,
                    description,
                    isPrivate
                ) { repo ->

                    showCreateRepoDialog = false
                    onRepositorySelected(repo)
                }
            },

            onCreated = {
                showCreateRepoDialog = false
            }
        )
    }

    if (showDisconnectConfirm) {

        AlertDialog(
            onDismissRequest = {
                showDisconnectConfirm = false
            },

            title = {
                Text("Disconnect GitHub?")
            },

            text = {
                Text(
                    "This removes your saved token from this device. " +
                        "You'll need to reconnect to use Git Way again."
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        showDisconnectConfirm = false

                        sessionViewModel.disconnect()

                        onDisconnect()
                    }
                ) {
                    Text(
                        "Disconnect",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showDisconnectConfirm = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}


@Composable
private fun SectionLabel(text: String) {

    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 4.dp)
    )
}


@Composable
private fun QuickActionsCard(
    onNewRepo: () -> Unit,
    onRefresh: () -> Unit
) {

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        padding = 8.dp
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            QuickActionTile(
                icon = Icons.Filled.Add,
                label = "New repository",
                onClick = onNewRepo,
                modifier = Modifier.weight(1f)
            )

            QuickActionTile(
                icon = Icons.Filled.Refresh,
                label = "Refresh repositories",
                onClick = onRefresh,
                modifier = Modifier.weight(1f)
            )
        }
    }
}


@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    val interaction = remember {
        MutableInteractionSource()
    }

    val pressed by interaction.collectIsPressedAsState()

    val bg by animateColorAsState(
        targetValue =
            if (pressed) {
                MaterialTheme.colorScheme
                    .onSurfaceVariant
                    .copy(alpha = 0.12f)
            } else {
                Color.Transparent
            },
        animationSpec = tween(140),
        label = "quickActionBg"
    )

    Column(
        modifier = modifier
            .height(76.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),

        horizontalAlignment = Alignment.CenterHorizontally,

        verticalArrangement = Arrangement.Center
    ) {

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


@Composable
private fun HeroHeaderCard(
    user: GitUser?,
    isLoading: Boolean,
    errorMessage: String?
) {

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        padding = 20.dp
    ) {

        when {

            user != null -> {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    val infiniteTransition =
                        rememberInfiniteTransition(
                            label = "avatarRing"
                        )

                    val ringRotation by
                        infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec =
                                infiniteRepeatable(
                                    animation = tween(
                                        durationMillis = 6000,
                                        easing = LinearEasing
                                    ),
                                    repeatMode = RepeatMode.Restart
                                ),
                            label = "ringRotation"
                        )

                    Box(
                        contentAlignment = Alignment.Center
                    ) {

                        Box(
                            modifier = Modifier
                                .size(112.dp)
                                .rotate(ringRotation)
                                .background(
                                    brush = Brush.sweepGradient(
                                        listOf(
                                            GlassBlobBlue,
                                            GlassBlobPurple,
                                            GlassBlobTeal,
                                            GlassBlobPink,
                                            GlassBlobBlue
                                        )
                                    ),
                                    shape = CircleShape
                                )
                        )

                        if (!user.avatarUrl.isNullOrBlank()) {

                            AsyncImage(
                                model = user.avatarUrl,
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                            )

                        } else {

                            Icon(
                                imageVector =
                                    Icons.Filled.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(100.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text(
                        text =
                            user.displayName
                                ?.takeIf { it.isNotBlank() }
                                ?: user.username,

                        style =
                            MaterialTheme.typography.headlineSmall,

                        modifier = Modifier.padding(top = 14.dp)
                    )

                    Text(
                        text = "@${user.username}",
                        style = MaterialTheme.typography.bodyMedium,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )

                    if (!user.bio.isNullOrBlank()) {

                        Text(
                            text = user.bio.orEmpty(),
                            style =
                                MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    val meta = listOfNotNull(
                        user.company
                            ?.takeIf { it.isNotBlank() },

                        user.location
                            ?.takeIf { it.isNotBlank() },

                        user.createdAt
                            .takeIf { it.length >= 4 }
                            ?.let {
                                "Joined ${it.take(4)}"
                            }
                    )

                    if (meta.isNotEmpty()) {

                        Text(
                            text = meta.joinToString("  ·  "),
                            style =
                                MaterialTheme.typography.labelSmall,
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            isLoading -> {

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically,
                    horizontalArrangement =
                        Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )

                    Text(
                        text = "Loading your profile…",
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }

            else -> {

                Text(
                    text =
                        errorMessage
                            ?: "Couldn't load your profile.",
                    color =
                        MaterialTheme.colorScheme.error
                )
            }
        }
    }
}


@Composable
private fun StatsRow(
    user: GitUser,
    repositories: List<GitRepository>
) {

    val totalStars =
        repositories.sumOf {
            it.stargazersCount
        }

    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            StatCell(
                label = "Repositories",
                value = user.publicRepos.toString()
            )

            StatCell(
                label = "Followers",
                value = user.followers.toString()
            )

            StatCell(
                label = "Following",
                value = user.following.toString()
            )

            StatCell(
                label = "Stars",
                value = totalStars.toString()
            )
        }
    }
}


@Composable
private fun StatCell(
    label: String,
    value: String
) {

    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )
    }
}


@Composable
private fun LanguageBreakdownCard(
    repositories: List<GitRepository>
) {

    val palette =
        listOf(
            GlassBlobBlue,
            GlassBlobPurple,
            GlassBlobTeal,
            GlassBlobPink,
            DiffModifiedYellow
        )

    val counts =
        repositories
            .mapNotNull {
                it.language
                    ?.takeIf { language ->
                        language.isNotBlank()
                    }
            }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(5)

    val total =
        counts
            .sumOf { it.value }
            .coerceAtLeast(1)

    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {

        Text(
            text = "Languages",
            style = MaterialTheme.typography.titleSmall
        )

        if (counts.isEmpty()) {

            Text(
                text = "No language data yet.",
                style = MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )

        } else {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(10.dp)
                    .clip(
                        RoundedCornerShape(5.dp)
                    )
            ) {

                counts.forEachIndexed { index, entry ->

                    Box(
                        modifier = Modifier
                            .weight(
                                entry.value
                                    .toFloat()
                                    .coerceAtLeast(0.01f)
                            )
                            .fillMaxSize()
                            .background(
                                palette[
                                    index % palette.size
                                ]
                            )
                    )
                }
            }

            counts.forEachIndexed { index, entry ->

                val pct =
                    entry.value * 100f / total

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {

                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                palette[
                                    index % palette.size
                                ],
                                CircleShape
                            )
                    )

                    Text(
                        text = entry.key,
                        style =
                            MaterialTheme.typography.bodySmall,
                        modifier =
                            Modifier
                                .padding(start = 8.dp)
                                .weight(1f)
                    )

                    Text(
                        text =
                            if (pct < 1f) {
                                "<1%"
                            } else {
                                "${pct.toInt()}%"
                            },
                        style =
                            MaterialTheme.typography.labelSmall,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }
            }
        }
    }
}


@Composable
private fun RepositoryCompositionCard(
    repositories: List<GitRepository>
) {

    val privateCount =
        repositories.count { it.isPrivate }

    val publicCount =
        repositories.size - privateCount

    val archivedCount =
        repositories.count { it.archived }

    val forkedCount =
        repositories.count { it.isFork }

    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {

        Text(
            text = "Repository composition",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            CompositionTile(
                icon = Icons.Filled.Shield,
                label = "Private",
                count = privateCount,
                tint = GlassBlobPurple
            )

            CompositionTile(
                icon = Icons.Filled.Description,
                label = "Public",
                count = publicCount,
                tint = GlassBlobTeal
            )

            CompositionTile(
                icon = Icons.Filled.Archive,
                label = "Archived",
                count = archivedCount,
                tint = GlassBlobBlue
            )

            CompositionTile(
                icon = Icons.Filled.CallSplit,
                label = "Forked",
                count = forkedCount,
                tint = GlassBlobPink
            )
        }
    }
}


@Composable
private fun CompositionTile(
    icon: ImageVector,
    label: String,
    count: Int,
    tint: Color
) {

    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )

        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 4.dp)
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )
    }
}


private data class Achievement(
    val label: String,
    val icon: ImageVector,
    val unlocked: Boolean
)


@Composable
private fun AchievementsRow(
    user: GitUser,
    repositories: List<GitRepository>
) {

    val totalStars =
        repositories.sumOf {
            it.stargazersCount
        }

    val languageCount =
        repositories
            .mapNotNull { it.language }
            .distinct()
            .size

    val achievements =
        listOf(

            Achievement(
                label = "10+ Repos",
                icon = Icons.Filled.Description,
                unlocked = user.publicRepos >= 10
            ),

            Achievement(
                label = "50+ Repos",
                icon = Icons.Filled.Description,
                unlocked = user.publicRepos >= 50
            ),

            Achievement(
                label = "100+ Followers",
                icon = Icons.Filled.Groups,
                unlocked = user.followers >= 100
            ),

            Achievement(
                label = "100+ Stars",
                icon = Icons.Filled.Star,
                unlocked = totalStars >= 100
            ),

            Achievement(
                label = "Polyglot",
                icon = Icons.Filled.Palette,
                unlocked = languageCount >= 3
            ),

            Achievement(
                label = "Connected",
                icon = Icons.Filled.Verified,
                unlocked = true
            )
        )

    LazyRow(
        horizontalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {

        items(
            items = achievements,
            key = { it.label }
        ) { achievement ->

            val tint =
                if (achievement.unlocked) {
                    GlassBlobTeal
                } else {
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
                }

            GlassCard(
                modifier = Modifier.width(108.dp),
                padding = 14.dp
            ) {

                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Icon(
                        imageVector =
                            achievement.icon,
                        contentDescription = null,
                        tint = tint,
                        modifier =
                            Modifier.size(26.dp)
                    )

                    Text(
                        text = achievement.label,
                        style =
                            MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color =
                            if (achievement.unlocked) {
                                MaterialTheme.colorScheme
                                    .onSurface
                            } else {
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                            },
                        modifier =
                            Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}


@Composable
private fun ConnectedAccountCard(
    user: GitUser?,
    rateLimit: ApiRateLimit?,
    onOpenGitHub: () -> Unit
) {

    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = null,
                tint = GlassBlobTeal,
                modifier = Modifier.size(18.dp)
            )

            Text(
                text =
                    if (user != null) {
                        "Connected as @${user.username}"
                    } else {
                        "Connected"
                    },
                style =
                    MaterialTheme.typography.titleSmall,
                modifier =
                    Modifier
                        .padding(start = 8.dp)
                        .weight(1f)
            )

            if (user != null) {

                IconButton(
                    onClick = onOpenGitHub
                ) {

                    Icon(
                        imageVector =
                            Icons.Filled.OpenInNew,
                        contentDescription =
                            "Open on GitHub",
                        modifier =
                            Modifier.size(18.dp)
                    )
                }
            }
        }

        rateLimit?.let { limit ->

            val fraction =
                if (limit.limit > 0) {
                    (
                        limit.remaining.toFloat() /
                            limit.limit.toFloat()
                        ).coerceIn(0f, 1f)
                } else {
                    1f
                }

            Row(
                verticalAlignment =
                    Alignment.CenterVertically,
                modifier =
                    Modifier.padding(top = 10.dp)
            ) {

                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )

                Text(
                    text =
                        "${limit.remaining} of ${limit.limit} API requests remaining",
                    style =
                        MaterialTheme.typography.labelSmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant,
                    modifier =
                        Modifier.padding(start = 6.dp)
                )
            }

            LinearProgressIndicator(
                progress = { fraction },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .height(4.dp)
                        .clip(
                            RoundedCornerShape(2.dp)
                        )
            )
        }

        Text(
            text =
                "Your Personal Access Token is stored encrypted on this device only.",
            style =
                MaterialTheme.typography.labelSmall,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant,
            modifier =
                Modifier.padding(top = 10.dp)
        )
    }
}


@Composable
private fun AppearanceCard(
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit
) {

    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {

        Text(
            text = "Theme",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            AppThemeMode.entries.forEach { mode ->

                GlassChip(
                    text = profileThemeLabel(mode),
                    selected = themeMode == mode,
                    onClick = {
                        onThemeModeChange(mode)
                    },
                    leadingIcon = {

                        Icon(
                            imageVector =
                                profileThemeIcon(mode),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )
            }
        }
    }
}


private fun profileThemeLabel(
    mode: AppThemeMode
): String {

    return when (mode) {

        AppThemeMode.SYSTEM ->
            "System"

        AppThemeMode.LIGHT ->
            "Light"

        AppThemeMode.DARK ->
            "Dark"

        AppThemeMode.AMOLED ->
            "AMOLED"
    }
}


private fun profileThemeIcon(
    mode: AppThemeMode
): ImageVector {

    return when (mode) {

        AppThemeMode.SYSTEM ->
            Icons.Filled.Brightness6

        AppThemeMode.LIGHT ->
            Icons.Filled.Brightness7

        AppThemeMode.DARK ->
            Icons.Filled.Brightness4

        AppThemeMode.AMOLED ->
            Icons.Filled.DarkMode
    }
}


@Composable
private fun SecurityCard(
    lockEnabled: Boolean,
    biometricAvailable: Boolean,
    errorMessage: String?,
    onToggle: (Boolean) -> Unit
) {

    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Icon(
                imageVector =
                    Icons.Filled.Fingerprint,
                contentDescription = null,
                tint = GlassBlobPurple,
                modifier = Modifier.size(20.dp)
            )

            Column(
                modifier =
                    Modifier
                        .padding(start = 10.dp)
                        .weight(1f)
            ) {

                Text(
                    text = "Fingerprint / Face Lock",
                    style =
                        MaterialTheme.typography.titleSmall
                )

                Text(
                    text =
                        if (biometricAvailable) {
                            "Require biometric unlock when Git Way opens."
                        } else {
                            "No biometrics set up on this device."
                        },
                    style =
                        MaterialTheme.typography.labelSmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }

            Switch(
                checked = lockEnabled,
                onCheckedChange = onToggle,
                enabled =
                    biometricAvailable || lockEnabled
            )
        }

        if (errorMessage != null) {

            Text(
                text = errorMessage,
                color =
                    MaterialTheme.colorScheme.error,
                style =
                    MaterialTheme.typography.bodySmall,
                modifier =
                    Modifier.padding(top = 8.dp)
            )
        }

        Row(
            verticalAlignment =
                Alignment.CenterVertically,
            modifier =
                Modifier.padding(top = 12.dp)
        ) {

            Icon(
                imageVector = Icons.Filled.Security,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )

            Text(
                text =
                    "Use biometric protection to prevent unauthorized access to Git Way.",
                style =
                    MaterialTheme.typography.labelSmall,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant,
                modifier =
                    Modifier.padding(start = 8.dp)
            )
        }
    }
}


@Composable
private fun AboutCard() {

    val context = LocalContext.current

    val versionName =
        remember {

            runCatching {

                context.packageManager
                    .getPackageInfo(
                        context.packageName,
                        0
                    )
                    .versionName

            }.getOrNull() ?: "—"
        }

    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )

            Text(
                text = "Git Way $versionName",
                style =
                    MaterialTheme.typography.titleSmall,
                modifier =
                    Modifier.padding(start = 8.dp)
            )
        }

        Text(
            text =
                "Open source, GNU GPL v3 licensed.",
            style =
                MaterialTheme.typography.labelSmall,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant,
            modifier =
                Modifier.padding(
                    top = 6.dp,
                    bottom = 10.dp
                )
        )

        GlassSecondaryButton(
            text = "View source on GitHub",
            onClick = {

                runCatching {

                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(
                                "https://github.com/Sandeepbedia/GitWay"
                            )
                        )
                    )
                }
            },
            leadingIcon = {

                Icon(
                    imageVector =
                        Icons.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        )
    }
}


@Composable
private fun DangerZoneCard(
    onDisconnect: () -> Unit
) {

    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint =
                    MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )

            Text(
                text = "Danger Zone",
                style =
                    MaterialTheme.typography.titleSmall,
                color =
                    MaterialTheme.colorScheme.error,
                modifier =
                    Modifier.padding(start = 8.dp)
            )
        }

        Text(
            text =
                "Disconnecting removes your saved token from this device. Nothing on GitHub is affected.",
            style =
                MaterialTheme.typography.labelSmall,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant,
            modifier =
                Modifier.padding(
                    top = 6.dp,
                    bottom = 10.dp
                )
        )

        GlassSecondaryButton(
            text = "Disconnect GitHub account",
            onClick = onDisconnect,
            leadingIcon = {

                Icon(
                    imageVector = Icons.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        )
    }
}