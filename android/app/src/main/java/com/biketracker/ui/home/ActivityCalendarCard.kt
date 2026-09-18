package com.biketracker.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biketracker.data.model.Ride
import com.biketracker.ui.theme.CardBorder
import com.biketracker.ui.theme.DarkSurface
import com.biketracker.ui.theme.DarkSurfaceVariant
import com.biketracker.ui.theme.OrangeAccent
import com.biketracker.ui.theme.TealAccent
import com.biketracker.ui.theme.TextPrimary
import com.biketracker.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class CalendarDay(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val ridesCount: Int,
    val isSelected: Boolean
)

@Composable
fun ActivityCalendarCard(
    rides: List<Ride>,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var isCollapsed by remember { mutableStateOf(false) }

    // Map rides by LocalDate
    val ridesByDate = remember(rides) {
        val map = mutableMapOf<LocalDate, MutableList<Ride>>()
        for (ride in rides) {
            val localDate = ride.startTime.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            map.getOrPut(localDate) { mutableListOf() }.add(ride)
        }
        map
    }

    // Polish month-year header formatter (e.g. "Wrzesień 2026")
    val monthTitle = remember(currentMonth) {
        val formatter = DateTimeFormatter.ofPattern("LLLL yyyy", Locale("pl", "PL"))
        val raw = currentMonth.format(formatter)
        raw.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(
                Locale(
                    "pl",
                    "PL"
                )
            ) else it.toString()
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Month Name, Nav Buttons, Collapse Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title and Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isCollapsed = !isCollapsed }
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = OrangeAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = monthTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                // Month Navigation Controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Previous Month Button
                    IconButton(
                        onClick = { currentMonth = currentMonth.minusMonths(1) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Poprzedni miesiąc",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Jump to Today Button
                    Surface(
                        color = DarkSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                currentMonth = YearMonth.now()
                            }
                    ) {
                        Text(
                            text = "Dziś",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Next Month Button
                    IconButton(
                        onClick = { currentMonth = currentMonth.plusMonths(1) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Następny miesiąc",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Collapse / Expand Toggle
                    IconButton(
                        onClick = { isCollapsed = !isCollapsed },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = if (isCollapsed) "Rozwiń kalendarz" else "Zwiń kalendarz",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Expandable Calendar Content
            AnimatedVisibility(
                visible = !isCollapsed,
                enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(10.dp))

                    // Weekdays Row (Pn - Nd)
                    val weekDays = listOf("Pn", "Wt", "Śr", "Cz", "Pt", "So", "Nd")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (day in weekDays) {
                            Text(
                                text = day,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Calendar Days Grid
                    val calendarDays = remember(currentMonth, ridesByDate, selectedDate) {
                        calculateCalendarDays(currentMonth, ridesByDate, selectedDate)
                    }

                    // Group into rows of 7 days
                    val rows = calendarDays.chunked(7)
                    for (week in rows) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (day in week) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CalendarDayCell(
                                        day = day,
                                        onClick = {
                                            if (day.isSelected) {
                                                onDateSelected(null)
                                            } else {
                                                onDateSelected(day.date)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Selected Date Filter Banner
            if (selectedDate != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val dateFormatted = remember(selectedDate) {
                    val fmt = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("pl", "PL"))
                    selectedDate.format(fmt)
                }
                val ridesOnDate = ridesByDate[selectedDate]?.size ?: 0

                Surface(
                    color = OrangeAccent.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        OrangeAccent.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Filtruj: $dateFormatted",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (ridesOnDate == 1) "1 zarejestrowany trening" else "$ridesOnDate zarejestrowanych treningów",
                                color = OrangeAccent,
                                fontSize = 11.sp
                            )
                        }

                        Surface(
                            color = Color.Transparent,
                            shape = CircleShape,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onDateSelected(null) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Wyczyść filtr",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Wyczyść",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: CalendarDay,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        day.isSelected -> OrangeAccent
        day.isToday -> DarkSurfaceVariant
        else -> Color.Transparent
    }

    val textColor = when {
        day.isSelected -> Color(0xFF0B0B1A)
        day.isCurrentMonth -> TextPrimary
        else -> TextSecondary.copy(alpha = 0.3f)
    }

    val borderColor = when {
        day.isToday && !day.isSelected -> TealAccent.copy(alpha = 0.8f)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .then(
                if (borderColor != Color.Transparent) {
                    Modifier.border(1.dp, borderColor, RoundedCornerShape(8.dp))
                } else Modifier
            )
            .clickable { onClick() }
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                color = textColor,
                fontSize = 12.sp,
                fontWeight = if (day.isSelected || day.isToday) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Activity Dot
            if (day.ridesCount > 0) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (day.isSelected) Color(0xFF0B0B1A) else OrangeAccent)
                )
            } else {
                Spacer(modifier = Modifier.size(4.dp))
            }
        }
    }
}

private fun calculateCalendarDays(
    yearMonth: YearMonth,
    ridesByDate: Map<LocalDate, List<Ride>>,
    selectedDate: LocalDate?
): List<CalendarDay> {
    val days = mutableListOf<CalendarDay>()
    val today = LocalDate.now()

    val firstOfMonth = yearMonth.atDay(1)
    // Day of week (1 = Monday, 7 = Sunday)
    val dayOfWeekValue = firstOfMonth.dayOfWeek.value
    val leadingDays = dayOfWeekValue - 1

    // Previous month filler days
    val prevMonth = yearMonth.minusMonths(1)
    val daysInPrevMonth = prevMonth.lengthOfMonth()
    for (i in leadingDays - 1 downTo 0) {
        val date = prevMonth.atDay(daysInPrevMonth - i)
        val ridesCount = ridesByDate[date]?.size ?: 0
        days.add(
            CalendarDay(
                date = date,
                isCurrentMonth = false,
                isToday = date == today,
                ridesCount = ridesCount,
                isSelected = date == selectedDate
            )
        )
    }

    // Current month days
    val daysInMonth = yearMonth.lengthOfMonth()
    for (day in 1..daysInMonth) {
        val date = yearMonth.atDay(day)
        val ridesCount = ridesByDate[date]?.size ?: 0
        days.add(
            CalendarDay(
                date = date,
                isCurrentMonth = true,
                isToday = date == today,
                ridesCount = ridesCount,
                isSelected = date == selectedDate
            )
        )
    }

    // Trailing days from next month to complete the week rows
    val totalCells = if (days.size <= 28) 28 else if (days.size <= 35) 35 else 42
    val trailingDays = totalCells - days.size
    val nextMonth = yearMonth.plusMonths(1)
    for (day in 1..trailingDays) {
        val date = nextMonth.atDay(day)
        val ridesCount = ridesByDate[date]?.size ?: 0
        days.add(
            CalendarDay(
                date = date,
                isCurrentMonth = false,
                isToday = date == today,
                ridesCount = ridesCount,
                isSelected = date == selectedDate
            )
        )
    }

    return days
}
