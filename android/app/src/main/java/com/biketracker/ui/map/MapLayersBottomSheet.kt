package com.biketracker.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biketracker.ui.theme.CardBorder
import com.biketracker.ui.theme.DarkBackground
import com.biketracker.ui.theme.DarkSurfaceVariant
import com.biketracker.ui.theme.OrangeAccent
import com.biketracker.ui.theme.SuccessGreen
import com.biketracker.ui.theme.TealAccent
import com.biketracker.ui.theme.TextPrimary
import com.biketracker.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapLayersBottomSheet(
    selectedBaseSource: MapBaseSource,
    activeOverlays: Set<MapOverlaySource>,
    onSelectBaseSource: (MapBaseSource) -> Unit,
    onToggleOverlay: (MapOverlaySource, Boolean) -> Unit,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = DarkBackground,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(TextSecondary.copy(alpha = 0.35f), RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(OrangeAccent.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                        .border(1.dp, OrangeAccent.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = OrangeAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Źródło mapy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Wybierz styl mapy i nakładki szlaków",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            HorizontalDivider(color = CardBorder.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 12.dp))

            // Section 1: Base Map Source
            Text(
                text = "PODKŁAD MAPY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            MapBaseSource.entries.forEach { baseSource ->
                val isSelected = baseSource == selectedBaseSource
                BaseMapOptionRow(
                    baseSource = baseSource,
                    isSelected = isSelected,
                    onClick = { onSelectBaseSource(baseSource) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = CardBorder.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 12.dp))

            // Section 2: Outdoor Trail Overlays
            Text(
                text = "SZLAKI TURYSTYCZNE (NAKŁADKI)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            MapOverlaySource.entries.forEach { overlaySource ->
                val isChecked = activeOverlays.contains(overlaySource)
                OverlayOptionRow(
                    overlaySource = overlaySource,
                    isChecked = isChecked,
                    onCheckedChange = { checked -> onToggleOverlay(overlaySource, checked) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BaseMapOptionRow(
    baseSource: MapBaseSource,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = OrangeAccent,
                unselectedColor = TextSecondary.copy(alpha = 0.6f)
            ),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = baseSource.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = TextPrimary
                )

                BadgePill(badge = baseSource.badge, baseSource = baseSource)
            }

            Text(
                text = baseSource.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun OverlayOptionRow(
    overlaySource: MapOverlaySource,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onCheckedChange(!isChecked) }
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = TealAccent,
                checkmarkColor = DarkBackground,
                uncheckedColor = TextSecondary.copy(alpha = 0.6f)
            ),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = overlaySource.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
                    color = TextPrimary
                )

                BadgePill(badge = overlaySource.badge, isOverlay = true)
            }

            Text(
                text = overlaySource.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun BadgePill(
    badge: String,
    baseSource: MapBaseSource? = null,
    isOverlay: Boolean = false
) {
    val (bgColor, borderColor, textColor) = when {
        isOverlay -> Triple(
            TealAccent.copy(alpha = 0.15f),
            TealAccent.copy(alpha = 0.4f),
            TealAccent
        )

        baseSource == MapBaseSource.CYCLOSM -> Triple(
            TealAccent.copy(alpha = 0.15f),
            TealAccent.copy(alpha = 0.4f),
            TealAccent
        )

        baseSource == MapBaseSource.OPEN_TOPO_MAP -> Triple(
            SuccessGreen.copy(alpha = 0.15f),
            SuccessGreen.copy(alpha = 0.4f),
            SuccessGreen
        )

        baseSource == MapBaseSource.UMP_PCPL -> Triple(
            OrangeAccent.copy(alpha = 0.15f),
            OrangeAccent.copy(alpha = 0.4f),
            OrangeAccent
        )

        else -> Triple(
            DarkSurfaceVariant,
            CardBorder,
            TextSecondary
        )
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = badge,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
