import com.bhanit.apps.echo.core.network.safeApiCall
import com.bhanit.apps.echo.data.model.CloudinaryParamsDTO
import com.bhanit.apps.echo.data.model.CloudinaryResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

class MediaApi(private val client: HttpClient) {
    
    suspend fun getUploadParams(): Result<CloudinaryParamsDTO> {
        return safeApiCall {
            client.get("/media/params").body()
        }
    }

    suspend fun uploadToCloudinary(
        fileBytes: ByteArray,
        params: CloudinaryParamsDTO
    ): Result<CloudinaryResponse> {
        val folderParam = if (params.folder != null) "&folder=${params.folder}" else ""
        val overwriteParam = "&overwrite=${params.overwrite}"
        val cloudinaryUrl = "https://api.cloudinary.com/v1_1/${params.cloudName}/image/upload?api_key=${params.apiKey}&timestamp=${params.timestamp}&signature=${params.signature}&public_id=${params.publicId}$folderParam$overwriteParam"


        
        return safeApiCall {
            client.submitFormWithBinaryData(
                url = cloudinaryUrl,
                formData = formData {
                    // All params are in the URL now. Only file remains in body.
                    
                    // File must be the last part for Cloudinary
                    append("file", fileBytes, Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"${params.publicId}.jpg\"")
                    })
                }
            ).body()
        }
    }
}
