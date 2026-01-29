package com.bhanit.apps.echo.core.network

import com.bhanit.apps.echo.data.model.ApiErrorDTO
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.ResponseException
import io.ktor.serialization.JsonConvertException
import kotlinx.serialization.SerializationException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException

data class ApiErrorModel(
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val errorDescription: String? = null
)

class AppException(
    val title: String,
    override val message: String,
    val code: Int? = null,
    val errorModel: ApiErrorModel? = null
) : Exception(message)

suspend fun <T> safeApiCall(apiCall: suspend () -> T): Result<T> {
    // Unified Messages
    val msgOffline = "Seems like you’re offline. Please check your internet connection."
    val msgParseError = "Something went wrong. Please try again."
    val msgUnknown = "Oops! An error occurred. Please try again after some time."
    val msgBadRequest = "We couldn’t complete this request. Please try again."
    val msgServerError = "It’s not you — it’s us. We are looking into it."
    
    // Helper to map DTO -> Model
    fun mapToModel(dto: ApiErrorDTO?): ApiErrorModel {
        if (dto == null) return ApiErrorModel(errorMessage = msgUnknown)
        return ApiErrorModel(
            errorCode = dto.errorCode,
            errorMessage = dto.errorMessage?.takeIf { it.isNotBlank() } ?: msgUnknown,
            errorDescription = dto.errorDescription
        )
    }

    // Helper to create AppException from Model
    fun error(model: ApiErrorModel, code: Int? = null): Result<T> {
        return Result.failure(
            AppException(
                title = "Error", // Generic title as requested contract focuses on errorMessage
                message = model.errorMessage ?: msgUnknown,
                code = code,
                errorModel = model
            )
        )
    }

    return try {
        Result.success(apiCall())
    } catch (e: Throwable) {
        when (e) {
            is kotlinx.coroutines.CancellationException -> throw e
            is AppException -> Result.failure(e)
            is ResponseException -> {
                val status = e.response.status.value
                val hasAuthHeader = e.response.call.request.headers.contains("Authorization")
                
                if (status == 401 && hasAuthHeader) {
                    val url = e.response.call.request.url
                    if (!url.host.contains("cloudinary")) {
                        try {
                            com.bhanit.apps.echo.core.auth.GlobalAuthEvents.emitLogout()
                        } catch (ignored: Exception) {}
                    }
                    return error(ApiErrorModel(errorMessage = "Session Expired"), status)
                }

                val model = try {
                    mapToModel(e.response.body<ApiErrorDTO>())
                } catch (ignored: Exception) {
                    when {
                        status == 400 || status == 403 -> ApiErrorModel(errorMessage = msgBadRequest)
                        status >= 500 -> ApiErrorModel(errorMessage = msgServerError)
                        else -> ApiErrorModel(errorMessage = msgUnknown)
                    }
                }
                error(model, status)
            }
            is SerializationException, 
            is JsonConvertException -> error(ApiErrorModel(errorMessage = msgParseError))
            is kotlinx.io.IOException,
            is HttpRequestTimeoutException,
            is ConnectTimeoutException,
            is SocketTimeoutException -> error(ApiErrorModel(errorMessage = msgOffline))
            else -> {
                // Handle UnknownHostException which usually indicates offline/DNS issues on Android/JVM
                if (e::class.simpleName?.contains("UnknownHostException") == true || 
                    e::class.simpleName?.contains("ConnectException") == true ||
                    e.toString().contains("UnknownHostException")) {
                    return error(ApiErrorModel(errorMessage = msgOffline))
                }
                e.printStackTrace()
                error(ApiErrorModel(errorMessage = msgUnknown))
            }
        }
    }
}

