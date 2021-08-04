package ru.skillbranch.skillarticles.data.local

import androidx.room.*
import com.facebook.stetho.BuildConfig
import ru.skillbranch.skillarticles.App
import ru.skillbranch.skillarticles.data.local.dao.*
import ru.skillbranch.skillarticles.data.local.entities.*

object DbManager {
    val db = Room.databaseBuilder(
        App.applicationContext(),
        AppDb::class.java,
        AppDb.DATABASE_NAME
    )
}

@Database(
    entities = [Article::class,
        ArticleCounts::class,
        Category::class,
        Tag::class,
        ArticlePersonalInfo::class,
        ArticleTagXRef::class,
        ArticleContent::class],
    version = AppDb.DATABASE_VERSION,
    exportSchema = false,
    views = [ArticleItem::class]
)
@TypeConverters(DateConverter::class)
abstract class AppDb : RoomDatabase() {
    companion object {
        const val DATABASE_NAME : String = BuildConfig.APPLICATION_ID + ".db"
        const val DATABASE_VERSION = 1
    }
    abstract fun articlesDao(): ArticlesDao
    abstract fun categoriesDao(): CategoriesDao
    abstract fun articleCountsDao(): ArticleCountsDao
    abstract fun articlePersonalInfosDao(): ArticlePersonalInfosDao
    abstract fun tagsDao(): TagsDao
    abstract fun articleContentsDao() : ArticleContentsDao
}