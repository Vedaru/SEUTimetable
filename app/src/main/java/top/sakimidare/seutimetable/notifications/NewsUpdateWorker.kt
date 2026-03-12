package top.sakimidare.seutimetable.notifications

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import top.sakimidare.seutimetable.data.model.NewsConfigs
import top.sakimidare.seutimetable.data.network.JwcCrawler

class NewsUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): ListenableWorker.Result {
        // check user preference; if disabled just succeed
        val prefs = applicationContext
        val repo = top.sakimidare.seutimetable.data.repository.UserPreferenceRepository(prefs)
        val enabled = repo.newsNotificationFlow.first()
        if (!enabled) return ListenableWorker.Result.success()

        try {
            val newsSource = NewsConfigs.JWC
            var hasNew = false
            var latestTitle: String? = null

            for (cat in newsSource.categories) {
                // only fetch first page
                val result = JwcCrawler.fetchNews(cat.path, 1)
                val seen = NewsNotificationStore.getSeenUrls(prefs, cat.path)
                val freshItems = result.newsItems.filter { it.detailUrl !in seen }
                if (freshItems.isNotEmpty()) {
                    // take the first one as headline
                    if (!hasNew) {
                        latestTitle = freshItems.first().title
                    }
                    hasNew = true
                }
                // update store to current list (limit to 20 to avoid excessive storage)
                val toSave = result.newsItems.take(20).map { it.detailUrl }.toSet()
                NewsNotificationStore.saveSeenUrls(prefs, cat.path, toSave)
            }

            if (hasNew && latestTitle != null) {
                NotificationHelper.showNewsNotification(prefs, latestTitle)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // failures are not critical; we can retry later
            return ListenableWorker.Result.retry()
        }

        return ListenableWorker.Result.success()
    }

    companion object {
        private const val WORK_NAME = "jwc_news_check"

        fun schedule(context: Context) {
            // prime seen list so user doesn't get an immediate batch of notifications
            prime(context)

            val workRequest = PeriodicWorkRequestBuilder<NewsUpdateWorker>(1, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        private fun prime(context: Context) {
            // fetch current headlines and mark as seen without notifying
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val newsSource = NewsConfigs.JWC
                    for (cat in newsSource.categories) {
                        val result = JwcCrawler.fetchNews(cat.path, 1)
                        val toSave = result.newsItems.take(20).map { it.detailUrl }.toSet()
                        NewsNotificationStore.saveSeenUrls(context, cat.path, toSave)
                    }
                } catch (_: Exception) {
                    // ignore
                }
            }
        }
    }
}
