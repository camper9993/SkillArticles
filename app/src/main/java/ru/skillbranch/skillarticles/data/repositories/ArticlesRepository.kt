package ru.skillbranch.skillarticles.data.repositories

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.paging.PositionalDataSource
import ru.skillbranch.skillarticles.data.NetworkDataHolder
import ru.skillbranch.skillarticles.data.local.DbManager.db
import ru.skillbranch.skillarticles.data.local.entities.ArticleItem
import ru.skillbranch.skillarticles.data.local.entities.CategoryData
import ru.skillbranch.skillarticles.data.remote.res.ArticleRes
import java.lang.StringBuilder

interface IArticlesRepository {
    fun loadArticlesFromNetwork(start: Int = 0, size: Int): List<ArticleRes>
    fun insertResultIntoDb(articles: List<ArticleRes>)
    fun toggleBookmark(articleId: String)
    fun findTags(): LiveData<List<String>>
    fun findCategoriesData(): LiveData<List<CategoryData>>
    fun rawQueryArticles(filter: ArticleStrategy.ArticleFilter): DataSource.Factory<Int, ArticleItem>
    fun incrementTagUseCount(tag: String): List<ArticleRes>
}

object ArticlesRepository : IArticlesRepository{

    private val network = NetworkDataHolder
//    private val articlesDao = db.articlesDao()

    override fun loadArticlesFromNetwork(start: Int, size: Int): List<ArticleRes> {
        network.findArticlesItem(start, size)
    }

    override fun insertResultIntoDb(articles: List<ArticleRes>) {

    }

    override fun toggleBookmark(articleId: String) {
        TODO("Not yet implemented")
    }

    override fun findTags(): LiveData<List<String>> {
        TODO("Not yet implemented")
    }

    override fun findCategoriesData(): LiveData<List<CategoryData>> {
        TODO("Not yet implemented")
    }

    override fun rawQueryArticles(filter: ArticleStrategy.ArticleFilter): DataSource.Factory<Int, ArticleItem> {
        TODO("Not yet implemented")
    }

    override fun incrementTagUseCount(tag: String): List<ArticleRes> {
        TODO("Not yet implemented")
    }


}

class ArticlesDataFactory(val strategy: ArticleStrategy) :
    DataSource.Factory<Int, ArticleItem>() {
    override fun create(): DataSource<Int, ArticleItem> = ArticleDataSource(strategy)
}


class ArticleDataSource(private val strategy: ArticleStrategy) :
    PositionalDataSource<ArticleItem>() {
    override fun loadInitial(
        params: LoadInitialParams,
        callback: LoadInitialCallback<ArticleItem>
    ) {
        val result = strategy.getItems(params.requestedStartPosition, params.requestedLoadSize)
        callback.onResult(result, params.requestedStartPosition)
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<ArticleItem>) {
        val result = strategy.getItems(params.startPosition, params.loadSize)
        callback.onResult(result)
    }
}

sealed class ArticleStrategy() {
    abstract fun getItems(start: Int, size: Int): List<ArticleItem>

    class AllArticles(
        private val itemProvider: (Int, Int) -> List<ArticleItem>
    ) : ArticleStrategy() {
        override fun getItems(start: Int, size: Int): List<ArticleItem> =
            itemProvider(start, size)
    }

    class SearchArticle(
        private val itemProvider: (Int, Int, String) -> List<ArticleItem>,
        private val query: String
    ) : ArticleStrategy() {
        override fun getItems(start: Int, size: Int): List<ArticleItem> =
            itemProvider(start, size, query)
    }

    class SearchBookmark(
        private val itemProvider: (Int, Int, String) -> List<ArticleItem>,
        private val query: String
    ) : ArticleStrategy() {
        override fun getItems(start: Int, size: Int): List<ArticleItem> =
            itemProvider(start, size, query)
    }

    class BookmarkArticles(
        private val itemProvider: (Int, Int) -> List<ArticleItem>
    ) : ArticleStrategy() {
        override fun getItems(start: Int, size: Int): List<ArticleItem> =
            itemProvider(start, size)
    }

    data class ArticleFilter(
        val search: String? = null,
        val isBookmark: Boolean = false,
        val categories: List<String> = listOf(),
        val isHashtah: Boolean = false
    ) {
        fun toQuery() : String {
            val qb = QueryBuilder()
            qb.table("ArticleItem")
            if (search != null && !isHashtah) qb.appendWhere("title LIKE '%$search%'")
            if (search != null && isHashtah) {
                qb.innerJoin("article_tag_x_ref AS refs", "refs.a_id = id")
                qb.appendWhere("refs.t_id = '$search'")
            }

            if (isBookmark) qb.appendWhere("is_bookmark = 1")
            if (categories.isNotEmpty()) qb.appendWhere("category_id IN (${categories.joinToString(",")})")
            qb.orderBy("date")
            return qb.build()
        }
    }

    class QueryBuilder() {
        private var table: String? = null
        private var selectColumns: String = "*"
        private var joinTables: String? = null
        private var whereCondition: String? = null
        private var order: String? = null

        fun table(table: String) : QueryBuilder {
            this.table = table
            return this
        }

        fun orderBy(column: String, isDesc: Boolean = true) : QueryBuilder {
            order = "ORDER BY $column ${if(isDesc) "DESC" else "ASC"}"
            return this
        }

        fun appendWhere(condition: String, logic: String = "AND") : QueryBuilder {
            if(whereCondition.isNullOrEmpty()) whereCondition = "WHERE $condition "
            else whereCondition += "$logic $condition"
            return this
        }

        fun innerJoin(table: String, on: String) : QueryBuilder {
            if (joinTables.isNullOrEmpty()) joinTables = "INNER JOIN $table ON $on "
            else joinTables += "INNER JOIN $table ON $on "
            return this
        }
        fun build(): String {
            check(table != null) { "table must be not null" }
            val strBuilder = StringBuilder("SELECT")
                .append("$selectColumns ")
                .append("FROM $table ")
            if (joinTables != null)  strBuilder.append(joinTables)
            if (whereCondition != null) strBuilder.append(whereCondition)
            if (order != null) strBuilder.append(order)
            return strBuilder.toString()
        }

    }
}