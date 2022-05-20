package submission.dicoding.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import submission.dicoding.R
import submission.dicoding.network.ListStoryItem

// File ini berisi Adapter untuk Recycler View

class StoryAdapter(private val listStory: List<ListStoryItem?>?) : RecyclerView.Adapter<StoryAdapter.ViewHolder>(){
    private lateinit var onItemClickCallback: OnItemClickCallback

    fun setOnItemClickCallback(onItemClickCallback: OnItemClickCallback){
        this.onItemClickCallback = onItemClickCallback
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgvAvatar: ImageView = itemView.findViewById(R.id.imgv_avatar)
        val tvName: TextView = itemView.findViewById(R.id.tv_story_name)
        val tvDesc: TextView = itemView.findViewById(R.id.tv_story_desc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_story, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(holder.itemView.context)
            .load(listStory?.get(position)?.photoUrl)
            .into(holder.imgvAvatar)
        holder.tvName.text = listStory?.get(position)?.name
        holder.tvDesc.text = listStory?.get(position)?.description

        holder.itemView.setOnClickListener {
            onItemClickCallback.onItemClicked(listStory?.get(holder.adapterPosition))
        }
    }

    override fun getItemCount(): Int = listStory?.size ?:0

    interface OnItemClickCallback{
        fun onItemClicked(data: ListStoryItem?)
    }

}