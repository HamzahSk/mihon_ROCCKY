
# Build a Text Recognition App using ML Kit and Jetpack Compose
Tutorial ini menjelaskan cara membuat aplikasi pengenalan teks di Android menggunakan **ML Kit**, **Jetpack Compose**, dan **CameraX**. Fitur yang mencakup:
 * Menangkap teks menggunakan kamera.
 * Memilih gambar dari galeri untuk diekstrak teksnya.
 * Menampilkan teks hasil *recognition* di layar.
## Step 1: Add Required Dependencies
Perbarui file build.gradle (app) kamu dengan *dependencies* berikut:
```gradle
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
}

android {
    compileSdk 34
    defaultConfig {
        applicationId "com.example.textrecognition"
        minSdk 24
        targetSdk 34
    }
    buildFeatures {
        compose true
    }
    composeOptions {
        kotlinCompilerExtensionVersion '1.5.0'
    }
}

dependencies {
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.activity:activity-compose:1.8.0'
    implementation 'androidx.compose.ui:ui:1.5.0'
    implementation 'androidx.compose.material3:material3:1.2.0-alpha03'
    
    // CameraX
    implementation 'androidx.camera:camera-camera2:1.3.0'
    implementation 'androidx.camera:camera-lifecycle:1.3.0'
    implementation 'androidx.camera:camera-view:1.3.0'
    implementation 'androidx.camera:camera-extensions:1.3.0'
    
    // ML Kit Text Recognition
    implementation 'com.google.mlkit:text-recognition:16.0.0'
}

```
## Step 2: Create a Text Recognition Helper
Buat *object* Kotlin baru bernama TextRecognitionHelper.kt untuk menangani logika ML Kit:
```kotlin
object TextRecognitionHelper {
    fun recognizeTextFromUri(context: Context, uri: Uri, onResult: (String) -> Unit) {
        val inputImage = InputImage.fromFilePath(context, uri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        
        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                onResult(visionText.text)
            }
            .addOnFailureListener { exception ->
                onResult("Recognition failed: ${exception.localizedMessage}")
            }
    }
}

```
## Step 3: Setup Main Activity
Siapkan MainActivity.kt sebagai *entry point* untuk Compose UI:
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                MainScreen()
            }
        }
    }
}

```
## Step 4: Implement the MainScreen UI
Buat *composable* MainScreen yang mencakup *permission handling*, pemilih galeri, dan UI kamera:
```kotlin
@Composable
fun MainScreen() {
    var recognizedText by remember { mutableStateOf("Recognized text will appear here.") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        uri?.let {
            TextRecognitionHelper.recognizeTextFromUri(context, uri) { result ->
                recognizedText = result
            }
        }
    }
    
    val cameraPermissionState = remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            cameraPermissionState.value = isGranted
        }
    )
    
    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        
        if (hasPermission) {
            cameraPermissionState.value = true
        } else {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }
    
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp)) {
            
            if (cameraPermissionState.value) {
                CameraBox {
                    recognizedText = it
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Camera permission is required to use this feature.")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text("Select Image from Gallery")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = recognizedText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )
        }
    }
}

```
## Step 5: Create CameraBox for Capturing Photos
Implementasikan integrasi CameraX di dalam *composable* CameraBox:
```kotlin
@Composable
fun CameraBox(onTextRecognized: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }
    
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    // Bind camera
    LaunchedEffect(Unit) {
        try {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().apply {
                surfaceProvider = previewView.surfaceProvider
            }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            Log.e("CameraBox", "Camera initialization failed", e)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Color.Gray, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .aspectRatio(1f)
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        Button(
            onClick = {
                val outputFile = File(
                    context.cacheDir,
                    "captured_image_${System.currentTimeMillis()}.jpg"
                )
                val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
                imageCapture.takePicture(
                    outputOptions,
                    cameraExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            val savedUri = outputFileResults.savedUri ?: outputFile.toUri()
                            TextRecognitionHelper.recognizeTextFromUri(context, savedUri) {
                                onTextRecognized(it)
                            }
                        }
                        override fun onError(exception: ImageCaptureException) {
                            Log.e("CameraPreview", "Capture failed: ${exception.message}", exception)
                        }
                    }
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
        ) {
            Text("Capture")
        }
    }
}

```
> **Catatan Penting:** Pastikan aplikasi memiliki *permission* kamera di AndroidManifest.xml:
> <uses-permission android:name="android.permission.CAMERA" />
> 
## Multi-language Text Recognition with ML Kit
Untuk mendukung pengenalan bahasa selain Latin (seperti Chinese, Devanagari, Japanese, atau Korean), ML Kit menyediakan model yang dioptimalkan per *script*.
### 1. Dependencies
Tambahkan dependensi berikut ke dalam build.gradle(:app):
```gradle
dependencies {
    implementation 'com.google.mlkit:text-recognition:16.0.1' // Latin
    implementation 'com.google.mlkit:text-recognition-chinese:16.0.1'
    implementation 'com.google.mlkit:text-recognition-devanagari:16.0.1'
    implementation 'com.google.mlkit:text-recognition-japanese:16.0.1'
    implementation 'com.google.mlkit:text-recognition-korean:16.0.1'
}

```
### 2. Recognizer Setup
Kamu dapat mengatur *recognizer* spesifik untuk setiap bahasa:
```kotlin
val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT)

val chineseRecognizer = TextRecognition.getClient(
    ChineseTextRecognizerOptions.Builder().build()
)

val devanagariRecognizer = TextRecognition.getClient(
    DevanagariTextRecognizerOptions.Builder().build()
)

val japaneseRecognizer = TextRecognition.getClient(
    JapaneseTextRecognizerOptions.Builder().build()
)

val koreanRecognizer = TextRecognition.getClient(
    KoreanTextRecognizerOptions.Builder().build()
)

```
**Fungsi dinamis untuk berpindah bahasa:**
```kotlin
fun getRecognizerByLanguage(lang: String) = when (lang.lowercase()) {
    "chinese" -> chineseRecognizer
    "devanagari" -> devanagariRecognizer
    "japanese" -> japaneseRecognizer
    "korean" -> koreanRecognizer
    else -> latinRecognizer
}

```
