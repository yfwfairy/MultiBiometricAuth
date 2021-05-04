package com.njupt.multibiometricauth.facewithvoice

import android.Manifest
import android.content.pm.ActivityInfo
import android.hardware.Camera
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.Button
import android.widget.GridLayout
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import com.arcsoft.face.FaceEngine
import com.njupt.multibiometricauth.R
import com.njupt.multibiometricauth.face.BaseActivity
import com.njupt.multibiometricauth.face.faceserver.CompareResult
import com.njupt.multibiometricauth.face.faceserver.FaceServer
import com.njupt.multibiometricauth.face.util.DrawHelper
import com.njupt.multibiometricauth.face.util.camera.CameraHelper
import com.njupt.multibiometricauth.face.util.face.FaceHelper
import com.njupt.multibiometricauth.face.widget.FaceRectView
import com.njupt.multibiometricauth.face.widget.FaceSearchResultAdapter
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_face_with_voice.*
import java.util.concurrent.ConcurrentHashMap

class FaceWithVoiceActivity : BaseActivity() , ViewTreeObserver.OnGlobalLayoutListener{
    companion object {
        private val TAG = "FaceWithVoiceActivity"

        private val MAX_DETECT_NUM = 10

        /**
         * 当FR成功，活体未成功时，FR等待活体的时间
         */
        private const val WAIT_LIVENESS_INTERVAL = 100

        /**
         * 失败重试间隔时间（ms）
         */
        private const val FAIL_RETRY_INTERVAL: Long = 1000

        /**
         * 出错重试最大次数
         */
        private const val MAX_RETRY_TIME = 3

        /**
         * 注册人脸状态码，准备注册
         */
        private val REGISTER_STATUS_READY = 0

        /**
         * 注册人脸状态码，注册中
         */
        private val REGISTER_STATUS_PROCESSING = 1

        /**
         * 注册人脸状态码，注册结束（无论成功失败）
         */
        private val REGISTER_STATUS_DONE = 2

        /**
         * 注册人脸状态码，已注册
         */
        private val REGISTER_STATUS_ALREADY_REG = 3
    }

    private val cameraHelper: CameraHelper? = null
    private val drawHelper: DrawHelper? = null
    private val previewSize: Camera.Size? = null

    /**
     * 优先打开的摄像头，本界面主要用于单目RGB摄像头设备，因此默认打开前置
     */
    private val rgbCameraID = Camera.CameraInfo.CAMERA_FACING_FRONT

    /**
     * VIDEO模式人脸检测引擎，用于预览帧人脸追踪
     */
    private val ftEngine: FaceEngine? = null

    /**
     * 用于特征提取的引擎
     */
    private val frEngine: FaceEngine? = null

    /**
     * IMAGE模式活体检测引擎，用于预览帧人脸活体检测
     */
    private val flEngine: FaceEngine? = null

    private val ftInitCode = -1
    private val frInitCode = -1
    private val flInitCode = -1
    private val faceHelper: FaceHelper? = null
    private var compareResultList: List<CompareResult>? = null
    private var adapter: FaceSearchResultAdapter? = null

    /**
     * 活体检测的开关
     */
    private val livenessDetect = true

    private val registerStatus = 0

    /**
     * 用于记录人脸识别相关状态
     */
    private val requestFeatureStatusMap = ConcurrentHashMap<Int, Int>()

    /**
     * 用于记录人脸特征提取出错重试次数
     */
    private val extractErrorRetryMap = ConcurrentHashMap<Int, Int>()

    /**
     * 用于存储活体值
     */
    private val livenessMap = ConcurrentHashMap<Int, Int>()

    /**
     * 用于存储活体检测出错重试次数
     */
    private val livenessErrorRetryMap = ConcurrentHashMap<Int, Int>()

    private val getFeatureDelayedDisposables = CompositeDisposable()
    private val delayFaceTaskCompositeDisposable = CompositeDisposable()


    private val ACTION_REQUEST_PERMISSIONS = 0x001

    /**
     * 识别阈值
     */
    private val SIMILAR_THRESHOLD = 0.8f

    /**
     * 所需的所有权限信息
     */
    private val NEEDED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_with_voice)

        //保持亮屏
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val attributes = window.attributes
        attributes.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        window.attributes = attributes

        // Activity启动后就锁定为启动时的方向
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED

        //本地人脸库初始化
        FaceServer.getInstance().init(this)

        initView()
    }

    private fun initView() {
        single_camera_texture_preview.viewTreeObserver.addOnGlobalLayoutListener(this)

        compareResultList = listOf()

        adapter = FaceSearchResultAdapter(compareResultList,this)

        single_camera_recycler_view_person.adapter = adapter

        resources.displayMetrics.let {
            val spanCount = it.widthPixels/ (it.density *100 + 0.5f)
            single_camera_recycler_view_person.layoutManager = GridLayoutManager(this, spanCount.toInt())
            single_camera_recycler_view_person.itemAnimator = DefaultItemAnimator()
        }

    }


    override fun onGlobalLayout() {
        single_camera_texture_preview.viewTreeObserver.removeOnGlobalLayoutListener(this)
        if (!checkPermissions(NEEDED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS)
        } else {
//            initEngine()
//            initCamera()
        }
    }
}