package com.autocalendar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.autocalendar.data.toDraft
import com.autocalendar.ui.AppNav
import com.autocalendar.ui.confirm.ConfirmViewModel
import com.autocalendar.ui.createConfirmViewModel
import com.autocalendar.ui.createHistoryViewModel
import com.autocalendar.ui.createMainViewModel
import com.autocalendar.ui.history.HistoryViewModel
import com.autocalendar.ui.main.MainViewModel

class MainActivity : ComponentActivity() {

    private val container: AppContainer by lazy { (application as AutoCalendarApp).container }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleSharedText(intent)

        setContent {
            MaterialTheme {
                AppNav(
                    pendingSharedText = container.pendingSharedText,
                    confirmRequest = container.confirmRequest,
                    createMain = ::buildMainViewModel,
                    createConfirm = ::buildConfirmViewModel,
                    createHistory = ::buildHistoryViewModel,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSharedText(intent)
    }

    private fun handleSharedText(intent: Intent?) {
        val shared = intent?.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
        if (shared.isEmpty()) return
        container.pendingSharedText.value = shared
    }

    private fun buildMainViewModel(): MainViewModel =
        createMainViewModel(
            parser = container.parser,
            onDraftReady = { draft, rawText ->
                container.confirmRequest.value = ConfirmRequest(draft, rawText)
            },
        )

    private fun buildConfirmViewModel(request: ConfirmRequest): ConfirmViewModel =
        createConfirmViewModel(
            container = container,
            request = request,
            onDone = {
                container.confirmRequest.value = null
                finish()
            },
        )

    private fun buildHistoryViewModel(): HistoryViewModel =
        createHistoryViewModel(
            container = container,
            onSelect = { item ->
                container.confirmRequest.value = ConfirmRequest(item.toDraft(), item.rawText)
            },
        )
}
