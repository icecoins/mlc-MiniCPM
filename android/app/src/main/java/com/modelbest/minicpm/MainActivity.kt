package com.modelbest.minicpm

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.CursorLoader
import android.content.DialogInterface
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.material3.ExperimentalMaterial3Api
import java.io.ByteArrayOutputStream
import java.util.UUID

// Ref: https://gist.github.com/MeNiks/947b471b762f3b26178ef165a7f5558a
object RealPathUtil {

    fun getRealPath(context: Context, fileUri: Uri): String? {
        // SDK >= 11 && SDK < 19
        return if (Build.VERSION.SDK_INT < 19) {
            getRealPathFromURIAPI11to18(context, fileUri)
        } else {
            getRealPathFromURIAPI19(context, fileUri)
        }// SDK > 19 (Android 4.4) and up
    }

    @SuppressLint("NewApi")
    fun getRealPathFromURIAPI11to18(context: Context, contentUri: Uri): String? {
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        var result: String? = null

        val cursorLoader = CursorLoader(context, contentUri, proj, null, null, null)
        val cursor = cursorLoader.loadInBackground()

        if (cursor != null) {
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            result = cursor.getString(columnIndex)
            cursor.close()
        }
        return result
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     * @author Niks
     */
    @SuppressLint("NewApi")
    fun getRealPathFromURIAPI19(context: Context, uri: Uri): String? {

        val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                if ("primary".equals(type, ignoreCase = true)) {
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }
            } else if (isDownloadsDocument(uri)) {
                var cursor: Cursor? = null
                try {
                    cursor = context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)
                    cursor!!.moveToNext()
                    val fileName = cursor.getString(0)
                    val path = Environment.getExternalStorageDirectory().toString() + "/Download/" + fileName
                    if (!TextUtils.isEmpty(path)) {
                        return path
                    }
                } finally {
                    cursor?.close()
                }
                val id = DocumentsContract.getDocumentId(uri)
                if (id.startsWith("raw:")) {
                    return id.replaceFirst("raw:".toRegex(), "")
                }
                val contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads"), java.lang.Long.valueOf(id))

                return getDataColumn(context, contentUri, null, null)
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                var contentUri: Uri? = null
                when (type) {
                    "image" -> contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "video" -> contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    "audio" -> contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }

                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])

                return getDataColumn(context, contentUri, selection, selectionArgs)
            }// MediaProvider
            // DownloadsProvider
        } else if ("content".equals(uri.scheme!!, ignoreCase = true)) {

            // Return the remote address
            return if (isGooglePhotosUri(uri)) uri.lastPathSegment else getDataColumn(context, uri, null, null)
        } else if ("file".equals(uri.scheme!!, ignoreCase = true)) {
            return uri.path
        }// File
        // MediaStore (and general)

        return null
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     * @author Niks
     */
    fun getDataColumn(context: Context, uri: Uri?, selection: String?,
                              selectionArgs: Array<String>?): String? {

        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)

        try {
            cursor = context.contentResolver.query(uri!!, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(index)
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }
}

fun saveImageToGallery(context: Activity, bitmap: Bitmap): String {
    val contentResolver = context?.contentResolver ?: return "" // 获取内容提供者对象

    var imagePath = ""

    try {
        val values = ContentValues()

        // 设置图片信息
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "image")
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")

        // 创建文件并返回其URI
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        if (uri != null && !uri.path.isNullOrEmpty()) {
            // 将Bitmap转换为字节数组输出流
            val outputStream = ByteArrayOutputStream()

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)

            // 写入字节数组到指定路径
            contentResolver.openOutputStream(uri)?.write(outputStream.toByteArray())

            imagePath = RealPathUtil.getRealPath(context.applicationContext, uri).toString()
            //imagePath = uri.toString()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return imagePath
}

