package com.example.shortclip

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.shortclip.databinding.ActivityVideoUploadBinding
import com.example.shortclip.model.VideoModel
import com.example.shortclip.util.UiUtil
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage

class VideoUploadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoUploadBinding
    private var selectedVideoUri: Uri? = null
    private lateinit var videoLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        videoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {result ->
            if(result.resultCode == RESULT_OK) {
                selectedVideoUri = result.data?.data
                showPostView()
            }
        }

        binding.uploadView.setOnClickListener {
            checkPermissionAndOpenVideoPicker()
        }

        binding.submitPostButton.setOnClickListener {
            postVideo()
        }

        binding.cancelPostButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun postVideo() {
        if(binding.postCaptionInput.text.toString().isEmpty()) {
            binding.postCaptionInput.setError("this text field is required")
            return
        }
        setInProgress(true)

        selectedVideoUri?.apply {
            //store in firebase cloud storage
            val videoRef = FirebaseStorage.getInstance()
                .reference
                .child("Videos/" + this.lastPathSegment)

            videoRef.putFile(this)
                .addOnSuccessListener {
                    videoRef.downloadUrl.addOnSuccessListener { downloadUrl->
                        //video model store in firebase firestore
                        postToFirestore(downloadUrl.toString())

                        startActivity(Intent(applicationContext, MainActivity::class.java))
                    }
                }
        }
    }

    private fun postToFirestore(url: String) {
        val videoModel = VideoModel(
            FirebaseAuth.getInstance().currentUser?.uid!! + "_" + Timestamp.now().toString(),
            binding.postCaptionInput.text.toString(),
            url,
            FirebaseAuth.getInstance().currentUser?.uid!!,
            Timestamp.now()
        )

        Firebase.firestore.collection("videos")
            .document(videoModel.videoId)
            .set(videoModel)
            .addOnSuccessListener {
                setInProgress(false)
                UiUtil.showToast(applicationContext, "Video Uploaded")
                finish()
            }
            .addOnFailureListener {
                UiUtil.showToast(applicationContext, "Failed to upload")
                setInProgress(false)
            }
    }

    private fun setInProgress(inProgress: Boolean) {
        if(inProgress) {
            binding.progressBar.visibility = View.VISIBLE
            binding.submitPostButton.visibility = View.GONE
        }
        else {
            binding.progressBar.visibility = View.GONE
            binding.submitPostButton.visibility = View.VISIBLE
        }
    }

    private fun showPostView() {
        selectedVideoUri?.let {
            binding.postView.visibility = View.VISIBLE
            binding.uploadView.visibility = View.GONE

            Glide.with(binding.postThubmnailView).load(it).into(binding.postThubmnailView)
        }
    }

    private fun checkPermissionAndOpenVideoPicker() {
        var readExternalVideo: String = ""

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            readExternalVideo = android.Manifest.permission.READ_MEDIA_VIDEO
        }
        else {
            readExternalVideo = android.Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if(ContextCompat.checkSelfPermission(this, readExternalVideo) == PackageManager.PERMISSION_GRANTED) {
            // we have permission
            openVideoPicker()
        }
        else {
            // request permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(readExternalVideo),100
            )
        }
    }

    private fun openVideoPicker() {
        var intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        intent.type = "video/*"
        videoLauncher.launch(intent)
    }
}