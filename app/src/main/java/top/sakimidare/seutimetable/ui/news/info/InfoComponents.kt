package top.sakimidare.seutimetable.ui.news.info

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.sakimidare.seutimetable.R
import top.sakimidare.seutimetable.data.model.CategoryConfig
import top.sakimidare.seutimetable.viewmodels.NewsViewModel

@Composable
fun InfoContents(
    modifier: Modifier,
    pagerState: PagerState,
    scope: CoroutineScope,
    tabs: List<CategoryConfig>,
){
    Column(modifier = modifier) {
        SecondaryScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surface,
            edgePadding = 16.dp,
            divider = {},
            indicator = {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(pagerState.currentPage),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            tabs.forEachIndexed { index, category ->
                val isSelected = pagerState.currentPage == index
                Tab(
                    selected = isSelected,
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(index) }
                    },
                    text = {
                        Text(
                            text = category.label,
                            maxLines = 1, // 强制单行，触发滚动
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = if (isSelected)
                                MaterialTheme.typography.titleSmall
                            else
                                MaterialTheme.typography.bodyMedium
                        )
                    }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { pageIndex ->
            NewsList(categoryPath = tabs[pageIndex].path)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsList(
    categoryPath: String,
    viewModel: NewsViewModel = viewModel()
) {
    // 1. 列表状态与协程作用域
    val listState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    // 2. 自动触发初始加载
    LaunchedEffect(categoryPath) {
        viewModel.loadCategory(categoryPath)
    }

    // 3. 触底加载逻辑：当倒数第 2 个元素可见时，触发下一页
    val shouldLoadMore = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
            // 如果不是正在加载，且还没到底，且快滑到最后了，就加载更多
            lastVisibleItemIndex > (totalItemsNumber - 2)
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && !viewModel.isLoading && !viewModel.isEnd(categoryPath)) {
            viewModel.loadNextPage(categoryPath)
        }
    }

    // 4. 回到顶部按钮的显示逻辑：滚动超过 5 个项后显示
    val showBackToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 5 }
    }

    val currentNews = viewModel.getNewsState(categoryPath)
    val error = viewModel.getError(categoryPath)
    val uriHandler = LocalUriHandler.current
    val configuration = LocalConfiguration.current
    val errorMessagePrefix = stringResource(R.string.loading_failed_prefix)

    val columns = when {
        configuration.screenWidthDp >= 840 -> GridCells.Fixed(3)
        configuration.screenWidthDp >= 600 -> GridCells.Fixed(2)
        else -> GridCells.Fixed(1)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel.getError(categoryPath)) {
        val currentError = viewModel.getError(categoryPath)
        val latestNews = viewModel.getNewsState(categoryPath)
        if (currentError != null && latestNews.isNotEmpty()) {
            snackbarHostState.showSnackbar(
                message = "$errorMessagePrefix: $currentError",
                withDismissAction = true
            )
        }
    }

    // 使用 Box 包装以放置悬浮按钮
    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = viewModel.isRefreshing,
            onRefresh = { viewModel.loadCategory(categoryPath, isRefresh = true) },
            modifier = Modifier.fillMaxSize()
        ) {
            if (viewModel.isLoading && currentNews.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            else if (error != null && currentNews.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("😥", fontSize = 48.sp)
                    Text(
                        text = "$errorMessagePrefix: $error",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                    Button(
                        onClick = { viewModel.loadCategory(categoryPath, isRefresh = true) },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(stringResource(R.string.refresh))
                    }
                }
            } else {
                LazyVerticalGrid(
                    state = listState, // 必须绑定 state
                    modifier = Modifier.fillMaxSize(),
                    columns = columns,
                    contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp), // 底部留白给按钮
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        items = currentNews,
                        key = { it.detailUrl }
                    ) { item ->
                        InfoCard(
                            text = item.title,
                            date = item.date,
                            onClick = {
                                if (item.detailUrl.isNotEmpty()) {
                                    uriHandler.openUri(item.detailUrl)
                                }
                            }
                        )
                    }

                    // 5. 底部加载状态显示
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val currentError = viewModel.getError(categoryPath)

                            if (currentError != null && currentNews.isNotEmpty()) {
                                TextButton(
                                    onClick = { viewModel.loadNextPage(categoryPath) }
                                ) {
                                    Text(stringResource(R.string.loading_failed_click_to_retry))
                                }
                            } else if (viewModel.isEnd(categoryPath)) {
                                Text(
                                    stringResource(R.string.end_of_info),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            } else if (viewModel.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }

        // 6. 回到顶部悬浮按钮（带动画）
        AnimatedVisibility(
            visible = showBackToTop,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            FloatingActionButton(
                onClick = { scope.launch { listState.animateScrollToItem(0) } },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape
            ) {
                Icon(Icons.Default.ArrowUpward, contentDescription = stringResource(R.string.top))
            }
        }

        // 错误提示
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
        )
    }
}
@Composable
fun InfoCard(
    text: String,
    date: String = "",
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        // 增加一点动态阴影效果
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp
        ),
        border = null,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. 标题：最多显示三行，超出部分省略号
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
//                modifier = Modifier.weight(1f)
            )

            // 2. 日期：如果日期不为空，显示在右下方
            if (date.isNotEmpty()) {
                Text(
                    text = date,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.End),
                    maxLines = 1 // 确保日期不换行
                )
            }
        }
    }
}