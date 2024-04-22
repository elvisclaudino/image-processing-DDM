package com.example.imageprocessing.viewmodels

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.example.imageprocessing.repositories.EditimageReposity

class EditImageViewModel(private val editImageRepository: EditimageReposity): ViewModel() {
    data class ImagePreviewDataState(
        val isLoading: Boolean,
        val bitmap: Bitmap?,
        val error: String?
    )
}