package ru.skillbranch.skillarticles.data.repositories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import androidx.paging.ItemKeyedDataSource
import ru.skillbranch.skillarticles.data.NetworkDataHolder
import ru.skillbranch.skillarticles.data.local.DbManager.db
import ru.skillbranch.skillarticles.data.local.PrefManager
import ru.skillbranch.skillarticles.data.local.entities.ArticleFull
import ru.skillbranch.skillarticles.data.local.entities.ArticlePersonalInfo
import ru.skillbranch.skillarticles.data.models.*
import ru.skillbranch.skillarticles.extensions.data.toArticleContent
import java.lang.Thread.sleep
import kotlin.math.abs

interface IArticleRepository {
    fun findArticle(articleId: String) : LiveData<ArticleFull>
    fun getAppSettings(): LiveData<AppSettings>
    fun toggleLike(articleId: String)
    fun toggleBookmark(articleId: String)
    fun isAuth(): MutableLiveData<Boolean>
    fun loadCommentsByRange(slug: String?, size: Int, articleId: String): List<CommentItemData>
    fun sendMessage(articleId: String, text: String, answerToSlug: String?)
    fun loadAllComments(articleId: String, total: Int): CommentsDataFactory
    fun decrementLike(articleId: String)
    fun incrementLike(articleId: String)
    fun updateSettings(copy: AppSettings)
    fun fetchArticleContent(articleId: String)
    fun findArticleCommentCount(articleId: String): LiveData<Int>
}

object ArticleRepository : IArticleRepository{


    private val network = NetworkDataHolder
    private val preferences = PrefManager
    private val articleDao = db.articlesDao()
    private val articlePersonalDao = db.articlePersonalInfosDao()
    private val articleCountsDao = db.articleCountsDao()
    private val articleContentDao = db.articleContentsDao()

    override fun findArticle(articleId: String): LiveData<ArticleFull> {
        return articleDao.findFullArticle(articleId)
    }

    override fun getAppSettings(): LiveData<AppSettings> = preferences.getAppSettings() //from preferences

    override fun toggleLike(articleId: String) {
        articlePersonalDao.toggleLikeOrInsert(articleId)
    }

    override fun toggleBookmark(articleId: String) {
        articlePersonalDao.toggleBookmarkOrInsert(articleId)
    }

    override fun updateSettings(appSettings: AppSettings) {
        //TODO
    }

    override fun fetchArticleContent(articleId: String) {
        val content = network.loadArticleContent(articleId).apply { sleep(3000) }
        articleContentDao.insert(content.toArticleContent())
    }

    override fun findArticleCommentCount(articleId: String): LiveData<Int> {
        return articleCountsDao.getCommentsCount(articleId)
    }

    override fun isAuth(): MutableLiveData<Boolean> = preferences.isAuth()

    override fun loadAllComments(articleId: String, totalCount: Int) =
        CommentsDataFactory(
            itemProvider = ::loadCommentsByRange,
            articleId = articleId,
            totalCount = totalCount
        )

    override fun loadCommentsByRange(
        slug: String?,
        size: Int,
        articleId: String
    ): List<CommentItemData> {
        val data = network.commentsData.getOrElse(articleId) { mutableListOf() }
        return when {
            slug == null -> data.take(size)

            size > 0 -> data.dropWhile { it.slug != slug }
                .drop(1)
                .take(size)

            size < 0 -> data
                .dropLastWhile { it.slug != slug }
                .dropLast(1)
                .takeLast(abs(size))

            else -> emptyList()
        }.apply { sleep(1500) }
    }

    override fun decrementLike(articleId: String) {
        articleCountsDao.decrementLike(articleId)
    }

    override fun incrementLike(articleId: String) {
        articleCountsDao.incrementLike(articleId)
    }

    override fun sendMessage(articleId: String, comment: String, answerToSlug: String?) {
        network.sendMessage(
            articleId, comment, answerToSlug,
            User("777", "John Doe", "https://skill-branch.ru/img/mail/bot/android-category.png")
        )
        articleCountsDao.incrementCommentsCount(articleId)
    }
}

class CommentsDataFactory(
    private val itemProvider: (String?, Int, String) -> List<CommentItemData>,
    private val articleId: String,
    private val totalCount: Int
) : DataSource.Factory<String?, CommentItemData>() {
    override fun create(): DataSource<String?, CommentItemData> =
        CommentsDataSource(itemProvider, articleId, totalCount)

}

class CommentsDataSource(
    private val itemProvider: (String?, Int, String) -> List<CommentItemData>,
    private val articleId: String,
    private val totalCount: Int
) : ItemKeyedDataSource<String, CommentItemData>() {

    override fun loadInitial(
        params: LoadInitialParams<String>,
        callback: LoadInitialCallback<CommentItemData>
    ) {
        val result = itemProvider(params.requestedInitialKey, params.requestedLoadSize, articleId)

        callback.onResult(
            if (totalCount > 0) result else emptyList(),
            0,
            totalCount
        )
    }

    override fun loadAfter(params: LoadParams<String>, callback: LoadCallback<CommentItemData>) {
        val result = itemProvider(params.key, params.requestedLoadSize, articleId)
        callback.onResult(result)
    }

    override fun loadBefore(params: LoadParams<String>, callback: LoadCallback<CommentItemData>) {
        val result = itemProvider(params.key, -params.requestedLoadSize, articleId)
        callback.onResult(result)
    }

    override fun getKey(item: CommentItemData): String = item.slug



}
