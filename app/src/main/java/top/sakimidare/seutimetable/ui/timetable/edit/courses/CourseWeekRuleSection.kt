package top.sakimidare.seutimetable.ui.timetable.edit.courses
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import top.sakimidare.seutimetable.R
import top.sakimidare.seutimetable.data.model.WeekRule
import top.sakimidare.seutimetable.ui.timetable.utils.toDisplayText


@Preview
@Composable
fun CourseWeekRuleSectionPreview() {
    CourseWeekRuleSection(
        weekRule = WeekRule.All,
        onRuleChange = {},
        onCustomClick = {}
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CourseWeekRuleSection(
    weekRule: WeekRule,
    onRuleChange: (WeekRule) -> Unit,
    onCustomClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.weekly_repetition_rules),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = weekRule.toDisplayText(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = weekRule is WeekRule.All,
                onClick = { onRuleChange(WeekRule.All) },
                label = { Text(stringResource(R.string.every_week)) }
            )
            FilterChip(
                selected = weekRule is WeekRule.Odd,
                onClick = { onRuleChange(WeekRule.Odd) },
                label = { Text(stringResource(R.string.odd_week)) }
            )
            FilterChip(
                selected = weekRule is WeekRule.Even,
                onClick = { onRuleChange(WeekRule.Even) },
                label = { Text(stringResource(R.string.even_week)) }
            )
            FilterChip(
                selected = weekRule is WeekRule.Custom,
                onClick = onCustomClick,
                label = { Text(stringResource(R.string.customize)) },
                leadingIcon = { Icon(Icons.Default.DateRange, null, Modifier.size(18.dp)) }
            )
        }
    }
}