package com.example.imageprocessing.viewmodels

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.imageprocessing.data.ImageFilter
import com.example.imageprocessing.repositories.EditimageReposity
import com.example.imageprocessing.utilities.Coroutines
import kotlin.math.log

class EditImageViewModel(private val editImageRepository: EditimageReposity): ViewModel() {

    //region:: Prepare image preview

    private val imagePreviewDataState = MutableLiveData<ImagePreviewDataState>()
    val imagePreviewUiState: LiveData<ImagePreviewDataState> get() = imagePreviewDataState

    fun prepareImagePreview(imageUri: Uri) {
        Coroutines.io {
            runCatching {
                emitImagePreviewUiState(isLoading = true)
                editImageRepository.prepareImagePreview(imageUri)
            }.onSuccess { bitmap ->
                if(bitmap != null) {
                    emitImagePreviewUiState(bitmap = bitmap)
                } else {
                    emitImagePreviewUiState(error = "Preview de imagem indisponivel")
                }
            }.onFailure {
                emitImagePreviewUiState(error = it.message.toString())
            }
        }
    }

    private fun emitImagePreviewUiState(
        isLoading: Boolean = false,
        bitmap: Bitmap? = null,
        error: String? = null
    ) {
        val dataState = ImagePreviewDataState(isLoading, bitmap, error)
        imagePreviewDataState.postValue(dataState)
    }

    data class ImagePreviewDataState(
        val isLoading: Boolean,
        val bitmap: Bitmap?,
        val error: String?
    )

    //endregion

    //region:: Load image filters

    private val imageFiltersDataState = MutableLiveData<ImageFiltersDataState>()
    val imageFilterUiState: LiveData<ImageFiltersDataState> get() = imageFiltersDataState

    fun loadImageFilters(originalImage: Bitmap) {
        Coroutines.io {
            runCatching {
                emitImageFiltersUiState(isLoading = true)
                editImageRepository.getImageFilters(getPreviewImage(originalImage))
            }.onSuccess { imageFilters ->
                emitImageFiltersUiState(imageFilters = imageFilters)
            }.onFailure {
                emitImageFiltersUiState(error = it.message.toString())
            }
        }
    }

    private fun getPreviewImage(originalImage: Bitmap) : Bitmap {
        return runCatching {
            val previewWidth = 150
            val previewHeight = originalImage.height * previewWidth / originalImage.width
            Bitmap.createScaledBitmap(originalImage, previewWidth, previewHeight, false)
        }.getOrDefault(originalImage)
    }

    private fun emitImageFiltersUiState(
        isLoading: Boolean = false,
        imageFilters: List<ImageFilter>? = null,
        error: String? = null
    ) {
        val dataState = ImageFiltersDataState(isLoading, imageFilters, error)
        imageFiltersDataState.postValue(dataState)
    }

    data class ImageFiltersDataState(
        val isLoading: Boolean,
        val imageFilters: List<ImageFilter>?,
        val error: String?
    )

    //endregion

    //region:: Save filtered image

    private val saveFilteredImageDataState = MutableLiveData<SaveFilteredImageState>()
    val saveFilteredImageUiState: LiveData<SaveFilteredImageState> get() = saveFilteredImageDataState

    fun saveFilteredimage(context: Context, filteredBitmap: Bitmap) {
        Log.d("Teste", "teste")
        Coroutines.io {
            runCatching {
                Log.d("run", "run")
                emitSaveFilteredImageUiState(isLoading = true)
                editImageRepository.saveFilteredImage(context, filteredBitmap)
            }.onSuccess { savedImageUri ->
                if (savedImageUri != null) {
                    Log.d("oi", "oiiii")
                    addToGallery(context, savedImageUri)
                    emitSaveFilteredImageUiState(uri = savedImageUri)
                } else {
                    emitSaveFilteredImageUiState(error = "Erro ao salvar a imagem")
                }
            }.onFailure {
                Log.d("erro", "erro")
                emitSaveFilteredImageUiState(error = it.message.toString())
            }
        }
    }


    private fun emitSaveFilteredImageUiState(
        isLoading: Boolean = false,
        uri: Uri? = null,
        error: String? = null
    ) {
        val dataState = SaveFilteredImageState(isLoading, uri, error)
        saveFilteredImageDataState.postValue(dataState)
    }

    data class SaveFilteredImageState(
        val isLoading: Boolean,
        val uri: Uri?,
        val error: String?
    )

    private fun addToGallery(context: Context, savedImageUri: Uri) {
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        mediaScanIntent.data = savedImageUri
        context.sendBroadcast(mediaScanIntent)
    }

    //endregion
}