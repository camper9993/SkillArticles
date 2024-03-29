package ru.skillbranch.skillarticles.data.repositories

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.sqlite.db.SimpleSQLiteQuery
import ru.skillbranch.skillarticles.data.NetworkDataHolder
import ru.skillbranch.skillarticles.data.local.DbManager.db
import ru.skillbranch.skillarticles.data.local.entities.ArticleItem
import ru.skillbranch.skillarticles.data.local.entities.ArticleTagXRef
import ru.skillbranch.skillarticles.data.local.entities.CategoryData
import ru.skillbranch.skillarticles.data.local.entities.Tag
import ru.skillbranch.skillarticles.data.remote.res.ArticleRes
import ru.skillbranch.skillarticles.extensions.data.toArticle
import ru.skillbranch.skillarticles.extensions.data.toArticleCounts
import java.lang.StringBuilder

interface IArticlesRepository {
    fun loadArticlesFromNetwork(start: Int = 0, size: Int): List<ArticleRes>
    fun insertArticlesToDb(articles: List<ArticleRes>)
    fun toggleBookmark(articleId: String)
    fun findTags(): LiveData<List<String>>
    fun findCategoriesData(): LiveData<List<CategoryData>>
    fun rawQueryArticles(filter: ArticleFilter): DataSource.Factory<Int, ArticleItem>
    fun incrementTagUseCount(tag: String)
}

object ArticlesRepository : IArticlesRepository{

    private val network = NetworkDataHolder
    private val articlesDao = db.articlesDao()
    private val articlesCountsDao = db.articleCountsDao()
    private val categoriesDao = db.categoriesDao()
    private val tagsDao = db.tagsDao()
    private val articlePersonalDao = db.articlePersonalInfosDao()

    override fun loadArticlesFromNetwork(start: Int, size: Int): List<ArticleRes> =
        network.findArticlesItem(start, size)

    override fun insertArticlesToDb(articles: List<ArticleRes>) {
        articlesDao.upsert(articles.map { it.data.toArticle() })
        articlesCountsDao.upsert(articles.map { it.counts.toArticleCounts() })

        val refs = articles.map { it.data }
            .fold(mutableListOf<Pair<String, String>>()){acc, res ->
                acc.also { list -> list.addAll(res.tags.map{res.id to it})}
            }

        val tags = refs.map { it.second }
            .distinct()
            .map { Tag(it) }

        val categories = articles.map{it.data.category}

        categoriesDao.insert(categories)
        tagsDao.insert(tags)
        tagsDao.insertRefs(refs.map { ArticleTagXRef(it.first, it.second) })

    }

    override fun toggleBookmark(articleId: String) {
        articlePersonalDao.toggleBookmark(articleId)
    }

    override fun findTags(): LiveData<List<String>> =
        tagsDao.findTags()

    override fun findCategoriesData(): LiveData<List<CategoryData>> =
        categoriesDao.findAllCategoriesData()

    override fun rawQueryArticles(filter: ArticleFilter): DataSource.Factory<Int, ArticleItem>
        = articlesDao.findArticlesByRaw(SimpleSQLiteQuery( filter.toQuery()))


    override fun incrementTagUseCount(tag: String) {
        tagsDao.incrementTagUseCount(tag)
    }


}

    data class ArticleFilter(
        val search: String? = null,
        val isBookmark: Boolean = false,
        val categories: List<String> = listOf(),
        val isHashtag: Boolean = false
    ) {
        fun toQuery() : String {
            val qb = QueryBuilder()
            qb.table("ArticleItem")
            if (search != null && !isHashtag) qb.appendWhere("title LIKE '%$search%'")
            if (search != null && isHashtag) {
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