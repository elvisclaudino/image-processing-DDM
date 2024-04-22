package com.example.imageprocessing.repositories

import android.graphics.Bitmap
import android.net.Uri

interface EditimageReposity {
    suspend fun prepareImagePreview(imageUri: Uri): Bitmap?
}