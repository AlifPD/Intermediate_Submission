package submission.dicoding.local

import android.content.Context
import submission.dicoding.network.ApiConfig

object Injection {
    fun provideRepository(context: Context): StoryRepository {
        return StoryRepository(ApiConfig().getApiService())
    }
}