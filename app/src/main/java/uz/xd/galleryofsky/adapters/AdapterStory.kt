package uz.xd.galleryofsky.adapters

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import uz.xd.galleryofsky.activities.FullScreenActivity
import uz.xd.galleryofsky.R
import uz.xd.galleryofsky.models.Story
import uz.xd.galleryofsky.databinding.ItemStoryBinding

class AdapterStory : RecyclerView.Adapter<AdapterStory.HolderAd> {

    private lateinit var binding: ItemStoryBinding
    private lateinit var sharedPreferences: SharedPreferences
    private var context: Context
    private var adArrayList: ArrayList<Story>
    private var callBack: CallBack
    private var firebaseAuth: FirebaseAuth

    constructor(context: Context, adArrayList: ArrayList<Story>, callBack: CallBack) {
        this.context = context
        this.adArrayList = adArrayList
        this.callBack = callBack

        firebaseAuth = FirebaseAuth.getInstance()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderAd {
        binding = ItemStoryBinding.inflate(LayoutInflater.from(context), parent, false)
        return HolderAd(binding.root)
    }

    override fun getItemCount(): Int {
        return adArrayList.size
    }

    override fun onBindViewHolder(holder: HolderAd, position: Int) {

        Glide.with(context)
            .load(adArrayList[position].imageUrl)
            .placeholder(R.drawable.img_1)
            .into(holder.imageTv)

        holder.itemView.setOnClickListener {
            val intent = Intent(context, FullScreenActivity::class.java)
            intent.putExtra("imageUri", "${adArrayList[position].imageUrl}")
            context.startActivity(intent)
        }
        sharedPreferences = context.getSharedPreferences("shared_prefences", MODE_PRIVATE)

        val isAdmin : Boolean = sharedPreferences.getBoolean("isAdmin", false)

        if (isAdmin) {
            holder.deleteIv.visibility = View.VISIBLE
        }else if (!isAdmin) {
            holder.deleteIv.visibility = View.GONE
        }
        holder.deleteIv.setOnClickListener {
            callBack.deletedImage(position)
        }
    }


    inner class HolderAd(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imageTv = binding.image
        var deleteIv = binding.delete
    }

    interface CallBack {
        fun deletedImage(position: Int)
    }
}