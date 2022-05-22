package submission.dicoding.view

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import submission.dicoding.R
import submission.dicoding.databinding.ActivityMapsBinding
import submission.dicoding.local.MainViewModel
import submission.dicoding.local.UserPreferences
import submission.dicoding.local.ViewModelFactory

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isCompassEnabled = true
        mMap.uiSettings.isMapToolbarEnabled = true

        getStoryData()
    }

    private fun getStoryData() {
        val viewModel = ViewModelProvider(
            this,
            ViewModelFactory(UserPreferences.getInstance(dataStore))
        )[MainViewModel::class.java]

        viewModel.getTokenKey().observe(this) { token ->
            viewModel.getStories(token, 20)
            viewModel.listStoryItem.observe(this) {
                if (it != null) {
                    for (i in it) {
                        val location = i?.lat?.let { it1 -> i.lon?.let { it2 -> LatLng(it1, it2) } }
                        location?.let { it1 ->
                            MarkerOptions()
                                .position(it1)
                                .title("${i.name}")
                                .snippet("${i.description}")
                        }?.let { it2 ->
                            mMap.addMarker(
                                it2
                            )
                        }
                    }
                }
            }
        }
    }
}