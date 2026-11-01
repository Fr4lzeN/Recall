package com.recall.app.core.common.diagnostics

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

@Singleton
class SearchDiagnostics @Inject constructor(@ApplicationContext context: Context) {
    private val preferences = context.getSharedPreferences("search_diagnostics", Context.MODE_PRIVATE)
    private val defaultEnabled = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    private val mutableEnabled = MutableStateFlow(preferences.getBoolean("enabled", defaultEnabled))
    val enabled: StateFlow<Boolean> = mutableEnabled.asStateFlow()
    private val store = SearchTraceStore(File(context.filesDir, "search_diagnostics"))
    private val writerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private sealed interface WriteCommand {
        data class Capture(val build: suspend () -> SearchDiagnosticRecord) : WriteCommand
        data class Barrier(val completed: CompletableDeferred<Unit>) : WriteCommand
    }
    private val commands = Channel<WriteCommand>(capacity = 4)

    init {
        writerScope.launch {
            for (command in commands) {
                when (command) {
                    is WriteCommand.Capture -> try {
                        if (enabled.value) record(command.build())
                    } catch (error: CancellationException) {
                        if (!currentCoroutineContext().isActive) throw error
                        Log.w(TAG, "Diagnostic capture was cancelled")
                    } catch (error: Exception) {
                        Log.w(TAG, "Could not capture search diagnostics: ${error.javaClass.simpleName}")
                    }
                    is WriteCommand.Barrier -> command.completed.complete(Unit)
                }
            }
        }
    }

    fun enqueue(buildRecord: suspend () -> SearchDiagnosticRecord) {
        if (enabled.value && !commands.trySend(WriteCommand.Capture(buildRecord)).isSuccess) {
            Log.w(TAG, "Diagnostic queue is full; trace was skipped")
        }
    }

    private suspend fun flushPending() {
        val completed = CompletableDeferred<Unit>()
        commands.send(WriteCommand.Barrier(completed))
        completed.await()
    }

    fun setEnabled(value: Boolean) {
        preferences.edit().putBoolean("enabled", value).apply()
        mutableEnabled.value = value
    }

    suspend fun record(record: SearchDiagnosticRecord): String? = withContext(Dispatchers.IO) {
        if (!enabled.value) return@withContext null
        try {
            store.write(record).also { file ->
                if (file != null) {
                    // Full text, filenames and vectors stay in app-private files, not Logcat.
                    Log.d(TAG, "trace=${record.traceId} status=${record.status} shown=${record.displayedIds.size} file=$file")
                } else {
                    Log.w(TAG, "Diagnostic trace exceeded storage limits")
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w(TAG, "Could not save diagnostic trace: ${error.javaClass.simpleName}")
            null
        }
    }

    suspend fun exportTo(output: OutputStream) {
        flushPending()
        withContext(Dispatchers.IO) { store.exportTo(output) }
    }

    suspend fun clear() {
        flushPending()
        withContext(Dispatchers.IO) { store.clear() }
    }

    companion object {
        const val MAX_CANDIDATES = 200
        private const val TAG = "RecallSearch"
    }
}
