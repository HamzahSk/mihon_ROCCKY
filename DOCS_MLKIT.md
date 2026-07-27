You can use ML Kit to recognize text in images or video, such as the text of
a street sign. The main characteristics of this feature are:
This API is available using either an unbundled library that must be downloaded before use or a bundled library that increases your app size. See [this guide](https://developers.google.com/ml-kit/tips/installation-paths) for more information on the differences between the two installation options.

| Feature | Unbundled | Bundled |
|---|---|---|
| Library name | com.google.android.gms:play-services-mlkit-text-recognition com.google.android.gms:play-services-mlkit-text-recognition-chinese com.google.android.gms:play-services-mlkit-text-recognition-devanagari com.google.android.gms:play-services-mlkit-text-recognition-japanese com.google.android.gms:play-services-mlkit-text-recognition-korean | com.google.mlkit:text-recognition com.google.mlkit:text-recognition-chinese com.google.mlkit:text-recognition-devanagari com.google.mlkit:text-recognition-japanese com.google.mlkit:text-recognition-korean |
| Implementation | Model is dynamically downloaded via Google Play Services. | Model is statically linked to your app at build time. |
| App size | About 260 KB size increase per script architecture. | About 4 MB size increase per script per architecture. |
| Initialization time | Might have to wait for model to download before first use. | Model is available immediately. |
| Performance | Real-time on most devices for Latin script library, slower for others. | Real-time on most devices for Latin script library, slower for others. |

## Try it out

- Play around with [the sample app](https://github.com/googlesamples/mlkit/tree/master/android/vision-quickstart) to see an example usage of this API.
- Try the code yourself with the [codelab](https://codelabs.developers.google.com/codelabs/mlkit-android).

## Before you begin

> [!NOTE]
> This API requires Android API level 23 or above. Make sure that your app's build file uses a `minSdkVersion` value of 23 or higher.

1. In your project-level `build.gradle` file, make sure to include Google's Maven repository in both your `buildscript` and `allprojects` sections.
2. Add the dependencies for the ML Kit Android libraries to your module's app-level gradle file, which is usually `app/build.gradle`:

   **For bundling the model with your app:**

       dependencies {
         // To recognize Latin script
         implementation 'com.google.mlkit:text-recognition:16.0.1'

         // To recognize Chinese script
         implementation 'com.google.mlkit:text-recognition-chinese:16.0.1'

         // To recognize Devanagari script
         implementation 'com.google.mlkit:text-recognition-devanagari:16.0.1'

         // To recognize Japanese script
         implementation 'com.google.mlkit:text-recognition-japanese:16.0.1'

         // To recognize Korean script
         implementation 'com.google.mlkit:text-recognition-korean:16.0.1'
       }

   **For using the model in Google Play Services:**

       dependencies {
         // To recognize Latin script
         implementation 'com.google.android.gms:play-services-mlkit-text-recognition:19.0.1'

         // To recognize Chinese script
         implementation 'com.google.android.gms:play-services-mlkit-text-recognition-chinese:16.0.1'

         // To recognize Devanagari script
         implementation 'com.google.android.gms:play-services-mlkit-text-recognition-devanagari:16.0.1'

         // To recognize Japanese script
         implementation 'com.google.android.gms:play-services-mlkit-text-recognition-japanese:16.0.1'

         // To recognize Korean script
         implementation 'com.google.android.gms:play-services-mlkit-text-recognition-korean:16.0.1'
       }

3. **If you choose to use the model in Google Play Services** , you can
   configure your app to automatically download the model to the device after
   your app is installed from the Play Store. To do so, add the following
   declaration to your app's `AndroidManifest.xml` file:

       <application ...>
             ...
             <meta-data
                 android:name="com.google.mlkit.vision.DEPENDENCIES"
                 android:value="ocr" >
             <!-- To use multiple models: android:value="ocr,ocr_chinese,ocr_devanagari,ocr_japanese,ocr_korean,..." -->
       </application>

   You can also explicitly check the model availability and request download
   through Google Play services [ModuleInstallClient API](https://developers.google.com/android/guides/module-install-apis). If you don't enable install-time model
   downloads or request explicit download, the model is downloaded the first
   time you run the scanner. Requests you make before the download has
   completed produce no results.

## 1. Create an instance of `TextRecognizer`

Create an instance of [`TextRecognizer`](https://developers.google.com/android/reference/com/google/mlkit/vision/text/TextRecognizer), passing the options related to the library you declared a dependency on above:

<br />

### Kotlin

```
// When using Latin script library
val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

// When using Chinese script library
val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

// When using Devanagari script library
val recognizer = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())

// When using Japanese script library
val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())

// When using Korean script library
val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
```

### Java

```
// When using Latin script library
TextRecognizer recognizer =
  TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

// When using Chinese script library
TextRecognizer recognizer =
  TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());

// When using Devanagari script library
TextRecognizer recognizer =
  TextRecognition.getClient(new DevanagariTextRecognizerOptions.Builder().build());

// When using Japanese script library
TextRecognizer recognizer =
  TextRecognition.getClient(new JapaneseTextRecognizerOptions.Builder().build());

// When using Korean script library
TextRecognizer recognizer =
  TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());
```

## 2. Prepare the input image

To recognize text in an image, create an [`InputImage`](https://developers.google.com/android/reference/com/google/mlkit/vision/common/InputImage) object from
either a `Bitmap`, `media.Image`, `ByteBuffer`, byte array, or a file on the
device. Then, pass the [`InputImage`](https://developers.google.com/android/reference/com/google/mlkit/vision/common/InputImage) object to the
[`TextRecognizer`](https://developers.google.com/android/reference/com/google/mlkit/vision/text/TextRecognizer)'s `processImage` method.


You can create an [`InputImage`](https://developers.google.com/android/reference/com/google/mlkit/vision/common/InputImage)
object from different sources, each is explained below.

### Using a `media.Image`


To create an [`InputImage`](https://developers.google.com/android/reference/com/google/mlkit/vision/common/InputImage)
object from a `media.Image` object, such as when you capture an image from a
device's camera, pass the `media.Image` object and the image's
rotation to `InputImage.fromMediaImage()`.


If you use the
[CameraX](https://developer.android.com/training/camerax) library, the `OnImageCapturedListener` and
`ImageAnalysis.Analyzer` classes calculate the rotation value
for you.

### Kotlin

```kotlin
private class YourImageAnalyzer : ImageAnalysis.Analyzer {

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            // Pass image to an ML Kit Vision API
            // ...
        }
    }
}
```

### Java

```java
private class YourAnalyzer implements ImageAnalysis.Analyzer {

    @Override
    public void analyze(ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
          InputImage image =
                InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
          // Pass image to an ML Kit Vision API
          // ...
        }
    }
}
```


If you don't use a camera library that gives you the image's rotation degree, you
can calculate it from the device's rotation degree and the orientation of camera
sensor in the device:

### Kotlin

```kotlin
private val ORIENTATIONS = SparseIntArray()

init {
    ORIENTATIONS.append(Surface.ROTATION_0, 0)
    ORIENTATIONS.append(Surface.ROTATION_90, 90)
    ORIENTATIONS.append(Surface.ROTATION_180, 180)
    ORIENTATIONS.append(Surface.ROTATION_270, 270)
}

/**
 * Get the angle by which an image must be rotated given the device's current
 * orientation.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
@Throws(CameraAccessException::class)
private fun getRotationCompensation(cameraId: String, activity: Activity, isFrontFacing: Boolean): Int {
    // Get the device's current rotation relative to its "native" orientation.
    // Then, from the ORIENTATIONS table, look up the angle the image must be
    // rotated to compensate for the device's rotation.
    val deviceRotation = activity.windowManager.defaultDisplay.rotation
    var rotationCompensation = ORIENTATIONS.get(deviceRotation)

    // Get the device's sensor orientation.
    val cameraManager = activity.getSystemService(CAMERA_SERVICE) as CameraManager
    val sensorOrientation = cameraManager
            .getCameraCharacteristics(cameraId)
            .get(CameraCharacteristics.SENSOR_ORIENTATION)!!

    if (isFrontFacing) {
        rotationCompensation = (sensorOrientation + rotationCompensation) % 360
    } else { // back-facing
        rotationCompensation = (sensorOrientation - rotationCompensation + 360) % 360
    }
    return rotationCompensation
}
```

### Java

```java
private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
static {
    ORIENTATIONS.append(Surface.ROTATION_0, 0);
    ORIENTATIONS.append(Surface.ROTATION_90, 90);
    ORIENTATIONS.append(Surface.ROTATION_180, 180);
    ORIENTATIONS.append(Surface.ROTATION_270, 270);
}

/**
 * Get the angle by which an image must be rotated given the device's current
 * orientation.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
private int getRotationCompensation(String cameraId, Activity activity, boolean isFrontFacing)
        throws CameraAccessException {
    // Get the device's current rotation relative to its "native" orientation.
    // Then, from the ORIENTATIONS table, look up the angle the image must be
    // rotated to compensate for the device's rotation.
    int deviceRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
    int rotationCompensation = ORIENTATIONS.get(deviceRotation);

    // Get the device's sensor orientation.
    CameraManager cameraManager = (CameraManager) activity.getSystemService(CAMERA_SERVICE);
    int sensorOrientation = cameraManager
            .getCameraCharacteristics(cameraId)
            .get(CameraCharacteristics.SENSOR_ORIENTATION);

    if (isFrontFacing) {
        rotationCompensation = (sensorOrientation + rotationCompensation) % 360;
    } else { // back-facing
        rotationCompensation = (sensorOrientation - rotationCompensation + 360) % 360;
    }
    return rotationCompensation;
}
```

Then, pass the `media.Image` object and the
rotation degree value to `InputImage.fromMediaImage()`:

### Kotlin

```kotlin
val image = InputImage.fromMediaImage(mediaImage, rotation)
```

### Java

```java
InputImage image = InputImage.fromMediaImage(mediaImage, rotation);
```

### Using a file URI

To create an [`InputImage`](https://developers.google.com/android/reference/com/google/mlkit/vision/common/InputImage)
object from a file URI, pass the app context and file URI to
`InputImage.fromFilePath()`. This is useful when you
use an `ACTION_GET_CONTENT` intent to prompt the user to select
an image from their gallery app.

### Kotlin

```kotlin
val image: InputImage
try {
    image = InputImage.fromFilePath(context, uri)
} catch (e: IOException) {
    e.printStackTrace()
}
```

### Java

```java
InputImage image;
try {
    image = InputImage.fromFilePath(context, uri);
} catch (IOException e) {
    e.printStackTrace();
}
```

### Using a `ByteBuffer` or `ByteArray`

To create an [`InputImage`](https://developers.google.com/android/reference/com/google/mlkit/vision/common/InputImage)
object from a `ByteBuffer` or a `ByteArray`, first calculate the image
rotation degree as previously described for `media.Image` input.
Then, create the `InputImage` object with the buffer or array, together with image's
height, width, color encoding format, and rotation degree:

### Kotlin

```kotlin
val image = InputImage.fromByteBuffer(
        byteBuffer,
        /* image width */ 480,
        /* image height */ 360,
        rotationDegrees,
        InputImage.IMAGE_FORMAT_NV21 // or IMAGE_FORMAT_YV12
)
// Or:
val image = InputImage.fromByteArray(
        byteArray,
        /* image width */ 480,
        /* image height */ 360,
        rotationDegrees,
        InputImage.IMAGE_FORMAT_NV21 // or IMAGE_FORMAT_YV12
)
```

### Java

```java
InputImage image = InputImage.fromByteBuffer(byteBuffer,
        /* image width */ 480,
        /* image height */ 360,
        rotationDegrees,
        InputImage.IMAGE_FORMAT_NV21 // or IMAGE_FORMAT_YV12
);
// Or:
InputImage image = InputImage.fromByteArray(
        byteArray,
        /* image width */480,
        /* image height */360,
        rotation,
        InputImage.IMAGE_FORMAT_NV21 // or IMAGE_FORMAT_YV12
);
```

### Using a `Bitmap`

To create an [`InputImage`](https://developers.google.com/android/reference/com/google/mlkit/vision/common/InputImage)
object from a `Bitmap` object, make the following declaration:

### Kotlin

```kotlin
val image = InputImage.fromBitmap(bitmap, 0)
```

### Java

```java
InputImage image = InputImage.fromBitmap(bitmap, rotationDegree);
```

The image is represented by a `Bitmap` object together with rotation degrees.

## 3. Process the image

Pass the image to the `process` method:

### Kotlin

```kotlin
val result = recognizer.process(image)
        .addOnSuccessListener { visionText ->
            // Task completed successfully
            // ...
        }
        .addOnFailureListener { e ->
            // Task failed with an exception
            // ...
        }
```

### Java

```java
Task<Text> result =
        recognizer.process(image)
                .addOnSuccessListener(new OnSuccessListener<Text>() {
                    @Override
                    public void onSuccess(Text visionText) {
                        // Task completed successfully
                        // ...
                    }
                })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                // ...
                            }
                        });
```
Note: If you are using the [`CameraX`](https://developer.android.com/jetpack/androidx/releases/camera) API, make sure to close the `ImageProxy` when finish using it, e.g., by adding an `OnCompleteListener` to the `Task` returned from the `process` method. See the [`VisionProcessorBase`](https://github.com/googlesamples/mlkit/blob/e949654bef09e97b237c7692349c52ee06e9fd9e/android/vision-quickstart/app/src/main/java/com/google/mlkit/vision/demo/java/VisionProcessorBase.java#L178-L181) class in the quickstart sample app for an example.

## 4. Extract text from blocks of recognized text

If the text recognition operation succeeds, a [`Text`](https://developers.google.com/android/reference/com/google/mlkit/vision/text/Text) object is passed to
the success listener. A `Text` object contains the full text recognized in
the image and zero or more [`TextBlock`](https://developers.google.com/android/reference/com/google/mlkit/vision/text/Text.TextBlock) objects.

Each [`TextBlock`](https://developers.google.com/android/reference/com/google/mlkit/vision/text/Text.TextBlock) represents a rectangular block of text,
which contains zero or more [`Line`](https://developers.google.com/android/reference/com/google/mlkit/vision/text/Text.Line) objects. Each
[`Line`](https://developers.google.com/android/reference/com/google/mlkit/vision/text/Text.Line) object represents a line of text, which contains zero
or more [`Element`](https://developers.google.com/android/reference/com/google/mlkit/vision/text/Text.Element) objects. Each [`Element`](https://developers.google.com/android/reference/com/google/mlkit/vision/text/Text.Element)
object represents a word or a word-like entity, which contains zero or more
[`Symbol`](https://developers.google.com/android/reference/com/google/mlkit/vision/text/Text.Symbol) objects. Each [`Symbol`](https://developers.google.com/android/reference/com/google/mlkit/vision/text/Text.Symbol)
object represents a character, a digit or a word-like entity.

For each [`TextBlock`](https://developers.google.com/android/reference/com/google/mlkit/vision/text/Text.TextBlock), [`Line`](https://developers.google.com/android/reference/com/google/mlkit/vision/text/Text.Line),
[`Element`](https://developers.google.com/android/reference/com/google/mlkit/vision/text/Text.Element) and [`Symbol`](https://developers.google.com/android/reference/com/google/mlkit/vision/text/Text.Symbol) object, you
can get the text recognized in the region, the bounding coordinates of the
region and many other attributes such as rotation information, confidence score
etc.

For example:

### Kotlin

```kotlin
val resultText = result.text
for (block in result.textBlocks) {
    val blockText = block.text
    val blockCornerPoints = block.cornerPoints
    val blockFrame = block.boundingBox
    for (line in block.lines) {
        val lineText = line.text
        val lineCornerPoints = line.cornerPoints
        val lineFrame = line.boundingBox
        for (element in line.elements) {
            val elementText = element.text
            val elementCornerPoints = element.cornerPoints
            val elementFrame = element.boundingBox
        }
    }
}
```

### Java

```java
String resultText = result.getText();
for (Text.TextBlock block : result.getTextBlocks()) {
    String blockText = block.getText();
    Point[] blockCornerPoints = block.getCornerPoints();
    Rect blockFrame = block.getBoundingBox();
    for (Text.Line line : block.getLines()) {
        String lineText = line.getText();
        Point[] lineCornerPoints = line.getCornerPoints();
        Rect lineFrame = line.getBoundingBox();
        for (Text.Element element : line.getElements()) {
            String elementText = element.getText();
            Point[] elementCornerPoints = element.getCornerPoints();
            Rect elementFrame = element.getBoundingBox();
            for (Text.Symbol symbol : element.getSymbols()) {
                String symbolText = symbol.getText();
                Point[] symbolCornerPoints = symbol.getCornerPoints();
                Rect symbolFrame = symbol.getBoundingBox();
            }
        }
    }
}
```

## Input image guidelines

- For ML Kit to accurately recognize text, input images must contain
  text that is represented by sufficient pixel data. Ideally,
  each character should be at least 16x16 pixels. There is generally no
  accuracy benefit for characters to be larger than 24x24 pixels.

  So, for example, a 640x480 image might work well to scan a business card
  that occupies the full width of the image. To scan a document printed on
  letter-sized paper, a 720x1280 pixel image might be required.
- Poor image focus can affect text recognition accuracy. If you aren't
  getting acceptable results, try asking the user to recapture the image.

- If you are recognizing text in a real-time application, you should
  consider the overall dimensions of the input images. Smaller
  images can be processed faster. To reduce latency, ensure that the text occupies as much of
  the image as possible, and capture images at lower resolutions (keeping in mind the accuracy
  requirements mentioned above). For more information, see
  [Tips to improve performance](https://developers.google.com/ml-kit/vision/text-recognition/v2/android#performance-tips).

## Tips to improve performance

- If you use the [`Camera`](https://developer.android.com/reference/android/hardware/Camera) or [`camera2`](https://developer.android.com/reference/android/hardware/camera2/package-summary) API, throttle calls to the detector. If a new video frame becomes available while the detector is running, drop the frame. See the [`VisionProcessorBase`](https://github.com/googlesamples/mlkit/blob/master/android/vision-quickstart/app/src/main/java/com/google/mlkit/vision/demo/kotlin/VisionProcessorBase.kt) class in the quickstart sample app for an example.
- If you use the [`CameraX`](https://developer.android.com/jetpack/androidx/releases/camera) API, be sure that backpressure strategy is set to its default value [`ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST`](https://developer.android.com/reference/androidx/camera/core/ImageAnalysis#STRATEGY_KEEP_ONLY_LATEST). This guarantees only one image will be delivered for analysis at a time. If more images are produced when the analyzer is busy, they will be dropped automatically and not queued for delivery. Once the image being analyzed is closed by calling ImageProxy.close(), the next latest image will be delivered.
- If you use the output of the detector to overlay graphics on the input image, first get the result from ML Kit, then render the image and overlay in a single step. This renders to the display surface only once for each input frame. See the [`CameraSourcePreview`](https://github.com/googlesamples/mlkit/blob/master/android/vision-quickstart/app/src/main/java/com/google/mlkit/vision/demo/CameraSourcePreview.java) and [`GraphicOverlay`](https://github.com/googlesamples/mlkit/blob/master/android/vision-quickstart/app/src/main/java/com/google/mlkit/vision/demo/GraphicOverlay.java) classes in the quickstart sample app for an example.
- If you use the Camera2 API, capture images in `ImageFormat.YUV_420_888` format. If you use the older Camera API, capture images in `ImageFormat.NV21` format.
- Consider capturing images at a lower resolution. However, also keep in mind this API's image dimension requirements.