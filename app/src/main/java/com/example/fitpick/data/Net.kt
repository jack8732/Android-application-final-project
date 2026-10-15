package com.example.fitpick.data

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// ---------- wger exercise API (free, no key) ----------
interface WgerApi {
    @GET("exerciseinfo/")
    suspend fun exercises(
        @Query("language") language: Int = 2,   // 2 = English
        @Query("muscles") muscles: Int,
        @Query("limit") limit: Int = 40
    ): WgerResponse
}

data class WgerResponse(val results: List<WgerExercise>?)
data class WgerExercise(
    val id: Int?,
    val muscles: List<WgerMuscle>?,
    val translations: List<WgerTranslation>?,
    val images: List<WgerImage>?
)
data class WgerMuscle(val id: Int?)
data class WgerTranslation(val name: String?, val language: Int?)
data class WgerImage(
    val image: String?,
    @SerializedName("is_main") val isMain: Boolean?
)

// ---------- Gemini API (AI) ----------
interface GeminiApi {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generate(
        @Path("model") model: String,
        @Query("key") key: String,
        @Body body: GeminiRequest
    ): GeminiResponse
}

data class GeminiRequest(
    val contents: List<GContent>,
    val generationConfig: GGenConfig = GGenConfig()
)
data class GContent(val parts: List<GPart>, val role: String? = "user")
data class GPart(val text: String?)
data class GGenConfig(
    val responseMimeType: String = "application/json",
    val temperature: Double = 0.7
)
data class GeminiResponse(val candidates: List<GCandidate>?)
data class GCandidate(val content: GContent?)

object Net {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun retrofit(baseUrl: String) = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val wger: WgerApi by lazy { retrofit("https://wger.de/api/v2/").create(WgerApi::class.java) }
    val gemini: GeminiApi by lazy {
        retrofit("https://generativelanguage.googleapis.com/").create(GeminiApi::class.java)
    }
}
