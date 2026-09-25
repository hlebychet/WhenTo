package com.autocalendar.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.autocalendar.AppContainer
import com.autocalendar.ConfirmRequest
import com.autocalendar.data.NewParsedMeeting
import com.autocalendar.data.ParsedMeeting
import com.autocalendar.domain.MeetingDraft
import com.autocalendar.parser.MeetingParser
import com.autocalendar.parser.MeetingTextValidator
import com.autocalendar.ui.confirm.ConfirmScreen
import com.autocalendar.ui.confirm.ConfirmViewModel
import com.autocalendar.ui.history.HistoryScreen
import com.autocalendar.ui.history.HistoryViewModel
import com.autocalendar.ui.main.MainScreen
import com.autocalendar.ui.main.MainViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

object Routes {
    const val MAIN = "main"
    const val CONFIRM = "confirm"
    const val HISTORY = "history"
}

@Composable
fun AppNav(
    pendingSharedText: MutableStateFlow<String?>,
    confirmRequest: MutableStateFlow<ConfirmRequest?>,
    createMain: () -> MainViewModel,
    createConfirm: (ConfirmRequest) -> ConfirmViewModel,
    createHistory: () -> HistoryViewModel,
) {
    val nav = rememberNavController()
    val request by confirmRequest.collectAsState()

    LaunchedEffect(request) {
        if (request != null && nav.currentDestination?.route != Routes.CONFIRM) {
            nav.navigate(Routes.CONFIRM)
        }
    }

    NavHost(navController = nav, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) {
            val viewModel = remember { createMain() }
            val pending by pendingSharedText.collectAsState()
            LaunchedEffect(pending) {
                val shared = pending
                if (shared != null) {
                    viewModel.onTextChange(shared)
                    pendingSharedText.value = null
                }
            }
            MainScreen(
                viewModel = viewModel,
                onOpenHistory = { nav.navigate(Routes.HISTORY) },
            )
        }
        composable(Routes.CONFIRM) {
            val current = request
            if (current != null) {
                val viewModel = remember(current) { createConfirm(current) }
                BackHandler {
                    confirmRequest.value = null
                    nav.popBackStack()
                }
                ConfirmScreen(
                    viewModel = viewModel,
                    onCancel = {
                        confirmRequest.value = null
                        nav.popBackStack()
                    },
                )
            } else {
                LaunchedEffect(Unit) { nav.popBackStack() }
            }
        }
        composable(Routes.HISTORY) {
            val viewModel = remember { createHistory() }
            HistoryScreen(
                viewModel = viewModel,
                onBack = { nav.popBackStack() },
            )
        }
    }
}

fun createMainViewModel(
    parser: MeetingParser,
    onDraftReady: (MeetingDraft, String) -> Unit,
): MainViewModel = MainViewModel(parser, { MeetingTextValidator.validate(it) }, onDraftReady)

fun createConfirmViewModel(
    container: AppContainer,
    request: ConfirmRequest,
    onDone: () -> Unit,
): ConfirmViewModel {
    val viewModel = ConfirmViewModel(
        launcher = container.launcher,
        onEventSaved = { event ->
            container.appScope.launch {
                container.store.add(
                    NewParsedMeeting(
                        rawText = request.rawText,
                        title = event.title,
                        startMillis = event.beginMillis,
                        endMillis = event.endMillis,
                        location = event.location,
                    ),
                )
            }
            onDone()
        },
    )
    viewModel.onDraftReady(request.draft, request.rawText)
    return viewModel
}

fun createHistoryViewModel(
    container: AppContainer,
    onSelect: (ParsedMeeting) -> Unit,
): HistoryViewModel = HistoryViewModel(container.store, onSelect)
