package com.muzaffer.bistai.data.remote.dto

import com.google.gson.annotations.SerializedName

/** GNews API /search endpoint'inin yanıt modelleri. */
data class GNewsResponse(
    @SerializedName("totalArticles") val totalArticles: Int,
    @SerializedName("articles")      val articles: List<GNewsArticle>
)

data class GNewsArticle(
    @SerializedName("title")       val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("url")         val url: String,
    @SerializedName("publishedAt") val publishedAt: String,
    @SerializedName("source")      val source: GNewsSource
)

data class GNewsSource(
    @SerializedName("name") val name: String,
    @SerializedName("url")  val url: String
)
