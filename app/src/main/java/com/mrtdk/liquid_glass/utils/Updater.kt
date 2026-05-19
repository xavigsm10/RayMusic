package com.mrtdk.liquid_glass.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.mrtdk.liquid_glass.BuildConfig
import okhttp3.*
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object Updater {
    private val client = OkHttpClient()
    private const val GITHUB_REPO = "xavigsm10/RayMusic"

    data class ReleaseInfo(
        val versionName: String,
        val downloadUrl: String,
        val body: String?
    )

    fun checkUpdate(callback: (ReleaseInfo?) -> Unit) {
        val request = Request.Builder()
            .url("https://api.github.com/repos/$GITHUB_REPO/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "RayMusic")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        callback(null)
                        return
                    }
                    val body = response.body?.string() ?: return callback(null)
                    val json = JSONObject(body)
                    val tagName = json.optString("tag_name", "")
                    val version = tagName.removePrefix("v")
                    
                    if (version != BuildConfig.VERSION_NAME) {
                        val assets = json.optJSONArray("assets")
                        var downloadUrl: String? = null
                        if (assets != null) {
                            for (i in 0 until assets.length()) {
                                val asset = assets.getJSONObject(i)
                                val url = asset.optString("browser_download_url", "")
                                if (url.endsWith(".apk")) {
                                    downloadUrl = url
                                    break
                                }
                            }
                        }
                        if (downloadUrl != null) {
                            callback(ReleaseInfo(version, downloadUrl, json.optString("body")))
                        } else {
                            callback(null)
                        }
                    } else {
                        callback(null)
                    }
                }
            }
        })
    }

    fun downloadApk(context: Context, url: String, onProgress: (Float) -> Unit, onComplete: (File?) -> Unit) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onComplete(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        onComplete(null)
                        return
                    }
                    val body = response.body ?: return onComplete(null)
                    val contentLength = body.contentLength()
                    val input = body.byteStream()
                    val file = File(context.cacheDir, "update.apk")
                    val output = FileOutputStream(file)

                    val data = ByteArray(8192)
                    var total = 0L
                    var count: Int
                    while (input.read(data).also { count = it } != -1) {
                        total += count
                        if (contentLength > 0) {
                            onProgress(total.toFloat() / contentLength)
                        }
                        output.write(data, 0, count)
                    }
                    output.flush()
                    output.close()
                    input.close()
                    onComplete(file)
                }
            }
        })
    }

    fun installApk(context: Context, file: File) {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.FileProvider",
                file
            )
        } else {
            Uri.fromFile(file)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}
