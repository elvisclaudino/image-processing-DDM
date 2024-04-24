package com.example.imageprocessing.activities.main

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.imageprocessing.activities.editimage.EditImageActivity
import com.example.imageprocessing.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_CODE_PICK_IMAGE = 1
        const val REQUEST_IMAGE_CAPTURE = 2
        const val KEY_IMAGE_URI = "imageUri"
    }

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setListeners()
    }

    private fun setListeners() {
        binding.buttonEditNewImage.setOnClickListener {
            openGallery()
        }

        binding.buttonCamera.setOnClickListener {
            captureImage()
        }
    }

    private fun openGallery() {
        Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        ).also { pickerIntent ->
            pickerIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivityForResult(pickerIntent, REQUEST_CODE_PICK_IMAGE)
        }
    }

    private fun captureImage() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (captureIntent.resolveActivity(packageManager) != null) {
                startActivityForResult(captureIntent, REQUEST_IMAGE_CAPTURE)
            } else {
                Log.d("permissão", "Permissão negada")
            }
        } else {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), REQUEST_IMAGE_CAPTURE)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_PICK_IMAGE -> {
                    data?.data?.let { imageUri ->
                        startEditImageActivity(imageUri)
                    }
                }
                REQUEST_IMAGE_CAPTURE -> {
                    data?.extras?.get("data")?.let { image ->
                        val imageUri = saveImageToGallery(image as Bitmap)
                        startEditImageActivity(imageUri)
                    }
                }
            }
        }
    }

    private fun startEditImageActivity(imageUri: Uri) {
        Intent(applicationContext, EditImageActivity::class.java).also { editImageIntent ->
            editImageIntent.putExtra(KEY_IMAGE_URI, imageUri)
            startActivity(editImageIntent)
        }
    }

    private fun saveImageToGallery(image: Bitmap): Uri {
        val savedImageUri: Uri
        val imageFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "${System.currentTimeMillis()}.jpg"
        )
        try {
            FileOutputStream(imageFile).use { fos ->
                image.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                fos.flush()
            }
            savedImageUri = Uri.fromFile(imageFile)
        } catch (e: IOException) {
            throw RuntimeException("Error saving image to gallery: ${e.message}")
        }

        return savedImageUri
    }

}