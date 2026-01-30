package top.sakimidare.seutimetable.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import top.sakimidare.seutimetable.R

/**
 * 💡 标准免责声明对话框
 * @param onConfirm 用户点击同意
 * @param onTerminate 用户点击拒绝（通常关联到 activity.finish()）
 */
@Composable
fun DisclaimerDialog(
    onConfirm: () -> Unit,
    onTerminate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* 强制用户必须交互，不允许点击外部关闭 */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        title = {
            Text(
                text = stringResource(R.string.disclaimer_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            // 💡 增加垂直滚动，确保在小屏手机上能看全长篇声明
            val scrollState = rememberScrollState()
            Text(
                text = stringResource(R.string.disclaimer_content),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(vertical = 8.dp)
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.accept_and_continue),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onTerminate) {
                Text(
                    text = stringResource(R.string.decline_and_exit),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    )
}