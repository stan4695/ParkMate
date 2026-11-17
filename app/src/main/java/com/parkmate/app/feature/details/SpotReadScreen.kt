package com.parkmate.app.feature.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.parkmate.app.R
import com.parkmate.app.core.ui.components.MiniMapThumbnail
import com.parkmate.app.core.ui.components.ParkMateOutlineButton
import com.parkmate.app.core.ui.components.ParkMatePrimaryButton
import com.parkmate.app.core.ui.components.ParkMateSettingRow
import com.parkmate.app.core.ui.components.ParkMateTopBar
import com.parkmate.app.core.ui.components.RealMapThumbnail
import com.parkmate.app.core.ui.components.StatusPillBadge
import com.parkmate.app.core.ui.theme.ParkMateThemeTokens
import com.parkmate.app.feature.home.formatParkedAgo
import java.io.File

/**
 * Matches both the Figma `Details` screen (when accessed from the Home map summary card)
 * and the `SpotRead` screen (with photo pager / NoPhotoCard, overflow menu Edit/Delete, note, mini-map, timer, and Guide Me).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpotReadScreen(
    spotId: Long,
    viewModel: SpotDetailsViewModel,
    onBack: () -> Unit,
    onOpenPhotoViewer: (Int) -> Unit,
    onAddPhoto: () -> Unit,
    onEditSpot: () -> Unit,
    onGuideMe: () -> Unit,
    onDeleted: () -> Unit,
    showInlineEditableNoteAndTimer: Boolean = false
) {
    LaunchedEffect(spotId) {
        viewModel.loadSpot(spotId)
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = ParkMateThemeTokens.colors
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val spot = state.spot

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.spot_delete_confirm_title)) },
            text = { Text(stringResource(R.string.spot_delete_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteCurrentSpot(onDeleted)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = colors.danger,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        ParkMateTopBar(
            title = stringResource(R.string.spot_details_title),
            eyebrow = if (spot?.isActive == true) {
                stringResource(R.string.spot_details_eyebrow)
            } else {
                stringResource(R.string.spot_details_history_eyebrow)
            },
            onBack = onBack,
            action = {
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.cd_more_options),
                            tint = colors.text
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_edit)) },
                            leadingIcon = {
                                Icon(Icons.Rounded.Edit, contentDescription = null)
                            },
                            onClick = {
                                menuExpanded = false
                                onEditSpot()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.action_delete),
                                    color = colors.danger
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = null,
                                    tint = colors.danger
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                showDeleteConfirm = true
                            }
                        )
                    }
                }
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            val photos = spot?.photoPaths.orEmpty()

            if (showInlineEditableNoteAndTimer && photos.isEmpty()) {
                // Figma `Details` top photo card with map artwork & "Parking entrance" badge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(128.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onAddPhoto)
                ) {
                if (spot != null) {
                    RealMapThumbnail(
                        latitude = spot.latitude,
                        longitude = spot.longitude,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    MiniMapThumbnail(
                        modifier = Modifier.fillMaxSize(),
                        showPin = true
                    )
                }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.surface.copy(alpha = 0.92f),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PhotoCamera,
                                contentDescription = null,
                                tint = colors.text,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.spot_photo_entrance_badge),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.text
                            )
                        }
                    }
                }
            } else if (photos.isNotEmpty()) {
                val pagerState = rememberPagerState(pageCount = { photos.size })
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.line)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        AsyncImage(
                            model = File(photos[page]),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onOpenPhotoViewer(page) }
                        )
                    }
                    // Pager dots matching .pager-dots in index.css
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0x6114161A),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 11.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(photos.size) { index ->
                                val active = pagerState.currentPage == index
                                Box(
                                    modifier = Modifier
                                        .width(if (active) 12.dp else 5.dp)
                                        .height(5.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (active) Color.White else Color.White.copy(alpha = 0.55f)
                                        )
                                )
                            }
                        }
                    }
                }
            } else {
                // NoPhotoCard matching .no-photo-card in index.css
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.line.copy(alpha = 0.7f))
                        .padding(horizontal = 28.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoCamera,
                        contentDescription = null,
                        tint = colors.muted,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.spot_no_photo_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.text,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    ParkMateOutlineButton(
                        text = stringResource(R.string.quick_save_add_photo),
                        icon = Icons.Rounded.AddAPhoto,
                        onClick = onAddPhoto
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Heading + Status Dot
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = spot?.addressLine.orEmpty(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.text
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val elapsedMins = spot?.let {
                        ((System.currentTimeMillis() - it.savedAtMillis) / 60_000L).coerceAtLeast(0L)
                    } ?: 0L
                    val subtitle = if (showInlineEditableNoteAndTimer) {
                        spot?.cityDistrict.orEmpty()
                    } else {
                        "${spot?.cityDistrict.orEmpty()} • ${formatParkedAgo(elapsedMins)}"
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted
                    )
                }
                StatusPillBadge(
                    text = if (spot?.isActive == true) {
                        stringResource(R.string.spot_status_active)
                    } else {
                        stringResource(R.string.spot_status_past)
                    }
                )
            }

            // Quick tag chips
            val tags = spot?.tags.orEmpty()
            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(11.dp))
                                .background(colors.blueSoft)
                                .padding(horizontal = 11.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = localizeQuickTag(tag),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.blue
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (showInlineEditableNoteAndTimer) {
                // Inline editable note field matching Details in App.tsx
                Text(
                    text = stringResource(R.string.spot_parking_note_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.text,
                    modifier = Modifier.padding(bottom = 7.dp)
                )
                OutlinedTextField(
                    value = state.draftNote,
                    onValueChange = { viewModel.saveInlineNoteNow(it) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Notes,
                            contentDescription = null,
                            tint = colors.muted
                        )
                    },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.details_note_placeholder),
                            color = colors.muted
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.surface,
                        unfocusedContainerColor = colors.surface,
                        focusedBorderColor = colors.blue,
                        unfocusedBorderColor = colors.line,
                        focusedTextColor = colors.text,
                        unfocusedTextColor = colors.text,
                        cursorColor = colors.blue
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Read-mode Note Card matching .read-note in index.css
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditSpot() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Notes,
                            contentDescription = null,
                            tint = colors.blue,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(11.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.spot_note_eyebrow),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.muted
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = spot?.note?.takeIf { it.isNotBlank() }
                                    ?: stringResource(R.string.spot_no_note),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.text
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mini-Map Card matching .mini-map-card in index.css
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onGuideMe)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (spot != null) {
                            RealMapThumbnail(
                                latitude = spot.latitude,
                                longitude = spot.longitude,
                                modifier = Modifier
                                    .width(70.dp)
                                    .height(66.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        } else {
                            MiniMapThumbnail(
                                modifier = Modifier
                                    .width(70.dp)
                                    .height(66.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                showPin = true
                            )
                        }
                        Spacer(modifier = Modifier.width(11.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.spot_your_car_eyebrow),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.muted
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (state.distanceMeters < 1000) {
                                    stringResource(R.string.home_distance_away_meters, state.distanceMeters)
                                } else {
                                    stringResource(R.string.home_distance_away_km, state.distanceMeters / 1000f)
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.text
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.home_about_walk_minutes, state.walkMinutes),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.muted
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = colors.muted
                        )
                    }
                }
            }

            // Paid Parking Timer Card + Remind Me Toggle
            Spacer(modifier = Modifier.height(16.dp))
            PaidParkingTimerCard(
                timerState = state.timerState,
                onAdd15Minutes = { viewModel.addTimerMinutes(15) },
                onAdd30Minutes = { viewModel.addTimerMinutes(30) },
                onResetTimer = { viewModel.resetTimer() }
            )

            Spacer(modifier = Modifier.height(8.dp))
            ParkMateSettingRow(
                icon = Icons.Rounded.Notifications,
                title = stringResource(R.string.timer_remind_me),
                subtitle = stringResource(R.string.timer_remind_me_sub),
                checked = state.remindTenMinutesBefore,
                onCheckedChange = { viewModel.toggleRemindBeforeExpiry(it) }
            )

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Sticky Guide Me CTA at bottom when in SpotRead mode
        if (!showInlineEditableNoteAndTimer) {
            HorizontalDivider(color = colors.line)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bg.copy(alpha = 0.95f))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                ParkMatePrimaryButton(
                    text = stringResource(R.string.home_guide_me),
                    icon = Icons.AutoMirrored.Rounded.DirectionsWalk,
                    onClick = onGuideMe,
                    modifier = Modifier.widthIn(max = 520.dp)
                )
            }
        }
    }
}
