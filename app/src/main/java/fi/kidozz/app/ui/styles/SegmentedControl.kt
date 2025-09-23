package fi.kidozz.app.ui.styles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A segmented control component that mimics the style from the provided image.
 * Features a pill-shaped container with rounded ends and mutually exclusive selection.
 */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelectionChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    selectedBackgroundColor: Color = Color.White,
    unselectedBackgroundColor: Color = Color(0xFFF5F5F5),
    textColor: Color = Color(0xFF333333),
    selectedTextColor: Color = Color(0xFF333333)
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(unselectedBackgroundColor)
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = index == selectedIndex
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(
                            when {
                                index == 0 -> RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
                                index == options.size - 1 -> RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)
                                else -> RoundedCornerShape(0.dp)
                            }
                        )
                        .background(
                            if (isSelected) selectedBackgroundColor else Color.Transparent
                        )
                        .clickable { onSelectionChange(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                        ),
                        color = if (isSelected) selectedTextColor else textColor
                    )
                }
            }
        }
    }
}

/**
 * A specialized segmented control for attendance status with predefined colors.
 */
@Composable
fun AttendanceSegmentedControl(
    selectedAttendance: String,
    onAttendanceChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf("In Care", "Out", "Sick")
    val selectedIndex = when (selectedAttendance.lowercase()) {
        "in-care" -> 0
        "out" -> 1
        "sick" -> 2
        else -> 1 // Default to "Out"
    }
    
    // Define colors for each attendance status
    val statusColors = mapOf(
        "in-care" to Color(0xFF4CAF50), // Green
        "out" to Color(0xFF9E9E9E),     // Gray
        "sick" to Color(0xFFF44336)     // Red
    )
    
    val selectedColor = statusColors[selectedAttendance] ?: Color(0xFF9E9E9E)
    
    SegmentedControl(
        options = options,
        selectedIndex = selectedIndex,
        onSelectionChange = { index ->
            val attendance = when (index) {
                0 -> "in-care"
                1 -> "out"
                2 -> "sick"
                else -> "out"
            }
            onAttendanceChange(attendance)
        },
        modifier = modifier,
        selectedBackgroundColor = selectedColor.copy(alpha = 0.2f),
        unselectedBackgroundColor = Color(0xFFF5F5F5),
        textColor = Color(0xFF666666),
        selectedTextColor = selectedColor
    )
}
