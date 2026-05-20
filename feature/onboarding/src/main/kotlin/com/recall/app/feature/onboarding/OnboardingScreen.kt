package com.recall.app.feature.onboarding

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.recall.app.core.designsystem.component.RecallConfirmDialog
import com.recall.app.core.designsystem.theme.RecallTheme

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        viewModel.onPermissionResult(results, activity)
        if (results.values.all { it }) {
            onOnboardingComplete()
        }
    }

    LaunchedEffect(uiState.permissionState) {
        if (uiState.permissionState == PermissionState.GRANTED && !uiState.shouldShowOnboarding) {
            onOnboardingComplete()
        }
    }

    if (uiState.showRationaleDialog) {
        RecallConfirmDialog(
            title = "Media access needed",
            body = "Recall needs access to your photos and videos to search and organize " +
                "your memories. All processing stays on your device.",
            confirmLabel = "Grant access",
            dismissLabel = "Not now",
            onConfirm = {
                viewModel.dismissRationaleDialog()
                viewModel.onRequestPermissions()
                permissionLauncher.launch(MediaPermissions.requiredPermissions())
            },
            onDismiss = viewModel::dismissRationaleDialog,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Welcome to Recall",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Your personal photo memory assistant. " +
                "Recall searches your library using on-device AI — " +
                "your photos never leave your phone.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(32.dp))
        when (uiState.permissionState) {
            PermissionState.PERMANENTLY_DENIED -> {
                Text(
                    text = "Media access was denied. Open Settings to grant permission.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(16.dp))
                PillButton(
                    text = "Open Settings",
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        )
                        context.startActivity(intent)
                    },
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedPillButton(
                    text = "I've granted access",
                    onClick = {
                        viewModel.refreshPermissionState()
                        if (MediaPermissions.hasAllPermissions(context)) {
                            viewModel.markOnboardingComplete()
                            onOnboardingComplete()
                        }
                    },
                )
            }
            PermissionState.GRANTED -> {
                PillButton(
                    text = "Continue",
                    onClick = onOnboardingComplete,
                )
            }
            else -> {
                PillButton(
                    text = "Grant Access",
                    onClick = {
                        if (activity != null && viewModel.shouldShowRationale(activity)) {
                            viewModel.showRationaleDialog()
                        } else {
                            viewModel.onRequestPermissions()
                            permissionLauncher.launch(MediaPermissions.requiredPermissions())
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun OutlinedPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = CircleShape,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Preview
@Composable
private fun OnboardingScreenPreview() {
    RecallTheme {
        OnboardingScreen(onOnboardingComplete = {})
    }
}
