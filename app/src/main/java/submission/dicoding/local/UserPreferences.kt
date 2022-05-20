package submission.dicoding.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// File ini berisi pengaturan DataStore untuk menyimpan Token Akses

class UserPreferences private constructor (private val dataStore: DataStore<Preferences>) {

    companion object{
        @Volatile
        private var INSTANCE: UserPreferences? = null

        private val TOKEN_KEY = stringPreferencesKey("token")

        fun getInstance(dataStore: DataStore<Preferences>): UserPreferences{
            return INSTANCE ?: synchronized(this){
                val instance = UserPreferences(dataStore)
                INSTANCE = instance
                instance
            }
        }
    }

    fun getTokenKey(): Flow<String>{
        return dataStore.data.map{
            it[TOKEN_KEY] ?: ""
        }
    }

    suspend fun saveTokenKey(token: String){
        dataStore.edit{
            it[TOKEN_KEY] = token
        }
    }

    suspend fun deleteTokenKey(){
        dataStore.edit{
            it[TOKEN_KEY] = ""
        }
    }
}