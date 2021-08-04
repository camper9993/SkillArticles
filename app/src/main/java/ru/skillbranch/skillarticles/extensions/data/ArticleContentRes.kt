package ru.skillbranch.skillarticles.extensions.data

import ru.skillbranch.skillarticles.data.local.entities.ArticleContent
import ru.skillbranch.skillarticles.data.remote.res.ArticleContentRes

fun ArticleContentRes.toArticleContent(): ArticleContent = ArticleContent(
    articleId = articleId,
    content = content,
    shareLink = shareLink,
    source = source,
    updatedAt = updatedAt
)