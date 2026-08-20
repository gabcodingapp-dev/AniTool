package com.gab.anitool.core

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

object FileUtils {

    fun getFileName(context: Context, uri: Uri): String {
        var name = "unknown"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(idx)
            }
        }
        return name
    }

    fun copyUriToFile(context: Context, uri: Uri, destFile: File): File {
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        return destFile
    }

    fun getTempFile(context: Context, name: String): File {
        val dir = File(context.cacheDir, "anitool")
        dir.mkdirs()
        return File(dir, name)
    }

    fun exportToFile(context: Context, content: String, name: String): File {
        val dir = File(context.getExternalFilesDir(null), "exports")
        dir.mkdirs()
        val file = File(dir, name)
        file.writeText(content)
        return file
    }
}
