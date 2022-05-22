package submission.dicoding.view

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import submission.dicoding.R
import submission.dicoding.databinding.ItemLoadingBinding
import submission.dicoding.databinding.ItemStoryBinding
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

class PagingStoryAdapter :
    PagingDataAdapter<ListStoryItem, PagingStoryAdapter.MyViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = ItemStoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val data = getItem(position)
        if (data != null) {
            holder.bind(data)
        }
    }

    class MyViewHolder(private val binding: ItemStoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(data: ListStoryItem) {
            Glide.with(binding.root.context)
                .load(data.photoUrl)
                .into(binding.imgvAvatar)
            binding.tvStoryName.text = data.name
            binding.tvStoryDesc.text = data.description

            binding.root.setOnClickListener {
                val toDetailIntent = Intent(binding.root.context, DetailStoryActivity::class.java).also{
                    it.putExtra("DATA", data)
                    Log.d("PagingStoryAdapter", "bind: $data")
                }
                binding.root.context.startActivity(toDetailIntent)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ListStoryItem>() {
            override fun areItemsTheSame(oldItem: ListStoryItem, newItem: ListStoryItem): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: ListStoryItem, newItem: ListStoryItem): Boolean {
                return oldItem.name == newItem.name
            }
        }
    }
}

class LoadingStateAdapter(private val retry: () -> Unit) : LoadStateAdapter<LoadingStateAdapter.LoadingStateViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): LoadingStateViewHolder {
        val binding = ItemLoadingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LoadingStateViewHolder(binding, retry)
    }
    override fun onBindViewHolder(holder: LoadingStateViewHolder, loadState: LoadState) {
        holder.bind(loadState)
    }
    class LoadingStateViewHolder(private val binding: ItemLoadingBinding, retry: () -> Unit) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.retryButton.setOnClickListener { retry.invoke() }
        }
        fun bind(loadState: LoadState) {
            if (loadState is LoadState.Error) {
                binding.errorMsg.text = loadState.error.localizedMessage
            }
            binding.progressBar.isVisible = loadState is LoadState.Loading
            binding.retryButton.isVisible = loadState is LoadState.Error
            binding.errorMsg.isVisible = loadState is LoadState.Error
        }
    }
}