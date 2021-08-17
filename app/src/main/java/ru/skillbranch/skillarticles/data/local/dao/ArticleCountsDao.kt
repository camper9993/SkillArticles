package ru.skillbranch.skillarticles.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import ru.skillbranch.skillarticles.data.local.entities.Article
import ru.skillbranch.skillarticles.data.local.entities.ArticleCounts

@Dao
interface ArticleCountsDao : BaseDao<ArticleCounts> {
    @Transaction
    fun upsert(list: List<ArticleCounts>) {
        insert(list)
            .mapIndexedNotNull {index, recordResult -> if(recordResult == -1L) list[index] else null}
            .also { if(it.isNotEmpty()) update(it) }
    }


//    fun incrementLikeOrInsert(articleId: String)

    @Query("""
        UPDATE article_counts SET likes = likes + 1, updated_at = CURRENT_TIMESTAMP
        WHERE article_id = :articleId
    """)
    fun incrementLike(articleId: String) : Int

    @Query("""
        UPDATE article_counts SET likes = MAX(0, likes - 1), updated_at = CURRENT_TIMESTAMP
        WHERE article_id = :articleId
    """)
    fun decrementLike(articleId: String) : Int

    @Query("""
        UPDATE article_counts SET comments = comments + 1, updated_at = CURRENT_TIMESTAMP
        WHERE article_id = :articleId
    """)
    fun incrementCommentsCount(articleId: String)

    @Query("""
        SELECT comments FROM article_counts
        WHERE article_id = :articleId
    """)
    fun getCommentsCount(articleId: String) : LiveData<Int>
}