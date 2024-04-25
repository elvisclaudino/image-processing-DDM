package com.example.imageprocessing.activities.editimage

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.example.imageprocessing.activities.filteredimage.FilteredImageActivity
import com.example.imageprocessing.activities.main.MainActivity
import com.example.imageprocessing.adapters.ImageFiltersAdapter
import com.example.imageprocessing.data.ImageFilter
import com.example.imageprocessing.databinding.ActivityEditImageBinding
import com.example.imageprocessing.listeners.ImageFilterListener
import com.example.imageprocessing.utilities.displayToast
import com.example.imageprocessing.utilities.show
import com.example.imageprocessing.viewmodels.EditImageViewModel
import jp.co.cyberagent.android.gpuimage.GPUImage
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

class EditImageActivity : AppCompatActivity(), ImageFilterListener {

    companion object {
        const val KEY_FILTERED_IMAGE_URI = "filteredImageUri"
    }

    private lateinit var binding: ActivityEditImageBinding
    private val viewModel:EditImageViewModel by viewModel()
    private lateinit var gpuImage: GPUImage

    // Image Bitmaps
    private lateinit var originalBitmap: Bitmap
    private var filteredBitmap = MutableLiveData<Bitmap>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditImageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setListeners()
        setupObservers()
        prepareImagePreview()
    }

    private fun setupObservers() {
        viewModel.imagePreviewUiState.observe(this) { dtState ->
            val dataState = dtState ?: return@observe
            binding.previewProgressBar.visibility =
                if(dataState.isLoading) View.VISIBLE else View.GONE
            dataState.bitmap?.let { bitmap ->
                // For the first time "filtered image = original image"
                originalBitmap = bitmap
                filteredBitmap.value = bitmap

                with(originalBitmap) {
                    gpuImage.setImage(this)
                    binding.imagePreview.show()
                    viewModel.loadImageFilters(this)
                }
            } ?: kotlin.run {
                dataState.error?.let { error ->
                    displayToast(error)
                }
            }
        }
        viewModel.imageFilterUiState.observe(this) { imgFiltersDataState ->
            val imageFiltersDataState = imgFiltersDataState ?: return@observe
            binding.imageFiltersProgressBar.visibility =
                if (imageFiltersDataState.isLoading) View.VISIBLE else View.GONE
            imageFiltersDataState.imageFilters?.let { imageFilters ->
                ImageFiltersAdapter(imageFilters, this).also { adapter ->
                    binding.filtersRecyclerView.adapter = adapter
                }
            } ?: kotlin.run {
                imageFiltersDataState.error?.let { error ->
                    displayToast(error)
                }
            }
        }
        filteredBitmap.observe(this) {bitmap ->
            binding.imagePreview.setImageBitmap(bitmap)
        }

        viewModel.saveFilteredImageUiState.observe(this) { saveFilteredImgDataState ->
            val saveFilteredImageDataState = saveFilteredImgDataState ?: return@observe
            if (saveFilteredImageDataState.isLoading) {
                binding.imageSave.visibility = View.GONE
                binding.savingProgressBar.visibility = View.VISIBLE
            } else {
                binding.savingProgressBar.visibility = View.GONE
                binding.imageSave.visibility = View.VISIBLE
            }
            saveFilteredImageDataState.uri?.let { savedImageUri ->
                Intent(
                    applicationContext,
                    FilteredImageActivity::class.java
                ).also { filteredImageIntent ->
                    filteredImageIntent.putExtra(KEY_FILTERED_IMAGE_URI, savedImageUri)
                    startActivity(filteredImageIntent)
                }
            } ?: kotlin.run {
                saveFilteredImageDataState.error?.let { error ->
                    displayToast(error)
                }
            }
        }
    }

    private fun prepareImagePreview() {
        gpuImage = GPUImage(applicationContext)
        intent.getParcelableExtra<Uri>(MainActivity.KEY_IMAGE_URI)?.let { imageUri ->
            viewModel.prepareImagePreview(imageUri)
        }
    }

    private fun setListeners() {
        binding.imageBack.setOnClickListener {
            onBackPressed()
        }

            binding.imageSave.setOnClickListener{
                filteredBitmap.value?.let { bitmap ->
                    saveImageToGallery(bitmap, this)
                    viewModel.saveFilteredimage(this, bitmap)
                }
            }

        // This will show origin image when long click the ImageView
        binding.imagePreview.setOnLongClickListener{
            binding.imagePreview.setImageBitmap(originalBitmap)
            return@setOnLongClickListener false
        }
        binding.imagePreview.setOnClickListener {
            binding.imagePreview.setImageBitmap(filteredBitmap.value)
        }
    }

    override fun onFilterSelected(imageFilter: ImageFilter) {
        with(imageFilter) {
            with(gpuImage) {
                setFilter(filter)
                filteredBitmap.value = bitmapWithFilterApplied
            }
        }
    }

    // Dentro da sua classe EditImageActivity
    private fun saveImageToGallery(bitmap: Bitmap, context: Context): Uri? {
        // Verifica se a permissão de escrita externa foi concedida
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "${UUID.randomUUID()}.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            imageUri?.let { uri ->
                val outputStream: OutputStream? = resolver.openOutputStream(uri)
                outputStream?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                    Toast.makeText(context, "Imagem salva com sucesso na galeria", Toast.LENGTH_SHORT).show()
                    return uri
                }
            }
        } else {
            val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val fileName = "${UUID.randomUUID()}.jpg"
            val file = File(directory, fileName)
            try {
                val fileOutputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
                fileOutputStream.flush()
                fileOutputStream.close()
                MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
                Toast.makeText(context, "Imagem salva com sucesso na galeria", Toast.LENGTH_SHORT).show()
                return Uri.fromFile(file)
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(context, "Erro ao salvar imagem na galeria", Toast.LENGTH_SHORT).show()
            }
        }
        return null
    }

}