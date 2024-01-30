package ai.mlc.mlcchat

import ai.mlc.mlcchat.ui.theme.MLCChatTheme
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.UUID


class MainActivity : ComponentActivity() {
    var image_path = ""
    lateinit var chatState: AppViewModel.ChatState

    @ExperimentalMaterial3Api
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chatState = AppViewModel(this.application).ChatState()
        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    // Permission is granted. Continue the action or workflow in your
                    // app.
                    Log.v("get_image", "is granted")
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // feature requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.
                    Log.v("get_image", "is granted")
                }
            }

        when {
            ContextCompat.checkSelfPermission(
                applicationContext,
                "android.permission.READ_EXTERNAL_STORAGE"
            ) == PackageManager.PERMISSION_GRANTED -> {
                // You can use the API that requires the permission.
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this, "android.permission.READ_EXTERNAL_STORAGE") -> {
                // In an educational UI, explain to the user why your app requires this
                // permission for a specific feature to behave as expected, and what
                // features are disabled if it's declined. In this UI, include a
                // "cancel" or "no thanks" button that lets the user continue
                // using your app without granting the permission.
               // showInContextUI(...)
            }
            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                requestPermissionLauncher.launch(
                    "android.permission.READ_EXTERNAL_STORAGE")
            }
        }

        setContent {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                MLCChatTheme {
                    NavView(this)
                }
            }
        }
    }

    @ExperimentalMaterial3Api
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (data == null) {
                return
            } else { //拍照
                val extras = data.extras
                if (extras != null) {
                    val bm = extras.getParcelable<Bitmap>("data")
                    //val uri: Uri = saveBitmap(bm)
                    //startImageZoom(uri)
                }
            }
        } else if (requestCode == 2) {
            if (data == null) { //相册
                return
            }
            val uri: Uri?
            uri = data.data
            Log.v("get_image", uri.toString())
            if (uri != null) {
                image_path = uri.path.toString()
                //image_path += ".jpg"
                //image_path =  "/sdcard/Pictures/IMG_20240129_214452.jpg"
            }
            chatState.messages.add(MessageData(MessageRole.User, "", UUID.randomUUID(), image_path))
            //val fileUri: Uri = convertUri(uri)
            //startImageZoom(fileUri)
        } else if (requestCode == 3) {
            if (data == null) {
                return
            } //剪裁后的图片
            val extras = data.extras ?: return
            val bm = extras.getParcelable<Bitmap>("data")
            //ShowImageView(bm)
        }
    }
}