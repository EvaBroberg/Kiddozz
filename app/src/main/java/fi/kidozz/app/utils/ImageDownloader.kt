package fi.kidozz.app.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object ImageDownloader {
    
    suspend fun downloadImage(context: Context, imageUrl: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d("ImageDownloader", "Starting download for: $imageUrl")
            
            val bitmap = when {
                imageUrl.startsWith("http") -> downloadFromUrl(imageUrl)
                imageUrl.startsWith("android.resource://") -> loadFromResource(context, imageUrl)
                imageUrl.startsWith("content://") -> loadFromContentUri(context, imageUrl)
                imageUrl.startsWith("file://") -> loadFromFile(imageUrl)
                else -> {
                    Log.e("ImageDownloader", "Unsupported URL format: $imageUrl")
                    return@withContext Result.failure(Exception("Unsupported URL format: $imageUrl"))
                }
            }
            
            bitmap?.let { bmp ->
                val savedUri = saveImageToGallery(context, bmp, generateFileName())
                Log.d("ImageDownloader", "Image saved successfully: $savedUri")
                Result.success(savedUri)
            } ?: run {
                Log.e("ImageDownloader", "Failed to load bitmap")
                Result.failure(Exception("Failed to load image"))
            }
        } catch (e: Exception) {
            Log.e("ImageDownloader", "Download failed", e)
            Result.failure(e)
        }
    }
    
    private suspend fun downloadFromUrl(url: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.doInput = true
            connection.connect()
            
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream: InputStream = connection.inputStream
                BitmapFactory.decodeStream(inputStream)
            } else {
                Log.e("ImageDownloader", "HTTP error: ${connection.responseCode}")
                null
            }
        } catch (e: Exception) {
            Log.e("ImageDownloader", "Failed to download from URL", e)
            null
        }
    }
    
    private fun loadFromResource(context: Context, resourceUri: String): Bitmap? {
        return try {
            Log.d("ImageDownloader", "Loading from resource: $resourceUri")
            val uri = Uri.parse(resourceUri)
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                bitmap
            } else {
                Log.e("ImageDownloader", "Could not open input stream for resource")
                null
            }
        } catch (e: Exception) {
            Log.e("ImageDownloader", "Failed to load from resource: $resourceUri", e)
            null
        }
    }
    
    private fun loadFromContentUri(context: Context, contentUri: String): Bitmap? {
        return try {
            Log.d("ImageDownloader", "Loading from content URI: $contentUri")
            val uri = Uri.parse(contentUri)
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                bitmap
            } else {
                Log.e("ImageDownloader", "Could not open input stream for content URI")
                null
            }
        } catch (e: Exception) {
            Log.e("ImageDownloader", "Failed to load from content URI: $contentUri", e)
            null
        }
    }
    
    private fun loadFromFile(fileUri: String): Bitmap? {
        return try {
            Log.d("ImageDownloader", "Loading from file: $fileUri")
            val file = File(fileUri.removePrefix("file://"))
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                Log.e("ImageDownloader", "File does not exist: $fileUri")
                null
            }
        } catch (e: Exception) {
            Log.e("ImageDownloader", "Failed to load from file: $fileUri", e)
            null
        }
    }
    
    private fun saveImageToGallery(context: Context, bitmap: Bitmap, fileName: String): String {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Kiddozz")
            }
        }
        
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        
        uri?.let { imageUri ->
            context.contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            }
            return imageUri.toString()
        }
        
        // Fallback for older Android versions
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val kiddozzDir = File(picturesDir, "Kiddozz")
        if (!kiddozzDir.exists()) {
            kiddozzDir.mkdirs()
        }
        
        val file = File(kiddozzDir, fileName)
        FileOutputStream(file).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        }
        
        return file.absolutePath
    }
    
    private fun generateFileName(): String {
        val timestamp = System.currentTimeMillis()
        return "kiddozz_event_${timestamp}.jpg"
    }
}
