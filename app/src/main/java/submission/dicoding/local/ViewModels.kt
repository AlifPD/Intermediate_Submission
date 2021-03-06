package submission.dicoding.local

import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import submission.dicoding.network.*

// File ini berisi semua ViewModel yang diperlukan oleh App

class MainViewModel(private val pref: UserPreferences) : ViewModel() {
    private var _registerResponse = MutableLiveData<RegisterResponses>()
    val registerResponse: LiveData<RegisterResponses> = _registerResponse

    private var _loginResponse = MutableLiveData<LoginResponse>()
    val loginResponse: LiveData<LoginResponse> = _loginResponse

    private var _listStoryItem = MutableLiveData<List<ListStoryItem?>?>()
    val listStoryItem: MutableLiveData<List<ListStoryItem?>?> = _listStoryItem

    private var _uploadStoryResponse = MutableLiveData<UploadStoryResponse>()
    val uploadStoryResponse: LiveData<UploadStoryResponse> = _uploadStoryResponse

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun getTokenKey(): LiveData<String>{
        return pref.getTokenKey().asLiveData()
    }

    fun saveTokenKey(token:String){
        viewModelScope.launch {
            pref.saveTokenKey(token)
        }
    }

    fun deleteTokenKey(){
        viewModelScope.launch{
            pref.deleteTokenKey()
        }
    }

    fun registerUser(name: String, email: String, password: String){
        _isLoading.value = true
        val clientRegister = ApiConfig().getApiService().registerUser(name, email, password)
        clientRegister.enqueue(object : Callback<RegisterResponses> {
            override fun onResponse(
                call: Call<RegisterResponses>,
                response: Response<RegisterResponses>
            ) {
                _isLoading.value = false
                if(response.isSuccessful){
                    _registerResponse.value = response.body()
                    Log.d("RegisterViewModel", "onResponseSuccess: Error= ${response.body()?.error}, Message= ${response.body()?.message}")
                }else{
                    _isLoading.value = false
                    Log.d("RegisterViewModel", "onResponseFailure: ${response.body()?.message}")
                }
            }

            override fun onFailure(call: Call<RegisterResponses>, t: Throwable) {
                _isLoading.value = false
                Log.e("RegisterViewModel", "onFailure: ${t.message}")
            }
        })
    }

    fun loginUser(email: String, password: String){
        _isLoading.value = true
        val clientLogin = ApiConfig().getApiService().loginUser(email, password)
        clientLogin.enqueue(object : Callback<LoginResponse>{
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if(response.isSuccessful){
                    _loginResponse.value = response.body()
                    _isLoading.value = false
                    saveTokenKey(response.body()?.loginResult?.token.toString())
                    Log.d("MainViewModel", "onResponseSuccess: Error= ${response.body()?.error}, Message= ${response.body()?.message}")
                }else{
                    _isLoading.value = false
                    Log.d("MainViewModel", "onResponseFailure: ${response.body()?.message}")
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                _isLoading.value = false
                Log.e("MainViewModel", "onFailure: ${t.message}")
            }
        })
    }

    fun getStories(token: String, size: Int){
        _isLoading.value = true
        val clientGetStory = ApiConfig().getApiService().getStories("Bearer $token", size, 1)
        clientGetStory.enqueue(object : Callback<GetStoryResponse>{
            override fun onResponse(
                call: Call<GetStoryResponse>,
                response: Response<GetStoryResponse>
            ) {
                if(response.isSuccessful){
                    _isLoading.value = false
                    _listStoryItem.value = response.body()?.listStory
                    Log.d("MainViewModel", "onResponseSuccess: Error= ${response.body()?.error}, Message= ${response.body()?.message}")
                }else{
                    _isLoading.value = false
                    Log.d("MainViewModel", "onResponseFailure: ${response.body()?.message}")
                }
            }

            override fun onFailure(call: Call<GetStoryResponse>, t: Throwable) {
                _isLoading.value = false
                Log.e("MainViewModel", "onFailure: ${t.message}")
            }

        })
    }

    fun uploadStory(image: MultipartBody.Part, description: RequestBody, token: String){
        _isLoading.value = true
        val uploadStoryClient = ApiConfig().getApiService().uploadStory("Bearer $token", description, image)
        uploadStoryClient.enqueue(object : Callback<UploadStoryResponse>{
            override fun onResponse(
                call: Call<UploadStoryResponse>,
                response: Response<UploadStoryResponse>
            ) {
                if(response.isSuccessful){
                    _isLoading.value = false
                    _uploadStoryResponse.value = response.body()
                    Log.d("MainViewModel", "onResponseSuccess: Error= ${response.body()?.error}, Message= ${response.body()?.message}")
                }else{
                    _isLoading.value = false
                    Log.d("MainViewModel", "onResponseFailure: Error= ${response.body()?.error}, Message= ${response.body()?.message}")
                }
            }

            override fun onFailure(call: Call<UploadStoryResponse>, t: Throwable) {
                _isLoading.value = false
                Log.e("MainViewModel", "onFailure: ${t.message}")
            }

        })
    }

}

class ViewModelFactory(private val pref: UserPreferences): ViewModelProvider.NewInstanceFactory(){
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(MainViewModel::class.java)){
            return MainViewModel(pref) as T
        }
        throw IllegalArgumentException("Unknown ViewModel Class: "+ modelClass.name)
    }
}

class PagingMainViewModel(private val storyRepository: StoryRepository) : ViewModel() {
    fun story(token: String): LiveData<PagingData<ListStoryItem>> =
        storyRepository.getStory(token).cachedIn(viewModelScope)
}

class PagingViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PagingMainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PagingMainViewModel(Injection.provideRepository(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}