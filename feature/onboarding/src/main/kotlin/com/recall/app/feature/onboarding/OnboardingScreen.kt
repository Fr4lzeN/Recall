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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
        AlertDialog(
            onDismissRequest = viewModel::dismissRationaleDialog,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                )
            },
            title = { Text("Media access needed") },
            text = {
                Text(
                    "Recall needs access to your photos and videos to search and organize " +
                        "your memories. All processing stays on your device.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissRationaleDialog()
                        viewModel.onRequestPermissions()
                        permissionLauncher.launch(MediaPermissions.requiredPermissions())
                    },
                ) {
                    Text("Grant access")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissRationaleDialog) {
                    Text("Not now")
                }
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
            modifier = Modifier.height(72.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Welcome to Recall",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
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
                Button(
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        )
                        context.startActivity(intent)
                    },
                ) {
                    Text("Open Settings")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        viewModel.refreshPermissionState()
                        if (MediaPermissions.hasAllPermissions(context)) {
                            viewModel.markOnboardingComplete()
                            onOnboardingComplete()
                        }
                    },
                ) {
                    Text("I've granted access")
                }
            }
            PermissionState.GRANTED -> {
                Button(onClick = onOnboardingComplete) {
                    Text("Continue")
                }
            }
            else -> {
                Button(
                    onClick = {
                        if (activity != null && viewModel.shouldShowRationale(activity)) {
                            viewModel.showRationaleDialog()
                        } else {
                            viewModel.onRequestPermissions()
                            permissionLauncher.launch(MediaPermissions.requiredPermissions())
                        }
                    },
                ) {
                    Text("Grant Access")
                }
            }
        }
    }
}

@Preview
@Composable
private fun OnboardingScreenPreview() {
    RecallTheme {
        OnboardingScreen(onOnboardingComplete = {})
    }
}
