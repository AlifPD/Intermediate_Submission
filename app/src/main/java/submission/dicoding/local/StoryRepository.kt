package submission.dicoding.local

import androidx.lifecycle.LiveData
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.liveData
import submission.dicoding.network.ApiService
import submission.dicoding.network.ListStoryItem
import submission.dicoding.network.StoryPagingSource

class StoryRepository(private val apiService: ApiService) {
    fun getStory(token: String): LiveData<PagingData<ListStoryItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 5
            ),
            pagingSourceFactory = {
                StoryPagingSource(apiService, token)
            }
        ).liveData
    }
}