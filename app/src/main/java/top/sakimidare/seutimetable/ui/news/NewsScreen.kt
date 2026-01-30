package top.sakimidare.seutimetable.ui.news

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import top.sakimidare.seutimetable.data.model.NewsConfigs
import top.sakimidare.seutimetable.ui.news.info.InfoContents

@Composable
fun NewsScreen() {
    val source = NewsConfigs.JWC
    val tabs = source.categories
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()
    InfoContents(
        modifier = Modifier,
        pagerState = pagerState,
        scope = scope,
        tabs = tabs,
    )
}