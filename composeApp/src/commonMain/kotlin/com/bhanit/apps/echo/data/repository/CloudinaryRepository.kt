package com.bhanit.apps.echo.data.repository

import MediaApi

class CloudinaryRepositoryImpl(private val mediaApi: MediaApi) {

    suspend fun uploadImage(fileBytes: ByteArray): Result<String> {
        return mediaApi.getUploadParams().mapCatching { params ->
            val response = mediaApi.uploadToCloudinary(fileBytes, params).getOrThrow()
            response.secure_url ?: throw Exception("Upload failed: No URL returned")
        }
    }
}
