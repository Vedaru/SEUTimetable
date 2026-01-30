package top.sakimidare.seutimetable.ui.timetable.edit.courses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import top.sakimidare.seutimetable.ui.theme.courseBackgroundColors

@Preview
@Composable
fun ColorPickerRowPreview() {
    ColorPickerRow(
        selectedIndex = 0,
        onColorSelect = {},
        presetColors = courseBackgroundColors
    )
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerRow(
    selectedIndex: Int, // 💡 改为下标
    presetColors: List<Color>,
    onColorSelect: (Int) -> Unit, // 💡 返回选中的下标
) {
    FlowRow(
        modifier = Modifier.padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        presetColors.forEachIndexed { index, color ->
            val isSelected = selectedIndex == index

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .then(
                        if (isSelected) Modifier.border(
                            2.dp,
                            MaterialTheme.colorScheme.primary,
                            CircleShape
                        ) else Modifier
                    )
                    .clip(CircleShape)
                    .clickable { onColorSelect(index) }
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 30.dp else 38.dp)
                        .clip(CircleShape)
                        .background(color),
                    contentAlignment = Alignment.Center
                ) {}
            }
        }
    }
}