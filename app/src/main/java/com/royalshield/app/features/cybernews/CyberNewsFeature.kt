package com.royalshield.app.features.cybernews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.royalshield.app.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class CyberNewsItem(
    @Json(name = "cveId") val cveId: String,
    val vendor: String,
    val product: String,
    val title: String,
    val summary: String,
    @Json(name = "requiredAction") val requiredAction: String,
    @Json(name = "dateAdded") val dateAdded: String,
    @Json(name = "dueDate") val dueDate: String,
    @Json(name = "knownRansomwareUse") val knownRansomwareUse: String,
    @Json(name = "sourceUrl") val sourceUrl: String,
)

@JsonClass(generateAdapter = true)
data class CyberNewsResponse(
    val source: String,
    @Json(name = "catalogVersion") val catalogVersion: String,
    @Json(name = "updatedAt") val updatedAt: String,
    val items: List<CyberNewsItem>,
)

data class CyberNewsUiState(
    val isLoading: Boolean = true,
    val response: CyberNewsResponse? = null,
    val errorMessage: String? = null,
)

private interface CyberNewsApi {
    @GET("api/v1/cyber-news")
    suspend fun getCyberNews(@Query("limit") limit: Int = 8): CyberNewsResponse
}

private object CyberNewsRepository {
    private val api: CyberNewsApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL.trimEnd('/') + "/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(CyberNewsApi::class.java)
    }

    suspend fun load(): CyberNewsResponse = api.getCyberNews()
}

class CyberNewsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CyberNewsUiState())
    val uiState: StateFlow<CyberNewsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { CyberNewsRepository.load() }
                .onSuccess { response ->
                    _uiState.value = CyberNewsUiState(isLoading = false, response = response)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Cyber news is temporarily unavailable",
                    )
                }
        }
    }
}
