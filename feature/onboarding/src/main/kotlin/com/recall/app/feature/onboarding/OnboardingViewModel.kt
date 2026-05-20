package com.recall.app.feature.onboarding

import android.app.Activity
import android.content.Context
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class OnboardingUiState(
    val permissionState: PermissionState = PermissionState.NOT_REQUESTED,
    val shouldShowOnboarding: Boolean = true,
    val showRationaleDialog: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        refreshPermissionState()
    }

    fun refreshPermissionState() {
        val permissionState = resolvePermissionState()
        val onboardingCompleted = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        val shouldShowOnboarding = !onboardingCompleted || permissionState != PermissionState.GRANTED
        _uiState.update {
            it.copy(
                permissionState = permissionState,
                shouldShowOnboarding = shouldShowOnboarding,
            )
        }
    }

    fun onPermissionResult(
        results: Map<String, Boolean>,
        activity: Activity?,
    ) {
        val allGranted = results.values.all { it }
        if (allGranted) {
            markOnboardingComplete()
            return
        }

        val permanentlyDenied = activity != null &&
            MediaPermissions.requiredPermissions().any { permission ->
                results[permission] == false &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
            } &&
            _uiState.value.permissionState != PermissionState.NOT_REQUESTED

        val newState = when {
            permanentlyDenied -> PermissionState.PERMANENTLY_DENIED
            else -> PermissionState.DENIED
        }
        _uiState.update {
            it.copy(
                permissionState = newState,
                showRationaleDialog = newState == PermissionState.DENIED,
            )
        }
    }

    fun onRequestPermissions() {
        _uiState.update { it.copy(showRationaleDialog = false) }
    }

    fun showRationaleDialog() {
        _uiState.update { it.copy(showRationaleDialog = true) }
    }

    fun dismissRationaleDialog() {
        _uiState.update { it.copy(showRationaleDialog = false) }
    }

    fun shouldShowRationale(activity: Activity): Boolean =
        MediaPermissions.requiredPermissions().any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }

    fun markOnboardingComplete() {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
        _uiState.update {
            it.copy(
                permissionState = PermissionState.GRANTED,
                shouldShowOnboarding = false,
                showRationaleDialog = false,
            )
        }
    }

    private fun resolvePermissionState(): PermissionState {
        if (MediaPermissions.hasAllPermissions(context)) {
            return PermissionState.GRANTED
        }
        return when (_uiState.value.permissionState) {
            PermissionState.GRANTED -> PermissionState.DENIED
            else -> _uiState.value.permissionState
        }
    }

    companion object {
        private const val PREFS_NAME = "recall_onboarding"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }
}
