package top.sakimidare.seutimetable.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import top.sakimidare.seutimetable.data.model.NewsItem
import top.sakimidare.seutimetable.data.network.JwcCrawler


class NewsViewModel : ViewModel() {

    private val newsCache = mutableStateMapOf<String, List<NewsItem>>()
    private val pageMap = mutableMapOf<String, Int>()
    private val isEndMap = mutableStateMapOf<String, Boolean>()

    var isLoading by mutableStateOf(false)
        private set

    var isRefreshing by mutableStateOf(false)
        private set

    fun getNewsState(path: String): List<NewsItem> = newsCache[path] ?: emptyList()

    fun isEnd(path: String): Boolean = isEndMap[path] ?: false

    fun loadCategory(path: String, isRefresh: Boolean = false) {
        if (!isRefresh && newsCache.containsKey(path)) return
        executeLoad(path, 1, isRefresh)
    }

    fun loadNextPage(path: String) {
        // 关键：增加 isLoading 拦截，防止重复请求同一页
        if (isLoading || isRefreshing || isEnd(path)) return

        val nextPage = (pageMap[path] ?: 1) + 1
        executeLoad(path, nextPage, isRefresh = false)
    }

    private fun executeLoad(path: String, page: Int, isRefresh: Boolean) {
        viewModelScope.launch {
            if (isRefresh) isRefreshing = true else isLoading = true

            try {
                // 1. 获取封装后的结果对象
                val result = JwcCrawler.fetchNews(path, page)
                val newData = result.newsItems

                // 2. 使用爬虫解析出的 hasNextPage 来更新结尾状态
                // 只有当 hasNextPage 为 false 时，才标记为 End
                isEndMap[path] = !result.hasNextPage

                if (isRefresh || page == 1) {
                    newsCache[path] = newData
                    pageMap[path] = 1
                    // 刷新时，如果数据不为空且爬虫说还有下一页，确保重置 End 状态
                    if (newData.isNotEmpty() && result.hasNextPage) {
                        isEndMap[path] = false
                    }
                } else {
                    if (newData.isNotEmpty()) {
                        val currentList = newsCache[path] ?: emptyList()
                        // 关键点：将新老数据合并后，根据 detailUrl 进行去重
                        val combinedList = (currentList + newData).distinctBy { it.detailUrl }

                        newsCache[path] = combinedList
                        pageMap[path] = page
                    }
                }

                // 额外保险：如果数据确实为空，强制标记为结束
                if (newData.isEmpty()) isEndMap[path] = true

            } catch (e: Exception) {
                e.printStackTrace()
                // 网络异常时，如果是加载下一页失败，建议标记为 End 防止无限重试
                if (page > 1) isEndMap[path] = true
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }
}