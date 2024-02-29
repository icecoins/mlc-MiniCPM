package com.modelbest.minicpm

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentUris
import android.content.Context
import android.content.DialogInterface
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Looper
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import java.lang.ref.WeakReference

object Utils {
    private var toast: Toast? = null
    private var mBackPressed: Long = 0
    var appViewModel:AppViewModel?=null
    var chatItemAdapter:ChatItemAdapter?=null
    var sendingImg:Boolean = false
    var cachePath:String? = null
    object ActivityController{
        private var activityList:ArrayList<Activity>?= null
        init {
            activityList = ArrayList()
        }
        fun addActivity(activity: Activity){
            activityList?.add(activity)
        }
        fun killAllActivities(){
            for (activity in activityList!!){
                activity.finish()
            }
        }
    }
    object LoadingDialogUtils {
        private var loadingDialog: AlertDialog? = null
        private var reference: WeakReference<Activity?>? = null
        private fun init(activity: Activity?, res: Int, text: String?) {
            if (loadingDialog == null || reference == null || reference!!.get() == null || reference!!.get()!!.isFinishing
            ) {
                reference = WeakReference(activity)
                loadingDialog = AlertDialog.Builder(reference!!.get()).create()
                if (res > 0) {
                    val view = LayoutInflater.from(activity).inflate(res, null)
                    if (null != text) {
                        val textView = view.findViewById<TextView>(R.id.loading_text)
                        textView.text = text
                    }
                    loadingDialog!!.setView(view)
                } else {
                    loadingDialog!!.setMessage("加载中...")
                }
                loadingDialog!!.setCancelable(false)
            }
        }

        private fun setCancelable(b: Boolean) {
            if (loadingDialog == null) return
            loadingDialog!!.setCancelable(b)
        }

        /**
         * 显示等待框
         */
        fun show(act: Activity?) {
            show(act, R.layout.dialog_loading, null, false)
        }

        fun show(act: Activity?, text: String?) {
            show(act, R.layout.dialog_loading, text, false)
        }

        fun show(activity: Activity?, res: Int, text: String?, isCancelable: Boolean) {
            dismiss()
            init(activity, res, text)
            setCancelable(isCancelable)
            loadingDialog!!.show()
        }

        /**
         * 隐藏等待框
         */
        fun dismiss() {
            if (loadingDialog != null && loadingDialog!!.isShowing) {
                loadingDialog!!.dismiss()
                loadingDialog = null
                reference = null
            }
        }
    }
    fun showMsg(ct: Context?, s: String?) {
        Thread {
            try {
                if (null == Looper.myLooper()) {
                    Looper.prepare()
                }
                if (toast != null) {
                    toast?.cancel()
                   toast = null
                }
                toast = Toast.makeText(ct, s, Toast.LENGTH_SHORT)
                toast?.show()
                if (null != Looper.myLooper()) {
                    Looper.loop()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
    fun setFullscreen(activity: Activity) {
        ActivityController.addActivity(activity)
        activity.actionBar?.hide()
        if (activity.resources.configuration.orientation ==
            Configuration.ORIENTATION_LANDSCAPE && Build.VERSION.SDK_INT >= 28
        ) {
            val lp = activity.window.attributes
            lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        val _window = activity.window
        val params = _window.attributes
        params.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        _window.attributes = params
        activity.window.statusBarColor = Color.TRANSPARENT
    }
    fun requestPermission(activity: Activity){
        val permissions = arrayOf(
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.CAMERA",
            "android.Manifest.permission.READ_MEDIA_IMAGES",
            "android.Manifest.permission.READ_MEDIA_AUDIO",
            "android.Manifest.permission.READ_MEDIA_VIDEO",
        )
        ActivityCompat.requestPermissions(activity, permissions, 1)
    }
    fun backPress(activity: Activity, msg:String, runnable: Runnable){
        if (mBackPressed > System.currentTimeMillis() - 2000) {
            runnable.run()
        } else {
            mBackPressed = System.currentTimeMillis()
            showMsg(activity, msg)
        }
    }
    fun closeInputMethod(activity: Activity) {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (imm.isActive) {
            val v: View = activity.window.peekDecorView()
            imm.hideSoftInputFromWindow(v.windowToken, 0)
        }
    }
    fun showHelp(activity: Activity) {
        val b = androidx.appcompat.app.AlertDialog.Builder(activity)
        val view = View.inflate(activity, R.layout.layout_help, null)
        b.setView(view)
        b.setNegativeButton(
            "已  阅"
        ) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
        b.show()
    }
    fun showImg(context: Context?, filePath: String?) {
        val builder = AlertDialog.Builder(context).create()
        val inflate = View.inflate(context, R.layout.layout_img_magnification, null)
        val imageView = inflate.findViewById<ImageView>(R.id.img_detail)
        imageView.setImageBitmap(BitmapFactory.decodeFile(filePath))
        builder.setView(inflate)
        builder.show()
    }
    fun showAlert(activity: Activity, title:String, pos:String, neg:String, posRun: Runnable, negRun: Runnable){
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(title)
        builder.setPositiveButton(pos) { dialog: DialogInterface, _: Int ->
            posRun.run()
            dialog.dismiss()
        }
        builder.setNegativeButton(neg) { dialog: DialogInterface, _: Int ->
            negRun.run()
            dialog.dismiss()
        }
        builder.show()
    }
    fun getRealPathFromURI(context: Context, contentUri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            cursor = context.contentResolver.query(contentUri, proj, null, null, null)
            val columnIndex = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            cursor.getString(columnIndex)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            null
        } finally {
            cursor?.close()
        }
    }
    fun getRealPathFromURIAPI19(context: Context, uri : Uri): String? {

        val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (RealPathUtil.isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                if ("primary".equals(type, ignoreCase = true)) {
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }
            } else if (RealPathUtil.isDownloadsDocument(uri)) {
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

                return RealPathUtil.getDataColumn(context, contentUri, null, null)
            } else if (RealPathUtil.isMediaDocument(uri)) {
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

                return RealPathUtil.getDataColumn(context, contentUri, selection, selectionArgs)
            }// MediaProvider
            // DownloadsProvider
        } else if ("content".equals(uri.scheme!!, ignoreCase = true)) {

            // Return the remote address
            return if (RealPathUtil.isGooglePhotosUri(uri)) uri.lastPathSegment else RealPathUtil.getDataColumn(
                context,
                uri,
                null,
                null
            )
        } else if ("file".equals(uri.scheme!!, ignoreCase = true)) {
            return uri.path
        }// File
        // MediaStore (and general)

        return null
    }
}