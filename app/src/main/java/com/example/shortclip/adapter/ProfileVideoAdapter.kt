package com.example.shortclip.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.shortclip.SingleVideoPlayerActivity
import com.example.shortclip.databinding.ProfileVideoItemRowBinding
import com.example.shortclip.model.VideoModel
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions

class ProfileVideoAdapter(options : FirestoreRecyclerOptions<VideoModel>)
    : FirestoreRecyclerAdapter<VideoModel, ProfileVideoAdapter.VideoViewHolder>(options)
{

    inner class VideoViewHolder(private val binding: ProfileVideoItemRowBinding)  : RecyclerView.ViewHolder(binding.root) {

        fun bindVideo(video : VideoModel) {
            Glide.with(binding.thubmnailImageView)
                .load(video.url)
                .into(binding.thubmnailImageView)

            binding.thubmnailImageView.setOnClickListener {
                val intent = Intent(binding.thubmnailImageView.context, SingleVideoPlayerActivity::class.java)
                intent.putExtra("videoId", video.videoId)
                binding.thubmnailImageView.context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ProfileVideoItemRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int, model: VideoModel) {
        holder.bindVideo(model)
    }

}