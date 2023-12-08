package uz.xd.galleryofsky.activities

import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import uz.xd.galleryofsky.models.ModelImagePicked
import uz.xd.galleryofsky.models.Story
import uz.xd.galleryofsky.adapters.AdapterStory
import uz.xd.galleryofsky.databinding.ActivityMainBinding
import uz.xd.galleryofsky.databinding.ItemPassBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var imagePickedArrayList: ArrayList<ModelImagePicked>
    private lateinit var storyArrayList: ArrayList<Story>

    private lateinit var adapterStory: AdapterStory

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    private lateinit var progressDialog: ProgressDialog

    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please Wait...")
        progressDialog.setCanceledOnTouchOutside(false)

        sharedPreferences = getSharedPreferences("shared_prefences", MODE_PRIVATE)
        editor = sharedPreferences.edit()

        loadImages()

        if (sharedPreferences.getBoolean("isAdmin", true)) {
            binding.add.visibility = View.VISIBLE
        } else if (sharedPreferences.getBoolean("isAdmin", false)) {
            binding.add.visibility = View.GONE
        }

        binding.add.setOnClickListener {
            showImagePickOptions()
        }

        binding.title.setOnLongClickListener {
            askPassword()
            true
        }
    }

    private fun askPassword() {
        val dialog = AlertDialog.Builder(this).create()
        val itemDialogEditBinding = ItemPassBinding.inflate(layoutInflater)
        dialog.setView(itemDialogEditBinding.root)

        itemDialogEditBinding.apply {
            val password = "123123456456"
            btnSave.setOnClickListener {
                val pass = itemDialogEditBinding.etPassword.text.toString().trim()

                if (pass == password) {
                    Toast.makeText(this@MainActivity, "You are admin", Toast.LENGTH_SHORT)
                        .show()

                    editor.putBoolean("isAdmin", true)
                    editor.commit()

                    binding.add.visibility = View.VISIBLE
                    adapterStory.notifyDataSetChanged()
                    dialog.dismiss()

                } else {
                    Toast.makeText(this@MainActivity, "Password is wrong", Toast.LENGTH_SHORT)
                        .show()
                    binding.add.visibility = View.GONE

                    editor.putBoolean("isAdmin", false)
                    editor.commit()

                    adapterStory.notifyDataSetChanged()
                }
            }
        }

        dialog.show()
    }

    private fun loadImages() {
        storyArrayList = ArrayList()
        val ref = FirebaseDatabase.getInstance().getReference("AllImages")
        ref.child("Images").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                storyArrayList.clear()
                for (ds in snapshot.children) {
                    try {
                        val modelAd = ds.getValue(Story::class.java)
                        storyArrayList.add(modelAd!!)

                        adapterStory = AdapterStory(this@MainActivity, storyArrayList,
                            object : AdapterStory.CallBack {
                                override fun deletedImage(position: Int) {
                                    progressDialog.setMessage("Deleting image...")
                                    progressDialog.dismiss()
                                    FirebaseDatabase.getInstance().getReference("AllImages")
                                        .child("Images")
                                        .child("${storyArrayList[position].id}")
                                        .removeValue()
                                        .addOnSuccessListener {
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Deleted Successfully",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            adapterStory.notifyItemRemoved(position)
                                            progressDialog.dismiss()
                                        }.addOnFailureListener {
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Failed to delete image due to ${it.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            progressDialog.dismiss()

                                        }

                                }

                            })
                        binding.imagesRv.adapter = adapterStory

                    } catch (e: Exception) {

                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun showImagePickOptions() {
        val popupMenu = PopupMenu(this, binding.add)
        imagePickedArrayList = ArrayList()

        popupMenu.menu.add(Menu.NONE, 1, 1, "Camera")
        popupMenu.menu.add(Menu.NONE, 2, 2, "Gallery")

        popupMenu.show()

        popupMenu.setOnMenuItemClickListener { item ->
            val itemId = item.itemId
            if (itemId == 1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestCameraPermissions.launch(arrayOf(android.Manifest.permission.CAMERA))
                } else {
                    requestCameraPermissions.launch(
                        arrayOf(
                            android.Manifest.permission.CAMERA,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    )
                }
            } else if (itemId == 2) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pickImageGallery()
                } else {
                    requestStoragePermission.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
            true
        }
    }

    private val requestCameraPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->

            var areAllGranted = true
            for (isGranted in result.values) {
                areAllGranted = areAllGranted && isGranted
            }
            if (areAllGranted) {
                pickImageCamera()
            } else {
                Toast.makeText(
                    this,
                    "Camera or Storage or both permissions denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private val requestStoragePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                pickImageGallery()
            } else {
                Toast.makeText(this, "Storage permission is denied", Toast.LENGTH_SHORT).show()
            }
        };

    private fun pickImageCamera() {
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, "Temp_image_title")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Temp_image_DESCRIPTION")

        imageUri =
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraActivityResultLauncherLauncher.launch(intent)

    }

    private val cameraActivityResultLauncherLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                try {
                    val timestamp = "${System.currentTimeMillis()}"
                    val modelImagePicked = ModelImagePicked(timestamp, imageUri, null, false)
                    imagePickedArrayList.add(modelImagePicked)
                    uploadImageStorage()
                } catch (e: Exception) {

                }
            } else {

            }
        }

    private fun pickImageGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
    }

    private val galleryActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                imageUri = data!!.data
                val timestamp = "${System.currentTimeMillis()}"
                val modelImagePicked = ModelImagePicked(timestamp, imageUri, null, false)
                imagePickedArrayList.add(modelImagePicked)
                uploadImageStorage()
                try {

                } catch (e: Exception) {
                }
            }
        }

    private fun uploadImageStorage() {
        for (i in imagePickedArrayList.indices) {
            val modelImagePicked = imagePickedArrayList[i]

            if (!modelImagePicked.fromInternet) {
                val imageName = modelImagePicked.id
                val filePathAndName = "AllImages/$imageName"
                val imageIndexForProgress = i + 1

                val storageReference = FirebaseStorage.getInstance().getReference(filePathAndName)
                storageReference.putFile(modelImagePicked.imageUri!!)
                    .addOnProgressListener { snapshot ->
                        val progress = 100.0 * snapshot.bytesTransferred / snapshot.totalByteCount
                        val message =
                            "Uploading $imageIndexForProgress of ${imagePickedArrayList.size} images..." +
                                    " Progress ${progress.toInt()}"
                        Toast.makeText(this@MainActivity, "$message", Toast.LENGTH_SHORT).show()
                    }.addOnSuccessListener { taskSnapshot ->
                        val uriTask = taskSnapshot.storage.downloadUrl
                        while (!uriTask.isSuccessful) {

                        };

                        val uploadedImageUrl = uriTask.result

                        if (uriTask.isSuccessful) {
                            val hashMap = HashMap<String, Any>()
                            hashMap["id"] = "${modelImagePicked.id}"
                            hashMap["imageUrl"] = "$uploadedImageUrl"

                            val ref = FirebaseDatabase.getInstance().getReference("AllImages")
                            ref.child("Images")
                                .child(imageName)
                                .updateChildren(hashMap)

                            Toast.makeText(this@MainActivity, "Image Posted", Toast.LENGTH_SHORT)
                                .show()
                        }

                    }.addOnFailureListener {
                    }
            } else {

            }

        }
    }

}