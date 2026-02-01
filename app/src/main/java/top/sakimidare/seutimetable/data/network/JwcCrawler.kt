package top.sakimidare.seutimetable.data.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import top.sakimidare.seutimetable.data.model.NewsItem

data class NewsPageResult(
    val newsItems: List<NewsItem>,
    val hasNextPage: Boolean
)

object JwcCrawler {
    private const val BASE_URL = "https://jwc.seu.edu.cn"
    private const val TAG = "JwcCrawler"
    suspend fun fetchNews(categoryPath: String, page: Int = 1): NewsPageResult =
        withContext(Dispatchers.IO) {
            val list = mutableListOf<NewsItem>()
            var hasNext: Boolean

            try {
                val finalPath =
                    if (page == 1) categoryPath else categoryPath.replace("list", "list$page")

                val doc = Jsoup.connect("$BASE_URL$finalPath")
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10000)
                    .get()

                // 抓取新闻列表
                val rows = doc.select("#wp_news_w8 table.main tr")
                for (row in rows) {
                    val linkElement = row.select("a[title]").first()
                    if (linkElement != null) {
                        val title = linkElement.attr("title")
                        val relativeLink = linkElement.attr("href")
                        val date = row.select("td.main div").text()

                        list.add(
                            NewsItem(
                                title = title,
                                date = date,
                                detailUrl = if (relativeLink.startsWith("http")) relativeLink else "$BASE_URL$relativeLink"
                            )
                        )
                    }
                }

                // 根据页码文字判断是否到底( 13/13 页)
                // 提取 <em class="curr_page"> 和 <em class="all_pages">
                val currPageElement = doc.select("em.curr_page").first()
                val allPagesElement = doc.select("em.all_pages").first()

                if (currPageElement != null && allPagesElement != null) {
                    val currentPage = currPageElement.text().toIntOrNull() ?: 1
                    val totalPages = allPagesElement.text().toIntOrNull() ?: 1

                    // 如果当前页小于总页数，说明还有下一页
                    hasNext = currentPage < totalPages
                } else {
                    // 如果没找到页码指示器，退而求其次判断列表是否为空
                    hasNext = list.isNotEmpty()
                }

            } catch (e: Exception) {
                Log.e(TAG, "抓取失败: ${e.message}")
                throw e
            }

            NewsPageResult(newsItems = list, hasNextPage = hasNext)
        }
}