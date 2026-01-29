package com.bhanit.apps.echo.domain.service

import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import io.github.cdimascio.dotenv.Dotenv
import java.time.Instant

class CloudinaryService {
    private val cloudinary: Cloudinary

    private val folderName: String

    init {
        val dotenv = Dotenv.configure().ignoreIfMissing().load()
        val cloudinaryUrl = dotenv["CLOUDINARY_URL"] ?: System.getenv("CLOUDINARY_URL")
        folderName = dotenv["CLOUDINARY_FOLDER"] ?: System.getenv("CLOUDINARY_FOLDER") ?: "echoprofile"
        
        if (cloudinaryUrl == null) {
            println("WARNING: CLOUDINARY_URL not found in .env or environment variables.")
            // Fallback for build phase or dev
            cloudinary = Cloudinary(ObjectUtils.asMap(
                "cloud_name", "echodev",
                "api_key", "placeholder",
                "api_secret", "placeholder"
            ))
        } else {
            cloudinary = Cloudinary(cloudinaryUrl)
        }
    }

    fun generateUploadParams(userId: String): com.bhanit.apps.echo.data.model.CloudinaryParamsDTO {
        val timestamp = Instant.now().epochSecond
        val publicId = userId // Use stable publicId (userId) to allow overwrites
        
        val params = HashMap<String, Any>()
        params["timestamp"] = timestamp
        params["public_id"] = publicId
        params["folder"] = folderName
        params["overwrite"] = true

        val signature = cloudinary.apiSignRequest(params, cloudinary.config.apiSecret)
        
        return com.bhanit.apps.echo.data.model.CloudinaryParamsDTO(
            apiKey = cloudinary.config.apiKey,
            cloudName = cloudinary.config.cloudName,
            timestamp = timestamp,
            signature = signature,
            publicId = publicId,
            folder = folderName,
            overwrite = true
        )
    }
}
