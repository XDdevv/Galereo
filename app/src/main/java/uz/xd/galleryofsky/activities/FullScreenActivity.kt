package uz.xd.galleryofsky.activities

import android.app.DownloadManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import uz.xd.galleryofsky.R
import uz.xd.galleryofsky.databinding.ActivityFullScreenBinding

class FullScreenActivity : AppCompatActivity() {

    private var imageUri = ""
    private lateinit var binding: ActivityFullScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        imageUri = intent.getStringExtra("imageUri").toString()

        Glide.with(binding.root)
            .load(imageUri)
            .placeholder(R.drawable.img_1)
            .into(binding.imageIv)

        binding.download.setOnClickListener {
            downloadFile(imageUri, "image_" + System.currentTimeMillis())
        }

    }

    private fun downloadFile(url: String, outputFile: String) {
        Toast.makeText(this, "Downloading started", Toast.LENGTH_SHORT).show()
        val request = DownloadManager.Request(Uri.parse(url))
        request.setTitle(outputFile)
        request.setDescription("Downloading $outputFile")
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.allowScanningByMediaScanner()
        request.setDestinationInExternalPublicDir(
            Environment.DIRECTORY_DOWNLOADS,
            outputFile
        )
        val manager =
            getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
    }
}