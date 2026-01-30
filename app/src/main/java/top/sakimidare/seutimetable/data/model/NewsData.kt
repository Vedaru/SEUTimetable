package top.sakimidare.seutimetable.data.model

// 每条资讯的 URL
data class NewsItem(
    val title: String,
    val date: String,
    val detailUrl: String
)
// 定义资讯来源（如：教务处、总务处）
data class NewsSource(
    val sourceName: String,
    val categories: List<CategoryConfig>
)

// 定义具体的 Tab 分类
data class CategoryConfig(
    val label: String,      // Tab 显示的文字，如 "最新动态"
    val path: String        // 对应的网页路径，如 "/zxdt/list.htm"
)
// 配置
object NewsConfigs {
    val JWC = NewsSource(
        sourceName = "教务处",
        categories = listOf(
            CategoryConfig("最新动态", "/zxdt/list.htm"),
            CategoryConfig("教务信息", "/jwxx/list.htm"),
            CategoryConfig("学籍管理", "/xjgl/list.htm"),
            CategoryConfig("教学研究", "/jxyj/list.htm"),
            CategoryConfig("实践教学", "/sjjx/list.htm"),
            CategoryConfig("国际交流", "/gjjl/list.psp"),
            CategoryConfig("文化素质教育", "/cbxx/list.htm"),

        )
    )
}