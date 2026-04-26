package com.smartr.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.*
import com.smartr.R

@Composable
fun VitalityInfoScreen() {
    val listState = rememberScalingLazyListState()

    ScreenScaffold(scrollState = listState, timeText = { TimeText() }) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(top = 32.dp, start = 16.dp, end = 16.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                ListHeader {
                    Text(
                        stringResource(R.string.vitality_info_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            item {
                TitleCard(
                    onClick = { },
                    title = { Text(stringResource(R.string.vitality_info_wellness_title)) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Text(
                        stringResource(R.string.vitality_info_wellness_desc),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            item {
                TitleCard(
                    onClick = { },
                    title = { Text(stringResource(R.string.vitality_info_level_title)) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column {
                        Text(
                            stringResource(R.string.vitality_info_level_desc),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.vitality_info_xp_rates),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item {
                TitleCard(
                    onClick = { },
                    title = { Text(stringResource(R.string.vitality_info_ranks_title)) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Text(
                        stringResource(R.string.vitality_info_ranks_desc),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
