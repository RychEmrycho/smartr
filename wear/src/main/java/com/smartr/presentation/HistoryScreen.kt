package com.smartr.presentation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.*

import com.smartr.R
import androidx.compose.ui.res.stringResource

@Composable
fun HistoryScreen(
    viewModel: DashboardViewModel = viewModel()
) {
    val summaries by viewModel.summaries.collectAsState()
    val listState = rememberScalingLazyListState()
    
    ScreenScaffold(scrollState = listState, timeText = { TimeText() }) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(top = 32.dp, start = 16.dp, end = 16.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item(key = "header") { 
                ListHeader { Text(stringResource(R.string.history_title), style = MaterialTheme.typography.titleMedium) } 
            }
            
            if (summaries.isEmpty()) {
                item(key = "empty") { 
                    Text(stringResource(R.string.history_empty), style = MaterialTheme.typography.bodyMedium) 
                }
            }

            items(
                items = summaries.reversed().take(10),
                key = { it.dateIso }
            ) { summary ->
                TitleCard(
                    onClick = { },
                    title = { Text(summary.dateIso) },
                    subtitle = { Text(stringResource(R.string.history_item_sitting_format, summary.sedentaryMinutes)) }
                ) {
                    Text(
                        stringResource(R.string.history_item_reminders_format, summary.remindersSent), 
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            item(key = "footer") {
                Text(
                    stringResource(R.string.history_footer),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
