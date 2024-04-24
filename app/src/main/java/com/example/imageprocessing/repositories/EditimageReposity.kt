package com.example.imageprocessing.repositories

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.imageprocessing.data.ImageFilter

interface EditimageReposity {
    suspend fun prepareImagePreview(imageUri: Uri): Bitmap?
    suspend fun getImageFilters(image: Bitmap): List<ImageFilter>
    suspend fun saveFilteredImage(context: Context, filteredBitmap: Bitmap): Uri?
}
