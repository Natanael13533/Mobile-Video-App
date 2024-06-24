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
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.shortclip.adapter.ProfileVideoAdapter
import com.example.shortclip.databinding.ActivityProfileBinding
import com.example.shortclip.model.UserModel
import com.example.shortclip.model.VideoModel
import com.example.shortclip.util.UiUtil
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var profileUserId: String
    private lateinit var currentUserId: String

    private lateinit var profileUserModel: UserModel

    private lateinit var photoLauncher: ActivityResultLauncher<Intent>

    private lateinit var adapter : ProfileVideoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        navbar()

        profileUserId = intent.getStringExtra("profile_user_id")!!
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid!!

        photoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if(result.resultCode == RESULT_OK) {
                //upload photo
                uploadToFirestore(result.data?.data!!)
            }
        }

        if(profileUserId == currentUserId) {
            // current user profile
            binding.profileBtn.text = resources.getString(R.string.logout)
            binding.profileBtn.setOnClickListener {
                logout()
            }
            binding.profilePic.setOnClickListener {
                checkPermissionAndPickPhoto()
            }
        }
        else {
            //other user profile
            binding.profileBtn.text = resources.getString(R.string.follow)
            binding.profileBtn.setOnClickListener {
                followUnfollowUser()
            }
        }

        getProfileDataFromFirebase()
        setupRecyclerView()
    }

//    private fun navbar() {
//        binding.bottomNavBar.setOnItemSelectedListener { menuItem ->
//            when(menuItem.itemId) {
//                R.id.bottom_menu_home -> {
//                    startActivity(Intent(this, MainActivity::class.java))
//                    finish()
//                }
//                R.id.bottom_menu_add_video -> {
//                    startActivity(Intent(this, VideoUploadActivity::class.java))
//                    finish()
//                }
//                R.id.bottom_menu_profile -> {
//                    UiUtil.showToast(applicationContext, "Profile")
//                }
//            }
//            false
//        }
//    }

    private fun followUnfollowUser() {
        Firebase.firestore.collection("users")
            .document(currentUserId)
            .get()
            .addOnSuccessListener {
                val currentUserModel = it.toObject(UserModel::class.java)!!

                if(profileUserModel.followerList.contains(currentUserId)) {
                    //unfollow user
                    profileUserModel.followerList.remove(currentUserId)
                    currentUserModel.followingList.remove(profileUserId)

                    binding.profileBtn.text = resources.getString(R.string.follow)
                }
                else {
                    //follow user
                    profileUserModel.followerList.add(currentUserId)
                    currentUserModel.followingList.add(profileUserId)

                    binding.profileBtn.text = resources.getString(R.string.unfollow)
                }

                updateUserData(profileUserModel)
                updateUserData(currentUserModel)
            }
    }

    private fun updateUserData(model: UserModel) {
        Firebase.firestore.collection("users")
            .document(model.id)
            .set(model)
            .addOnSuccessListener {
                getProfileDataFromFirebase()
            }
    }

    private fun uploadToFirestore(photoUri: Uri) {
        binding.progressBar.visibility = View.VISIBLE

        val photoRef = FirebaseStorage.getInstance()
            .reference
            .child("profilePic/" + currentUserId)

        photoRef.putFile(photoUri)
            .addOnSuccessListener {
                photoRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    postToFirestore(downloadUrl.toString())
                }
            }
    }

    private fun postToFirestore(url: String) {
        Firebase.firestore.collection("users")
            .document(currentUserId)
            .update("profilePic", url)
            .addOnSuccessListener {
                getProfileDataFromFirebase()
            }
    }

    private fun checkPermissionAndPickPhoto() {
        var readExternalPhoto: String = ""

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            readExternalPhoto = android.Manifest.permission.READ_MEDIA_IMAGES
        }
        else {
            readExternalPhoto = android.Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if(ContextCompat.checkSelfPermission(this, readExternalPhoto) == PackageManager.PERMISSION_GRANTED) {
            //we have permission
            openPhotoPicker()
        }
        else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(readExternalPhoto), 100
            )
        }
    }

    private fun openPhotoPicker() {
        var intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        photoLauncher.launch(intent)
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun getProfileDataFromFirebase() {
        Firebase.firestore.collection("users")
            .document(profileUserId)
            .get()
            .addOnSuccessListener {
                profileUserModel = it.toObject(UserModel::class.java)!!
                setUI()
            }
    }

    private fun setUI() {
        profileUserModel.apply {
            Glide.with(binding.profilePic).load(profilePic)
                .circleCrop()
                .apply(RequestOptions().placeholder(R.drawable.icon_account_circle))
                .into(binding.profilePic)

            binding.profileUsername.text = "@" + username
            if (profileUserModel.followerList.contains(currentUserId))
                binding.profileBtn.text = resources.getString(R.string.unfollow)
            binding.progressBar.visibility = View.GONE
            binding.followingCount.text = followingList.size.toString()
            binding.followerCount.text = followerList.size.toString()

            Firebase.firestore.collection("videos")
                .whereEqualTo("uploaderId", profileUserId)
                .get()
                .addOnSuccessListener {
                    binding.postCount.text = it.size().toString()
                }
        }
    }

    private fun setupRecyclerView() {
        val options = FirestoreRecyclerOptions.Builder<VideoModel>()
            .setQuery(
                Firebase.firestore.collection("videos")
                    .whereEqualTo("uploaderId", profileUserId)
                    .orderBy("createdTime", Query.Direction.DESCENDING),
                    VideoModel::class.java
            ).build()
        adapter = ProfileVideoAdapter(options)
        binding.recyclerView.layoutManager = GridLayoutManager(this, 3)
        binding.recyclerView.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        adapter.startListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        adapter.stopListening()
    }
}