class MainActivity : ComponentActivity() {
    var image_path = ""
    var has_image = false
    var cpm_progress:ProgressBar? = null
    var cpm_v_progress:ProgressBar? = null
    lateinit var chatState: AppViewModel.ChatState
    @ExperimentalMaterial3Api
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Utils.setFullscreen(this)
        initConfigs()
        this.onBackPressedDispatcher.addCallback(this, object :OnBackPressedCallback(true) {
            override fun handleOnBackPressed(){
               Utils.backPress(this@MainActivity, "连续返回两次退出程序") { Utils.ActivityController.killAllActivities() }
            }
        })
        Utils.requestPermission(this@MainActivity)
        //chatState = AppViewModel(this.application).ChatState()
//        setContent {
//            Surface(
//                modifier = Modifier
//                    .fillMaxSize()
//            ) {
//                MLCChatTheme {
//                    NavView(this)
//                }
//            }
//        }
    }

    fun updateProgressBar(value:Int, flag: Int){
        when(flag){
            0->{
                cpm_progress!!.progress = value
            }
            1->{
                cpm_v_progress!!.progress = value
            }
        }
    }

    private fun initConfigs(){
        Utils.appViewModel = AppViewModel(this.application)
        if(Utils.appViewModel?.modelList?.size!=2){
            Log.e("MAIN", "初始化失败")
            return
        }
        Utils.appViewModel!!.chatState.setUpMain(this@MainActivity)
        Log.e("MAIN", "初始化成功")
        Utils.showHelp(this@MainActivity)
        cpm_progress = findViewById(R.id.main_cpm_progressbar)
        cpm_v_progress = findViewById(R.id.main_cpm_v_progressbar)
        val cpm_download:ImageView = findViewById(R.id.main_cpm_download)
        val cpm_v_download:ImageView = findViewById(R.id.main_cpm_v_download)
        cpm_download.setOnClickListener {
            when(Utils.appViewModel?.modelList?.get(0)?.modelInitState?.value){
                ModelInitState.Paused ->{
                    if(Utils.appViewModel?.modelList?.get(0)?.modelInitState?.value == ModelInitState.Downloading ||
                        Utils.appViewModel?.modelList?.get(1)?.modelInitState?.value == ModelInitState.Downloading){
                        Utils.showMsg(this@MainActivity, "模型正在下载，无需重复点击")
                        return@setOnClickListener
                    }
                    Utils.showAlert(this@MainActivity, "下载期间请保持APP在前台运行", "下载", "取消", {
                        cpm_download.setImageBitmap(BitmapFactory.decodeResource(this@MainActivity.resources ,R.mipmap.ic_pause))
                        Utils.appViewModel?.modelList?.get(0)?.handleStart()
                    }, {})
                }
                ModelInitState.Downloading ->{
                    Utils.appViewModel?.modelList?.get(0)?.handlePause()
                    cpm_download.setImageBitmap(BitmapFactory.decodeResource(this@MainActivity.resources ,R.mipmap.ic_download_light))
                }
                ModelInitState.Finished ->{
                    Utils.showMsg(this@MainActivity, "已完成下载")
                }
                else -> {
                    Log.e("STATE", Utils.appViewModel?.modelList?.get(0)?.modelInitState?.value.toString())
                    Utils.showMsg(this@MainActivity, "下载异常")
                }
            }
        }
        findViewById<ImageView>(R.id.main_cpm_chat)?.setOnClickListener {
            if(Utils.appViewModel?.modelList?.get(0)?.modelInitState?.value == ModelInitState.Finished){
                Utils.appViewModel?.modelList?.get(0)?.startChat()
                startActivity(Intent(this, Chat::class.java).setAction(""))
            }else{
                Utils.showMsg(this@MainActivity, "预训练模型 MiniCPM 未下载")
            }
        }
        findViewById<ImageView>(R.id.main_cpm_delete)?.setOnClickListener {
            if(Utils.appViewModel?.modelList?.get(0)?.modelInitState?.value == ModelInitState.Finished ||
                Utils.appViewModel?.modelList?.get(0)?.modelInitState?.value == ModelInitState.Paused){
                Utils.showAlert(this@MainActivity, "是否删除已下载的预训练模型？",
                    "确定", "取消", { Utils.appViewModel!!.modelList[0].handleClear() }, {})
            }else{
                Utils.showMsg(this@MainActivity, "请先暂停下载")
            }
        }

        cpm_v_download.setOnClickListener {
            when (Utils.appViewModel?.modelList?.get(1)?.modelInitState?.value) {
                ModelInitState.Paused -> {
                    if(Utils.appViewModel?.modelList?.get(0)?.modelInitState?.value == ModelInitState.Downloading ||
                        Utils.appViewModel?.modelList?.get(1)?.modelInitState?.value == ModelInitState.Downloading){
                        Utils.showMsg(this@MainActivity, "模型正在下载，无需重复点击")
                        return@setOnClickListener
                    }
                    Utils.showAlert(this@MainActivity, "下载期间请保持APP在前台运行", "下载", "取消", {
                        cpm_v_download.setImageBitmap(BitmapFactory.decodeResource(this@MainActivity.resources, R.mipmap.ic_pause))
                        Utils.appViewModel?.modelList?.get(1)?.handleStart()
                    }, {})
                }
                ModelInitState.Downloading -> {
                    Utils.appViewModel?.modelList?.get(1)?.handlePause()
                    cpm_v_download.setImageBitmap(
                        BitmapFactory.decodeResource(
                            this@MainActivity.resources,
                            R.mipmap.ic_download_light
                        )
                    )
                }
                ModelInitState.Finished -> {
                    Utils.showMsg(this@MainActivity, "已完成下载")
                }
                else -> {
                    Utils.showMsg(this@MainActivity, "下载异常")
                }
            }
        }
        findViewById<ImageView>(R.id.main_cpm_v_chat)?.setOnClickListener {
            if(Utils.appViewModel?.modelList?.get(1)?.modelInitState?.value == ModelInitState.Finished){
                Utils.appViewModel?.modelList?.get(1)?.startChat()
                startActivity(Intent(this, Chat::class.java).setAction(""))
            }else{
                Utils.showMsg(this@MainActivity, "预训练模型 MiniCPM-V 未下载")
            }
        }
        findViewById<ImageView>(R.id.main_cpm_v_delete)?.setOnClickListener {
            if(Utils.appViewModel?.modelList?.get(1)?.modelInitState?.value == ModelInitState.Finished ||
                Utils.appViewModel?.modelList?.get(1)?.modelInitState?.value == ModelInitState.Paused){
                Utils.showAlert(this@MainActivity, "是否删除已下载的预训练模型？",
                    "确定", "取消", { Utils.appViewModel!!.modelList[1].handleClear() }, {})
            }else{
                Utils.showMsg(this@MainActivity, "请先暂停下载")
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
                    if (bm != null) {
                        image_path = saveImageToGallery(this, bm)
                        chatState.messages.add(MessageData(MessageRole.User, "", UUID.randomUUID(), image_path))
                    }
                }
            }
        } else if (requestCode == 2) {
            if (data == null) { //相册
                return
            }
            var uri = data.data
            if (uri != null) {
                //image_path = getFilePathFromUri(uri)
                image_path = RealPathUtil.getRealPath(this.applicationContext, uri).toString()
            }
            Log.v("get_image", image_path)
            chatState.messages.add(MessageData(MessageRole.User, "", UUID.randomUUID(), image_path))

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
