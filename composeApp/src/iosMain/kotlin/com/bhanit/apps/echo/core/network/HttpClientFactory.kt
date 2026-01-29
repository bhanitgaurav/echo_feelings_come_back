package com.bhanit.apps.echo.core.network

import com.bhanit.apps.echo.features.auth.domain.SessionRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.statement.request
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json


actual class HttpClientFactory actual constructor(
    private val sessionRepository: SessionRepository,
    private val baseUrl: String
) {
    actual fun create(): HttpClient {
        return HttpClient(Darwin) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }

            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        println("HttpClient: $message")
                    }
                }
                level = LogLevel.HEADERS
            }
            
            defaultRequest {
                url(baseUrl)
                header("Accept", "application/json")
            }
            
            /* 
            // Removed Auth plugin to avoid token caching issues
            install(Auth) { ... } 
            */
            
            HttpResponseValidator {
                validateResponse { response ->
                    if (response.status == io.ktor.http.HttpStatusCode.Unauthorized) {
                        val path = response.request.url.encodedPath
                        val host = response.request.url.host
                        val hasAuthHeader = response.request.headers.contains("Authorization")
                        if (!path.contains("/auth/") && hasAuthHeader) {
                            println("DEBUG: HttpClient - 401 Interceptor: Logging out. (Host: $host, Path: $path)")
                            com.bhanit.apps.echo.core.auth.GlobalAuthEvents.emitLogout()
                        } else {
                            println("DEBUG: HttpClient - 401 Interceptor: Ignored (No Auth Header). (Host: $host, Path: $path)")
                        }
                    }
                }
                handleResponseExceptionWithRequest { exception, _ ->
                    val clientException =
                        exception as? io.ktor.client.plugins.ClientRequestException
                            ?: return@handleResponseExceptionWithRequest
                    val exceptionResponse = clientException.response
                    if (exceptionResponse.status == io.ktor.http.HttpStatusCode.Unauthorized) {
                        val path = exceptionResponse.request.url.encodedPath
                        val host = exceptionResponse.request.url.host
                        val hasAuthHeader = exceptionResponse.request.headers.contains("Authorization")
                        if (!path.contains("/auth/") && hasAuthHeader) {
                            println("DEBUG: HttpClient - 401 Exception Interceptor: Logging out. (Host: $host, Path: $path)")
                            com.bhanit.apps.echo.core.auth.GlobalAuthEvents.emitLogout()
                        } else {
                            println("DEBUG: HttpClient - 401 Exception Interceptor: Ignored (No Auth Header). (Host: $host, Path: $path)")
                        }
                    }
                }
            }
            expectSuccess = true
        }.also { client ->
             client.requestPipeline.intercept(io.ktor.client.request.HttpRequestPipeline.State) {
                 // Determine the host for the current request
                 val host = context.url.host
                 if (!host.contains("cloudinary")) {
                     val token = sessionRepository.getSession()?.token
                     if (token != null) {
                          context.headers.append("Authorization", "Bearer $token")
                     }
                 }
             }
        }
    }
}
