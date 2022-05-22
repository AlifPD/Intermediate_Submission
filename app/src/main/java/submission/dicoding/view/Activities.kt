package submission.dicoding.view

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_GET_CONTENT
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import submission.dicoding.R
import submission.dicoding.databinding.*
import submission.dicoding.helper.createFile
import submission.dicoding.helper.reduceImageFile
import submission.dicoding.helper.rotateBitmap
import submission.dicoding.helper.uriToFile
import submission.dicoding.local.*
import submission.dicoding.network.ListStoryItem
import submission.dicoding.view.AddStoryActivity.Companion.CAMERA_X_RESULT
import java.io.*

// File ini berisi semua Activity yang ada di App

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private val pagingMainViewModel: PagingMainViewModel by viewModels {
        PagingViewModelFactory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(
            this,
            ViewModelFactory(UserPreferences.getInstance(dataStore))
        )[MainViewModel::class.java]

        viewModel.isLoading.observe(this){
            showLoading(it)
        }

        val isJustLogin = intent.getBooleanExtra(LoginActivity.LOGINFLAG, false)
        if (!isJustLogin) {
            viewModel.getTokenKey().observe(this) { token ->
                if(token.isEmpty()) {
                    getData()
                    startActivity(Intent(this, WelcomeActivity::class.java))
                    finish()
                }else{
                    getData()
                }
            }
        }else {
            viewModel.getTokenKey().observe(this) {
                getData()
            }
        }

        binding.logoutButton.setOnClickListener {
            viewModel.deleteTokenKey()
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }
        binding.addStoryButton.setOnClickListener {
            startActivity(Intent(this, AddStoryActivity::class.java))
        }
        binding.mapButton.setOnClickListener {
            startActivity(Intent(this, MapsActivity::class.java))
        }
    }

    private fun showStories(list: List<ListStoryItem?>?) {
        binding.rvStories.layoutManager = LinearLayoutManager(this)

        val storyAdapter = StoryAdapter(list)
        binding.rvStories.adapter = storyAdapter

        storyAdapter.setOnItemClickCallback(object : StoryAdapter.OnItemClickCallback {
            override fun onItemClicked(data: ListStoryItem?) {
                val toDetailIntent = Intent(this@MainActivity, DetailStoryActivity::class.java)
                toDetailIntent.putExtra("DATA", data)
                startActivity(toDetailIntent)
            }

        })
    }

    private fun showLoading(b: Boolean) {
        if (b) {
            binding.loadingMain.visibility = View.VISIBLE
        } else {
            binding.loadingMain.visibility = View.GONE
        }
    }

    private fun getData() {
        binding.rvStories.layoutManager = LinearLayoutManager(this)

        val pagingAdapter = PagingStoryAdapter()
        binding.rvStories.adapter = pagingAdapter.withLoadStateFooter(
            footer = LoadingStateAdapter {
                pagingAdapter.retry()
            }
        )

        viewModel.getTokenKey().observe(this){token ->
            pagingMainViewModel.story(token).observe(this) {
                pagingAdapter.submitData(lifecycle, it)
            }
        }
    }
}

class WelcomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onButtonClicked()
        playAnimation()
    }

    private fun playAnimation() {
        ObjectAnimator.ofFloat(binding.logoWelcome, View.TRANSLATION_X, -30f, 30f).apply {
            duration = 6000
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
        }.start()
        val login =
            ObjectAnimator.ofFloat(binding.toLoginButton, View.ALPHA, 1f).setDuration(1000)
        val signup =
            ObjectAnimator.ofFloat(binding.toSignupButton, View.ALPHA, 1f).setDuration(1000)
        val title =
            ObjectAnimator.ofFloat(binding.tvWelcomeDesc, View.ALPHA, 1f).setDuration(1000)

        val together = AnimatorSet().apply {
            playTogether(login, signup)
        }

        AnimatorSet().apply {
            playSequentially(title, together)
            start()
        }
    }

    private fun onButtonClicked() {
        binding.toLoginButton.setOnClickListener {
            Log.d("Welcome Activity", getString(R.string.login_button_click))
            startActivity(Intent(this, LoginActivity::class.java))
        }
        binding.toSignupButton.setOnClickListener {
            Log.d("Welcome Activity", getString(R.string.signup_button_click))
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }
}

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        playAnimation()

        val viewModel = ViewModelProvider(
            this,
            ViewModelFactory(UserPreferences.getInstance(dataStore))
        )[MainViewModel::class.java]

        viewModel.isLoading.observe(this){
            showLoading(it)
        }

        binding.signupButton.setOnClickListener {
            val inputName = binding.nameSignupEdittext.text.toString()
            val inputEmail = binding.emailSignupEdittext.text.toString()
            val inputPassword = binding.passwordSignupEdittext.text.toString()

            viewModel.registerUser(inputName, inputEmail, inputPassword)
            viewModel.registerResponse.observe(this) {
                when (it.error) {
                    true -> {
                        Toast.makeText(
                            this,
                            it.message.toString() + getString(R.string.register_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                        binding.nameSignupEdittext.text?.clear()
                        binding.emailSignupEdittext.text?.clear()
                        binding.passwordSignupEdittext.text?.clear()
                    }
                    else -> {
                        Toast.makeText(
                            this,
                            getString(R.string.register_success),
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }
            }

        }
    }

    private fun playAnimation() {
        ObjectAnimator.ofFloat(binding.logoSignup, View.TRANSLATION_X, -30f, 30f).apply {
            duration = 6000
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
        }.start()
        val desc = ObjectAnimator.ofFloat(binding.signupDesc, View.ALPHA, 1f).setDuration(750)
        val emailDesc =
            ObjectAnimator.ofFloat(binding.emailSignupDesc, View.ALPHA, 1f).setDuration(750)
        val emailInput = ObjectAnimator.ofFloat(binding.emailSignupInputlayout, View.ALPHA, 1f)
            .setDuration(750)
        val nameDesc =
            ObjectAnimator.ofFloat(binding.nameSignupDesc, View.ALPHA, 1f).setDuration(750)
        val nameInput = ObjectAnimator.ofFloat(binding.nameSingnupInputlayout, View.ALPHA, 1f)
            .setDuration(750)
        val passwordDesc =
            ObjectAnimator.ofFloat(binding.passwordSignupDesc, View.ALPHA, 1f).setDuration(750)
        val passwordInput =
            ObjectAnimator.ofFloat(binding.paswordSignupInputlayout, View.ALPHA, 1f)
                .setDuration(750)
        val button =
            ObjectAnimator.ofFloat(binding.signupButton, View.ALPHA, 1f).setDuration(750)


        val together = AnimatorSet().apply {
            playTogether(
                emailDesc,
                emailInput,
                nameDesc,
                nameInput,
                passwordDesc,
                passwordInput
            )
        }

        AnimatorSet().apply {
            playSequentially(desc, together, button)
            start()
        }
    }

    private fun showLoading(b: Boolean) {
        if (b) {
            binding.loadingSignup.visibility = View.VISIBLE
        } else {
            binding.loadingSignup.visibility = View.GONE
        }
    }
}

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        playAnimation()

        val viewModel = ViewModelProvider(
            this,
            ViewModelFactory(UserPreferences.getInstance(dataStore))
        )[MainViewModel::class.java]

        viewModel.isLoading.observe(this){
            showLoading(it)
        }

        binding.loginButton.setOnClickListener {
            val inputEmail = binding.emailLoginEdittext.text.toString()
            val inputPassword = binding.passwordLoginEdittext.text.toString()

            viewModel.loginUser(inputEmail, inputPassword)
            viewModel.loginResponse.observe(this) {
                if (it.error == true) {
                    Toast.makeText(
                        this,
                        "${it.message}, Harap Coba Lagi",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.emailLoginEdittext.text?.clear()
                    binding.passwordLoginEdittext.text?.clear()
                } else {
                    Toast.makeText(
                        this,
                        "${it.message}, Anda Berhasil Login",
                        Toast.LENGTH_SHORT
                    ).show()

                    viewModel.saveTokenKey(it.loginResult?.token.toString())
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags =
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    intent.putExtra(LOGINFLAG, true)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    private fun playAnimation() {
        ObjectAnimator.ofFloat(binding.logoLogin, View.TRANSLATION_X, -30f, 30f).apply {
            duration = 6000
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
        }.start()
        val desc = ObjectAnimator.ofFloat(binding.loginDesc, View.ALPHA, 1f).setDuration(750)
        val emailDesc =
            ObjectAnimator.ofFloat(binding.emailLoginDesc, View.ALPHA, 1f).setDuration(750)
        val emailInput = ObjectAnimator.ofFloat(binding.emailLoginInputlayout, View.ALPHA, 1f)
            .setDuration(750)
        val passwordDesc =
            ObjectAnimator.ofFloat(binding.passwordLoginDesc, View.ALPHA, 1f).setDuration(750)
        val passwordInput =
            ObjectAnimator.ofFloat(binding.passwordLoginInputlayout, View.ALPHA, 1f)
                .setDuration(750)
        val button =
            ObjectAnimator.ofFloat(binding.loginButton, View.ALPHA, 1f).setDuration(750)


        val together = AnimatorSet().apply {
            playTogether(emailDesc, emailInput, passwordDesc, passwordInput)
        }

        AnimatorSet().apply {
            playSequentially(desc, together, button)
            start()
        }
    }

    private fun showLoading(b: Boolean) {
        if (b) {
            binding.loadingLogin.visibility = View.VISIBLE
        } else {
            binding.loadingLogin.visibility = View.GONE
        }
    }

    companion object{
        const val LOGINFLAG = "LOGINFLAG"
    }
}

class DetailStoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailStoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailStoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val data = intent.getParcelableExtra<ListStoryItem?>("DATA")
        Log.d("DetailStoryActivity", "onDataReceived: $data")

        Glide.with(this)
            .load(data?.photoUrl)
            .into(binding.imgvDetailAvatar)
        binding.tvDetailName.text = data?.name
        binding.tvDetailDesc.text = data?.description
    }
}

class AddStoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddStoryBinding
    private var getFile: File? = null

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == CAMERA_X_RESULT) {
            val myFile = it.data?.getSerializableExtra("picture") as File
            val isBackCamera = it.data?.getBooleanExtra("isBackCamera", true) as Boolean

            getFile = myFile

            val result = rotateBitmap(
                BitmapFactory.decodeFile(getFile?.path),
                isBackCamera
            )

            binding.imgvCaptured.setImageBitmap(result)
        }
    }
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            val selectedImg: Uri = it.data?.data as Uri
            val myFile = uriToFile(selectedImg, this)

            getFile = myFile

            binding.imgvCaptured.setImageURI(selectedImg)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddStoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val viewModel = ViewModelProvider(
            this,
            ViewModelFactory(UserPreferences.getInstance(dataStore))
        )[MainViewModel::class.java]

        viewModel.isLoading.observe(this){
            showLoading(it)
        }

        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }

        binding.buttonCamera.setOnClickListener { startCameraX() }
        binding.buttonGallery.setOnClickListener { startGallery() }
        binding.uploadStoryButton.setOnClickListener { uploadStory() }
    }

    private fun startCameraX() {
        val intent = Intent(this, CameraActivity::class.java)
        cameraLauncher.launch(intent)
    }

    private fun startGallery() {
        val intent = Intent()
        intent.action = ACTION_GET_CONTENT
        intent.type = "image/*"

        val chooser = Intent.createChooser(intent, "Select a Picture")
        galleryLauncher.launch(chooser)
    }

    private fun uploadStory() {
        if (getFile != null) {
            val file = (reduceImageFile(getFile as File))

            val description =
                (binding.descEdittext.text.toString()).toRequestBody("text/plain".toMediaType())
            val requestImageFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())

            val viewModel = ViewModelProvider(
                this,
                ViewModelFactory(UserPreferences.getInstance(dataStore))
            )[MainViewModel::class.java]

            val storyMultipart: MultipartBody.Part = MultipartBody.Part.createFormData(
                "photo",
                file.name,
                requestImageFile
            )

            viewModel.getTokenKey().observe(this) {
                viewModel.uploadStory(storyMultipart, description, it)
            }
            viewModel.uploadStoryResponse.observe(this) {
                if (it.error == false) {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, it.error.toString(), Toast.LENGTH_SHORT).show()
                    binding.descEdittext.text?.clear()
                }
            }
        } else {
            Toast.makeText(this, getString(R.string.invalid_image), Toast.LENGTH_SHORT).show()
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (!allPermissionsGranted()) {
                Toast.makeText(
                    this,
                    getString(R.string.permission_denied),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun showLoading(b: Boolean) {
        if (b) {
            binding.loadingAddStory.visibility = View.VISIBLE
        } else {
            binding.loadingAddStory.visibility = View.GONE
        }
    }

    companion object {
        const val CAMERA_X_RESULT = 200
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val REQUEST_CODE_PERMISSIONS = 10
    }
}

class CameraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraBinding
    private var imageCapture: ImageCapture? = null
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startCamera()

        binding.switchCamera.setOnClickListener {
            cameraSelector =
                if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) CameraSelector.DEFAULT_FRONT_CAMERA
                else CameraSelector.DEFAULT_BACK_CAMERA

            startCamera()
        }
        binding.captureImage.setOnClickListener { takePhoto() }
    }

    private fun startCamera() {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (exc: Exception) {
                Toast.makeText(
                    this@CameraActivity,
                    getString(R.string.camera_launch_fail),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = createFile(application)

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(
                        this@CameraActivity,
                        getString(R.string.failed_takePhoto),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Toast.makeText(
                        this@CameraActivity,
                        getString(R.string.success_takePhoto),
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent = Intent()
                    intent.putExtra("picture", photoFile)
                    intent.putExtra(
                        "isBackCamera",
                        cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA
                    )
                    setResult(CAMERA_X_RESULT, intent)
                    finish()
                }
            }
        )
    }
}