package com.parkmate.app.feature.details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.parkmate.app.R
import com.parkmate.app.core.ui.components.ParkMatePrimaryButton
import com.parkmate.app.core.ui.components.ParkMateTopBar
import com.parkmate.app.core.ui.theme.ParkMateThemeTokens
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddDetailsScreen(
    spotId: Long,
    viewModel: SpotDetailsViewModel,
    onBack: () -> Unit,
    onAddPhoto: () -> Unit,
    onSaved: () -> Unit
) {
    LaunchedEffect(spotId) {
        viewModel.loadSpot(spotId)
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = ParkMateThemeTokens.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        ParkMateTopBar(
            title = stringResource(R.string.add_details_title),
            eyebrow = stringResource(R.string.add_details_eyebrow),
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // Photos Section Heading
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = stringResource(R.string.details_photos_heading),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.text
                )
                Text(
                    text = stringResource(R.string.details_photos_count, state.draftPhotos.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted
                )
            }

            // Horizontal Photo Strip (up to 4 photos + Add tile)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                state.draftPhotos.forEach { path ->
                    Box(
                        modifier = Modifier
                            .width(78.dp)
                            .height(82.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.line)
                    ) {
                        AsyncImage(
                            model = File(path),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { viewModel.removePhoto(path) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x9914161A))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.cd_remove_photo),
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                if (state.draftPhotos.size < 4) {
                    Box(
                        modifier = Modifier
                            .width(78.dp)
                            .height(82.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = 1.5.dp,
                                color = colors.muted.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .background(colors.surface)
                            .clickable(onClick = onAddPhoto),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = colors.blue,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.details_add_photo_btn),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.blue
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Quick Tags Heading
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = stringResource(R.string.details_quick_tags_heading),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.text
                )
                Text(
                    text = stringResource(R.string.details_quick_tags_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SpotDetailsViewModel.AVAILABLE_QUICK_TAGS.forEach { tag ->
                    val isSelected = state.draftTags.contains(tag)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) colors.blueSoft else colors.surface,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) colors.blue.copy(alpha = 0.45f) else colors.line
                        ),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.toggleQuickTag(tag) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp)
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = colors.blue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = localizeQuickTag(tag),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) colors.blue else colors.text
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Note Section Heading
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = stringResource(R.string.details_note_heading),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.text
                )
                Text(
                    text = stringResource(R.string.details_note_counter, state.draftNote.length),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted
                )
            }

            OutlinedTextField(
                value = state.draftNote,
                onValueChange = { viewModel.updateDraftNote(it) },
                placeholder = {
                    Text(
                        text = stringResource(R.string.details_note_placeholder),
                        color = colors.muted
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedBorderColor = colors.blue,
                    unfocusedBorderColor = colors.line,
                    focusedTextColor = colors.text,
                    unfocusedTextColor = colors.text
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Sticky Save Bottom Bar
        HorizontalDivider(color = colors.line)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bg.copy(alpha = 0.95f))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            ParkMatePrimaryButton(
                text = stringResource(R.string.action_save),
                onClick = { viewModel.saveDraftDetails(onSaved) },
                modifier = Modifier.widthIn(max = 520.dp)
            )
        }
    }
}

@Composable
fun localizeQuickTag(rawTag: String): String {
    return when (rawTag) {
        "Level -1" -> stringResource(R.string.tag_level_minus_1)
        "Level -2" -> stringResource(R.string.tag_level_minus_2)
        "Zone A" -> stringResource(R.string.tag_zone_a)
        "Zone B" -> stringResource(R.string.tag_zone_b)
        "Row 14" -> stringResource(R.string.tag_row_14)
        "Pillar C4" -> stringResource(R.string.tag_pillar_c4)
        "Near elevator" -> stringResource(R.string.tag_near_elevator)
        "Near exit" -> stringResource(R.string.tag_near_exit)
        else -> rawTag
    }
}
