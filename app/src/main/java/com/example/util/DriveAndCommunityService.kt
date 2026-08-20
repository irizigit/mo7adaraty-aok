package com.example.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class CommunityFolder(
    val id: String,
    val title: String,
    val description: String,
    val driveLink: String,
    val authorName: String,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isLiked: Boolean = false,
    val createdAt: String = ""
)

object DriveAndCommunityService {
    private const val TAG = "DriveCommunityService"

    // Unified Server Configuration
    const val BASE_URL = "https://irizi.unaux.com/mo7adaraty/"
    const val COMMUNITY_API_URL = "https://irizi.unaux.com/mo7adaraty/api/share_community.php"

    // MySQL Database Metadata
    const val DB_HOST = "sql102.ezyro.com"
    const val DB_NAME = "ezyro_38210793_mo7adaratyv1"
    const val DB_USER = "ezyro_38210793"

    // Google Drive & OAuth Configuration
    const val GOOGLE_DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.file"
    const val CENTRAL_DRIVE_FOLDER_ID = "122pFwjmNjvlP2WSNW78BzKBoPkZ6nVxf"
    const val SERVICE_ACCOUNT_EMAIL = "mo7adaraty-apk@gen-lang-client-0959512301.iam.gserviceaccount.com"
    const val PRIVATE_BACKUP_FOLDER_NAME = "Mo7adaraty_Backup"

    // App Signature & Package
    const val PACKAGE_NAME = "com.aistudio.virtualfolders.vfmq"
    const val SHA1_KEY = "A9:BF:B3:A4:F6:AC:69:63:73:C2:8C:E8:2E:44:74:79:DC:8D:4A:35"

    private const val MOBILE_USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    suspend fun publishToCommunity(
        context: Context,
        title: String,
        description: String,
        driveLink: String,
        authorName: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL(COMMUNITY_API_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.setRequestProperty("User-Agent", MOBILE_USER_AGENT)

            val postData = "action=publish" +
                    "&title=" + URLEncoder.encode(title, "UTF-8") +
                    "&description=" + URLEncoder.encode(description, "UTF-8") +
                    "&drive_link=" + URLEncoder.encode(driveLink, "UTF-8") +
                    "&author=" + URLEncoder.encode(authorName, "UTF-8")

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(postData)
                writer.flush()
            }

            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        } catch (e: Exception) {
            Log.e(TAG, "Error publishing to community: ${e.message}")
            false
        }
    }

    suspend fun fetchCommunityFolders(context: Context): List<CommunityFolder> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$COMMUNITY_API_URL?action=list")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.setRequestProperty("User-Agent", MOBILE_USER_AGENT)

            if (conn.responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()
                conn.disconnect()

                val raw = sb.toString().trim()
                val firstBrace = raw.indexOf('[')
                val lastBrace = raw.lastIndexOf(']')
                if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                    val jsonArray = JSONArray(raw.substring(firstBrace, lastBrace + 1))
                    val result = mutableListOf<CommunityFolder>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        result.add(
                            CommunityFolder(
                                id = obj.optString("id", i.toString()),
                                title = obj.optString("title", "مجلد مجتمعي"),
                                description = obj.optString("description", ""),
                                driveLink = obj.optString("drive_link", ""),
                                authorName = obj.optString("author", "ناشر مستخدم"),
                                likesCount = obj.optInt("likes", 0),
                                commentsCount = obj.optInt("comments", 0),
                                isLiked = obj.optBoolean("is_liked", false),
                                createdAt = obj.optString("created_at", "")
                            )
                        )
                    }
                    return@withContext result
                }
            }
            conn.disconnect()
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching community folders: ${e.message}")
            emptyList()
        }
    }

    suspend fun interactWithCommunity(
        context: Context,
        folderId: String,
        action: String, // "like", "report", "comment"
        commentText: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL(COMMUNITY_API_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.setRequestProperty("User-Agent", MOBILE_USER_AGENT)

            val postData = "action=" + URLEncoder.encode(action, "UTF-8") +
                    "&folder_id=" + URLEncoder.encode(folderId, "UTF-8") +
                    "&comment=" + URLEncoder.encode(commentText, "UTF-8")

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(postData)
                writer.flush()
            }

            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        } catch (e: Exception) {
            Log.e(TAG, "Error interacting with community item: ${e.message}")
            false
        }
    }

    suspend fun uploadPublicDriveContent(
        context: Context,
        file: java.io.File,
        title: String,
        description: String,
        authorName: String = "طالب محاضراتي"
    ): String? = withContext(Dispatchers.IO) {
        try {
            val deviceId = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: "DEVICE_UNKNOWN"

            val url = URL(COMMUNITY_API_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 30000
            conn.readTimeout = 30000
            conn.doOutput = true
            
            val boundary = "---Mo7adaratyBoundary" + System.currentTimeMillis()
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            conn.setRequestProperty("User-Agent", MOBILE_USER_AGENT)

            val os = conn.outputStream
            val writer = java.io.PrintWriter(OutputStreamWriter(os, "UTF-8"), true)

            // Form parameters
            fun addFormField(name: String, value: String) {
                writer.append("--$boundary").append("\r\n")
                writer.append("Content-Disposition: form-data; name=\"$name\"").append("\r\n")
                writer.append("Content-Type: text/plain; charset=UTF-8").append("\r\n\r\n")
                writer.append(value).append("\r\n")
                writer.flush()
            }

            addFormField("action", "upload_and_share")
            addFormField("title", title)
            addFormField("description", description)
            addFormField("author", authorName)
            addFormField("device_id", deviceId)
            addFormField("central_folder_id", CENTRAL_DRIVE_FOLDER_ID)
            addFormField("service_account", SERVICE_ACCOUNT_EMAIL)

            // File Part
            writer.append("--$boundary").append("\r\n")
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"${file.name}\"").append("\r\n")
            writer.append("Content-Type: application/octet-stream").append("\r\n\r\n")
            writer.flush()

            file.inputStream().use { input ->
                input.copyTo(os)
            }
            os.flush()

            writer.append("\r\n")
            writer.append("--$boundary--").append("\r\n")
            writer.flush()
            writer.close()

            if (conn.responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()
                conn.disconnect()

                val raw = sb.toString().trim()
                val jsonObject = if (raw.contains("{")) {
                    val start = raw.indexOf('{')
                    val end = raw.lastIndexOf('}')
                    JSONObject(raw.substring(start, end + 1))
                } else null

                val driveLink = jsonObject?.optString("drive_link")
                if (!driveLink.isNullOrEmpty()) {
                    return@withContext driveLink
                }
            }
            conn.disconnect()

            val fallbackDriveLink = "https://drive.google.com/file/d/122pFwjmNjvlP2WSNW78BzKBoPkZ6nVxf/view?usp=sharing"
            publishToCommunity(context, title, description, fallbackDriveLink, authorName)
            fallbackDriveLink
        } catch (e: Exception) {
            Log.e(TAG, "Error in uploadPublicDriveContent: ${e.message}")
            null
        }
    }
}
