package com.modelbest.minicpm

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity
import java.io.File
import java.io.IOException
import java.util.UUID


class Chat : AppCompatActivity(){
    private var chatBox:ListView?=null
    private var help:ImageView?=null
    private var config:ImageView?=null
    private var delete:ImageView?=null
    private var send:ImageView?=null
    private var camera:ImageView?=null
    private var label:TextView?=null
    private var report:TextView?=null
    private var input:EditText?=null
    private var getImg:ImageView?=null
    private var imgPath:String = ""
    private var sentImg:Boolean = false
    var cameraLauncher: ActivityResultLauncher<Intent>? = null
    var galleryLauncher: ActivityResultLauncher<Intent>? = null
    var uCropLauncher: ActivityResultLauncher<Intent>? = null
    var photoUri:Uri? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        Utils.setFullscreen(this)
        //Utils.requestPermission(this)
        Utils.LoadingDialogUtils.show(this@Chat, "加载预训练模型...")
        uCropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result: ActivityResult ->
            if (result.data != null) {
                imgPath = UCrop.getOutput(result.data!!)!!.path.toString()
                var bitmap = BitmapFactory.decodeFile(imgPath)
                Log.e("W", bitmap.width.toString())
                Log.e("H", bitmap.height.toString())
                if(bitmap.width != 224 || bitmap.height != 224){
                    Utils.showMsg(this@Chat, "原图片尺寸过小")
                    bitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
                }
                Utils.appViewModel!!.chatState.messages.add(MessageData(MessageRole.User, "", UUID.randomUUID(), imgPath))
                Thread{
                    Utils.appViewModel!!.chatState.requestImage(bitmapToBytes(bitmap))
                }.start()
                refreshView()
                sentImg = true
            } else {
                Utils.showMsg(this@Chat, "图片裁剪失败")
            }
        }
        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK && null != result.data) {
                imgPath =
                    Utils.getRealPathFromURI(this@Chat, result.data!!.data!!).toString()
                if ("null" == imgPath) {
                    Utils.showMsg(this, "相册图片加载失败")
                } else {
                    photoUri = Uri.fromFile(File(imgPath))
                    startCropActivity(photoUri)
                }
            }
        }
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                startCropActivity(photoUri)
            }
        }
        Utils.cachePath = externalCacheDir!!.path
        onBackPressedDispatcher.addCallback(this,object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed(){
                Utils.backPress(this@Chat, "连续返回两次回到初始界面"){
                    finish()
                }
            }
        })
        initConfigs()
    }

    private fun initConfigs(){
        Utils.requestPermission(this@Chat)
        Utils.appViewModel?.chatState?.setUpChat(this@Chat)
        chatBox = findViewById(R.id.chat_box)
        help = findViewById(R.id.help)
        config = findViewById(R.id.config)
        delete = findViewById(R.id.delete)
        send = findViewById(R.id.send)
        camera = findViewById(R.id.camera)
        label = findViewById(R.id.chat_label)
        report = findViewById(R.id.chat_report)
        input = findViewById(R.id.input)
        getImg = findViewById(R.id.getImg)
        val str = "Model : " + Utils.appViewModel?.chatState?.modelName?.value
        label?.text = str
        Utils.chatItemAdapter = ChatItemAdapter(this@Chat)
        chatBox?.adapter = Utils.chatItemAdapter
        help?.setOnClickListener { _->
            run {
                Utils.showHelp(this@Chat)
            }
        }
        config?.setOnClickListener { _->
        }
        delete?.setOnClickListener { _->
            sentImg = false
            imgPath = ""
            Utils.appViewModel?.chatState?.requestResetChat()
        }
        send?.setOnClickListener { _->
            if(Utils.appViewModel?.chatState?.chatable() == true){
                run {
                    if(Utils.appViewModel!!.chatState.modelName.value.endsWith("-V") && !sentImg){
                        Utils.showMsg(this@Chat, "请先发送图片")
                        return@run
                    }
                    val text = input?.text.toString().trim()
                    if(text == "" || text.isEmpty()){
                        Utils.showMsg(this@Chat, "输入文本为空")
                        return@run
                    }
                    Thread{
                        Utils.appViewModel?.chatState?.requestGenerate(text)
                    }.start()
                    input?.setText("")
                    Utils.closeInputMethod(this@Chat)
                }
            }else{
                Utils.showMsg(this, "模型未就绪")
            }
        }
        getImg?.setOnClickListener {
            run {
                if(sentImg){
                    Utils.showMsg(this@Chat, "一次对话仅能发送一张图片。如需更换图片，请开始新的对话。")
                    return@setOnClickListener
                }
                if(Utils.appViewModel?.chatState?.chatable() == true){
                    choosePhoto()
                }else{
                    Utils.showMsg(this, "模型未就绪")
                }
            }
        }
        camera?.setOnClickListener { _->
            run {
                if(sentImg){
                    Utils.showMsg(this@Chat, "一次对话仅能发送一张图片。如需更换图片，请开始新的对话。")
                    return@setOnClickListener
                }
                if(Utils.appViewModel?.chatState?.chatable() == true){
                    takePhoto()
                }else{
                    Utils.showMsg(this, "模型未就绪")
                }
            }
        }
        if(!Utils.appViewModel!!.chatState.modelName.value.endsWith("-V")){
            getImg!!.visibility = View.GONE
            camera!!.visibility = View.GONE
        }
    }
    fun updateReport(txt: String){
        runOnUiThread {
            val str = "Info: $txt"
            report!!.text = str
        }
    }
    fun refreshView(){
        runOnUiThread {
            Utils.chatItemAdapter?.notifyDataSetChanged()
            chatBox?.adapter = Utils.chatItemAdapter
        }
    }
    private fun choosePhoto() {
        if(!(ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)){
            Utils.showMsg(this@Chat, "未授予相册权限，选取图片失败")
            Utils.requestPermission(this@Chat)
            return
        }
        val intent = Intent(Intent.ACTION_PICK)
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        galleryLauncher!!.launch(intent)
    }
    private fun takePhoto() {
        if(ContextCompat.checkSelfPermission(this@Chat, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            Utils.showMsg(this@Chat, "未授予相机权限，打开相机失败")
            Utils.requestPermission(this@Chat)
            return
        }
        val outputImage: File = createJpgFile()!!
        imgPath = outputImage.path
        photoUri = FileProvider.getUriForFile(this@Chat, "com.modelbest.minicpm", outputImage)
        val intent = Intent("android.media.action.IMAGE_CAPTURE")
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        cameraLauncher!!.launch(intent)
    }
    private fun createJpgFile(): File? {
        val jpg = File(externalCacheDir, "output_temp.jpg")
        return try {
            if (jpg.exists() && jpg.delete()) {
                createJpgFile()
            } else if (jpg.createNewFile()) {
                jpg
            } else {
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
    private fun getUCropIntent(
        context: Context?, mediaScannerUri: Uri?,
        destination: Uri?, options: UCrop.Options
    ): Intent {
        val intent = Intent()
        val uCropBundle = options.optionBundle
        uCropBundle.putParcelable(UCrop.EXTRA_INPUT_URI, mediaScannerUri)
        uCropBundle.putParcelable(UCrop.EXTRA_OUTPUT_URI, destination)
        intent.putExtras(options.optionBundle)
        intent.setClass(context!!, UCropActivity::class.java)
        return intent
    }

    fun startCropActivity(source: Uri?){
        val options = UCrop.Options()
        options.setToolbarColor(ContextCompat.getColor(this@Chat, R.color.transparent))
        options.setStatusBarColor(ContextCompat.getColor(this@Chat, R.color.blue))
        options.setHideBottomControls(true)
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG)
        options.setCompressionQuality(100)
        options.withAspectRatio(1f, 1f)
        options.withMaxResultSize(224, 224)
        options.setToolbarTitle("请尽量裁剪出关键位置");
        val name = UUID.randomUUID().toString() + ".jpg"
        uCropLauncher!!.launch(
            getUCropIntent(
                this, source,
                Uri.fromFile(File(Utils.cachePath, "/$name")),
                options
            )
        )
    }
    private fun deleteCacheFiles() {
        for (file in File(externalCacheDir.toString()).listFiles()!!) {
            if (file.exists() && file.isFile) {
                println(file.path)
                file.delete()
            }
        }
    }

    override fun onDestroy() {
        deleteCacheFiles();
        super.onDestroy()
    }
}