package com.modelbest.minicpm

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
class MainActivity : ComponentActivity() {
    var image_path = ""
    var has_image = false
    var cpm_progress:ProgressBar? = null
    var cpm_v_progress:ProgressBar? = null
    lateinit var chatState: AppViewModel.ChatState
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
                Utils.appViewModel?.chatState?.modelName?.value = "MiniCPM"
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
                        Utils.appViewModel?.chatState?.modelName?.value = "MiniCPM-V"
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
}
