package com.njupt.multibiometricauth.facewithvoice

import android.Manifest
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.pm.ActivityInfo
import android.graphics.Point
import android.hardware.Camera
import android.os.Bundle
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import com.arcsoft.face.*
import com.arcsoft.face.enums.DetectFaceOrientPriority
import com.arcsoft.face.enums.DetectMode
import com.iflytek.cloud.*
import com.iflytek.cloud.record.PcmRecorder
import com.iflytek.cloud.record.PcmRecorder.PcmRecordListener
import com.iflytek.cloud.util.VerifierUtil
import com.njupt.multibiometricauth.Constants
import com.njupt.multibiometricauth.MMAApplication
import com.njupt.multibiometricauth.R
import com.njupt.multibiometricauth.SQLite.UserDatabaseHelper
import com.njupt.multibiometricauth.face.FaceConfigActivity
import com.njupt.multibiometricauth.face.faceserver.CompareResult
import com.njupt.multibiometricauth.face.faceserver.FaceServer
import com.njupt.multibiometricauth.face.model.DrawInfo
import com.njupt.multibiometricauth.face.model.FacePreviewInfo
import com.njupt.multibiometricauth.face.util.ConfigUtil
import com.njupt.multibiometricauth.face.util.DrawHelper
import com.njupt.multibiometricauth.face.util.camera.CameraHelper
import com.njupt.multibiometricauth.face.util.camera.CameraListener
import com.njupt.multibiometricauth.face.util.face.*
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_face_with_voice.*
import org.json.JSONObject
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class FaceWithVoiceActivity : FaceConfigActivity(), ViewTreeObserver.OnGlobalLayoutListener {
    companion object {
        private const val TAG = "FaceWithVoiceActivity"

        private const val MAX_DETECT_NUM = 10

        /**
         * ???FR??????????????????????????????FR?????????????????????
         */
        private const val WAIT_LIVENESS_INTERVAL = 100

        /**
         * ???????????????????????????ms???
         */
        private const val FAIL_RETRY_INTERVAL: Long = 1000

        /**
         * ????????????????????????
         */
        private const val MAX_RETRY_TIME = 3

        /**
         * ????????????????????????????????????
         */
        private const val REGISTER_STATUS_READY = 0

        /**
         * ?????????????????????????????????
         */
        private const val REGISTER_STATUS_PROCESSING = 1

        /**
         * ????????????????????????????????????????????????????????????
         */
        private const val REGISTER_STATUS_DONE = 2

        /**
         * ?????????????????????????????????
         */
        private val REGISTER_STATUS_ALREADY_REG = 3
    }

    private lateinit var cameraHelper: CameraHelper
    private lateinit var drawHelper: DrawHelper
    private var previewSize: Camera.Size? = null

    /**
     * ??????????????????????????????????????????????????????RGB??????????????????????????????????????????
     */
    private val rgbCameraID = Camera.CameraInfo.CAMERA_FACING_FRONT

    /**
     * VIDEO??????????????????????????????????????????????????????
     */
    private lateinit var ftEngine: FaceEngine

    /**
     * ???????????????????????????
     */
    private lateinit var frEngine: FaceEngine

    /**
     * IMAGE????????????????????????????????????????????????????????????
     */
    private lateinit var flEngine: FaceEngine

    // ??????????????????
    private lateinit var mIdVerifier: IdentityVerifier

    private var ftInitCode = -1
    private var frInitCode = -1
    private var flInitCode = -1
    private var faceHelper: FaceHelper? = null
    private var compareResultList: MutableList<CompareResult>? = null
    private var adapter: FaceSearchWithSimAdapter? = null

    /**
     * ?????????????????????
     */
    private val livenessDetect = false

    private var registerStatus = 0

    /**
     * ????????????????????????????????????
     */
    private val requestFeatureStatusMap = ConcurrentHashMap<Int, Int>()

    /**
     * ????????????????????????????????????????????????
     */
    private val extractErrorRetryMap = ConcurrentHashMap<Int, Int>()

    /**
     * ?????????????????????
     */
    private val livenessMap = ConcurrentHashMap<Int, Int>()

    /**
     * ??????????????????????????????????????????
     */
    private val livenessErrorRetryMap = ConcurrentHashMap<Int, Int>()

    private val getFeatureDelayedDisposables = CompositeDisposable()
    private val delayFaceTaskCompositeDisposable = CompositeDisposable()


    private val ACTION_REQUEST_PERMISSIONS = 0x001

    private lateinit var mUserDatabaseHelper: UserDatabaseHelper

    /**
     * ????????????
     */
    private val SIMILAR_THRESHOLD = 0.8f

    /**
     * ???????????????????????????
     */
    private val NEEDED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE
    )

    private var mToast: Toast? = null

    private val voiceRecMap =  mutableMapOf<String, Float>()

    // ????????????????????????
    private var mIsWorking = false

    // ??????????????????
    private var mCanIdentify = false

    // ???????????????
    private val SAMPLE_RATE = 16000

    // pcm?????????
    private var mPcmRecorder: PcmRecorder? = null

    // ???????????????
    private var mProDialog: ProgressDialog? = null

    // ?????????????????????
    private val mPwdType = 3

    // ???????????????????????????
    private var mIdentifyNumPwd = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_with_voice)

        mUserDatabaseHelper = UserDatabaseHelper(this)
        //????????????
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val attributes = window.attributes
        attributes.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        window.attributes = attributes

        // Activity???????????????????????????????????????
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED

        initView()

        //????????????????????????
        FaceServer.getInstance().init(this)

        //???????????????
        initVoiceEngine()

    }

    private fun initVoiceEngine() {
        mIdVerifier = IdentityVerifier.createVerifier(this) { errorCode ->
            if (ErrorCode.SUCCESS == errorCode) {
                showTip("???????????????????????????")
            } else {
                showTip("??????????????????????????????????????????$errorCode,???????????????https://www.xfyun.cn/document/error-code??????????????????")
            }
        }
    }

    private fun showTip(str: String) {
        runOnUiThread {
            mToast?.setText(str)
            mToast?.show()
        }
    }

    private fun initView() {
        mToast = Toast.makeText(this, "", Toast.LENGTH_LONG)

        initProDialog()

        single_camera_texture_preview.viewTreeObserver.addOnGlobalLayoutListener(this)

        compareResultList = mutableListOf()

        adapter = FaceSearchWithSimAdapter(compareResultList, this)

        single_camera_recycler_view_person.adapter = adapter

        resources.displayMetrics.let {
            val spanCount = it.widthPixels / (it.density * 100 + 0.5f)
            single_camera_recycler_view_person.layoutManager = GridLayoutManager(this, spanCount.toInt())
            single_camera_recycler_view_person.itemAnimator = DefaultItemAnimator()
        }

        mIdentifyNumPwd = VerifierUtil.generateNumberPassword(8)

        record_btn.setOnTouchListener(mPressTouchListener)

        voice_text_txv.text = mIdentifyNumPwd
    }

    private fun initProDialog() {
        mProDialog = ProgressDialog(this)
        mProDialog?.setCancelable(true)
        mProDialog?.setTitle("?????????")
        // cancel??????????????????????????????????????????

        mProDialog?.setOnCancelListener(DialogInterface.OnCancelListener {
            if (null != mIdVerifier) {
                mIdVerifier.cancel()
            }
        })
    }


    override fun onGlobalLayout() {
        single_camera_texture_preview.viewTreeObserver.removeOnGlobalLayoutListener(this)
        if (!checkPermissions(NEEDED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS)
        } else {
            initEngine()
            initCamera()
        }
    }

    private fun initCamera() {
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)

        val faceListener: FaceListener = object : FaceListener {
            override fun onFail(e: Exception) {
                Log.e(TAG, "onFail: " + e.message)
            }

            //??????FR?????????
            override fun onFaceFeatureInfoGet(faceFeature: FaceFeature?, requestId: Int, errorCode: Int) {
                //FR??????
                if (faceFeature != null) {
//                    Log.i(TAG, "onPreview: fr end = " + System.currentTimeMillis() + " trackId = " + requestId);
                    val liveness = livenessMap[requestId]
                    //??????????????????????????????????????????
                    if (!livenessDetect) {
                        searchFace(faceFeature, requestId)
                    } else if (liveness != null && liveness == LivenessInfo.ALIVE) {
                        searchFace(faceFeature, requestId)
                    } else {
                        //??????????????????????????????????????????????????????
                        if (requestFeatureStatusMap.containsKey(requestId)) {
                            Observable.timer(WAIT_LIVENESS_INTERVAL.toLong(), TimeUnit.MILLISECONDS)
                                    .subscribe(object : Observer<Long?> {
                                        var disposable: Disposable? = null
                                        override fun onSubscribe(d: Disposable) {
                                            disposable = d
                                            getFeatureDelayedDisposables.add(disposable!!)
                                        }

                                        override fun onError(e: Throwable) {}

                                        override fun onComplete() {
                                            getFeatureDelayedDisposables.remove(disposable!!)
                                        }

                                        override fun onNext(t: Long) {
                                            onFaceFeatureInfoGet(faceFeature, requestId, errorCode)
                                        }
                                    })
                        }
                    }
                } else {
                    if (increaseAndGetValue(extractErrorRetryMap, requestId) > MAX_RETRY_TIME) {
                        extractErrorRetryMap[requestId] = 0
                        // ?????????FaceInfo????????????????????????????????????????????????????????????RGB????????????????????????????????????
                        val msg: String = if (errorCode == ErrorInfo.MERR_FSDK_FACEFEATURE_LOW_CONFIDENCE_LEVEL) {
                            getString(R.string.low_confidence_level)
                        } else {
                            "ExtractCode:$errorCode"
                        }
                        faceHelper?.setName(requestId, getString(R.string.recognize_failed_notice, msg))
                        // ??????????????????????????????????????????????????????????????????????????????
                        requestFeatureStatusMap[requestId] = RequestFeatureStatus.FAILED
                        retryRecognizeDelayed(requestId)
                    } else {
                        requestFeatureStatusMap[requestId] = RequestFeatureStatus.TO_RETRY
                    }
                }
            }

            override fun onFaceLivenessInfoGet(livenessInfo: LivenessInfo?, requestId: Int, errorCode: Int) {
                if (livenessInfo != null) {
                    val liveness = livenessInfo.liveness
                    livenessMap[requestId] = liveness
                    // ??????????????????
                    if (liveness == LivenessInfo.NOT_ALIVE) {
                        faceHelper?.setName(requestId, getString(R.string.recognize_failed_notice, "NOT_ALIVE"))
                        // ?????? FAIL_RETRY_INTERVAL ??????????????????????????????UNKNOWN????????????????????????????????????????????????
                        retryLivenessDetectDelayed(requestId)
                    }
                } else {
                    if (increaseAndGetValue(livenessErrorRetryMap, requestId) > MAX_RETRY_TIME) {
                        livenessErrorRetryMap[requestId] = 0
                        // ?????????FaceInfo????????????????????????????????????????????????????????????RGB????????????????????????????????????
                        val msg: String = if (errorCode == ErrorInfo.MERR_FSDK_FACEFEATURE_LOW_CONFIDENCE_LEVEL) {
                            getString(R.string.low_confidence_level)
                        } else {
                            "ProcessCode:$errorCode"
                        }
                        faceHelper?.setName(requestId, getString(R.string.recognize_failed_notice, msg))
                        retryLivenessDetectDelayed(requestId)
                    } else {
                        livenessMap[requestId] = LivenessInfo.UNKNOWN
                    }
                }
            }
        }


        val cameraListener: CameraListener = object : CameraListener {
            override fun onCameraOpened(camera: Camera, cameraId: Int, displayOrientation: Int, isMirror: Boolean) {
                val lastPreviewSize = previewSize
                previewSize = camera.parameters.previewSize
                drawHelper = DrawHelper(previewSize!!.width, previewSize!!.height, single_camera_texture_preview.width, single_camera_texture_preview.height, displayOrientation, cameraId, isMirror, false, false)
                Log.i(TAG, "onCameraOpened: $drawHelper")
                // ????????????????????????????????????????????????????????????
                if (lastPreviewSize == null || lastPreviewSize.width != previewSize!!.width || lastPreviewSize.height != previewSize!!.height) {
                    var trackedFaceCount: Int? = null
                    // ??????????????????????????????

                    trackedFaceCount = faceHelper?.trackedFaceCount
                    faceHelper?.release()

                    faceHelper = FaceHelper.Builder()
                            .ftEngine(ftEngine)
                            .frEngine(frEngine)
                            .flEngine(flEngine)
                            .frQueueSize(MAX_DETECT_NUM)
                            .flQueueSize(MAX_DETECT_NUM)
                            .previewSize(previewSize)
                            .faceListener(faceListener)
                            .trackedFaceCount(trackedFaceCount
                                    ?: ConfigUtil.getTrackedFaceCount(applicationContext))
                            .build()
                }
            }

            override fun onPreview(nv21: ByteArray, camera: Camera) {
                if (single_camera_face_rect_view != null) {
                    single_camera_face_rect_view.clearFaceInfo()
                }
                val facePreviewInfoList = faceHelper?.onPreviewFrame(nv21)
                if (facePreviewInfoList != null && single_camera_face_rect_view != null) {
                    drawPreviewInfo(facePreviewInfoList)
                }
//                registerFace(nv21, facePreviewInfoList)
                clearLeftFace(facePreviewInfoList)
                if (facePreviewInfoList != null && facePreviewInfoList.size > 0 && previewSize != null) {
                    for (i in facePreviewInfoList.indices) {
                        val status = requestFeatureStatusMap[facePreviewInfoList[i].trackId]
                        /**
                         * ????????????????????????????????????????????????????????????????????????????????????????????????ANALYZING???????????????????????????ALIVE???NOT_ALIVE??????????????????????????????
                         */
                        if (livenessDetect && (status == null || status != RequestFeatureStatus.SUCCEED)) {
                            val liveness = livenessMap[facePreviewInfoList[i].trackId]
                            if (liveness == null
                                    || liveness != LivenessInfo.ALIVE && liveness != LivenessInfo.NOT_ALIVE && liveness != RequestLivenessStatus.ANALYZING) {
                                livenessMap[facePreviewInfoList[i].trackId] = RequestLivenessStatus.ANALYZING
                                faceHelper?.requestFaceLiveness(nv21, facePreviewInfoList[i].faceInfo, previewSize!!.width, previewSize!!.height, FaceEngine.CP_PAF_NV21, facePreviewInfoList[i].trackId, LivenessType.RGB)
                            }
                        }
                        /**
                         * ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                         * ??????????????????????????????????????????[FaceListener.onFaceFeatureInfoGet]?????????
                         */
                        if (status == null
                                || status == RequestFeatureStatus.TO_RETRY) {
                            requestFeatureStatusMap[facePreviewInfoList[i].trackId] = RequestFeatureStatus.SEARCHING
                            faceHelper?.requestFaceFeature(nv21, facePreviewInfoList[i].faceInfo, previewSize!!.width, previewSize!!.height, FaceEngine.CP_PAF_NV21, facePreviewInfoList[i].trackId)
                            //                            Log.i(TAG, "onPreview: fr start = " + System.currentTimeMillis() + " trackId = " + facePreviewInfoList.get(i).getTrackedFaceCount());
                        }
                    }
                }
            }

            override fun onCameraClosed() {
                Log.i(TAG, "onCameraClosed: ")
            }

            override fun onCameraError(e: Exception) {
                Log.i(TAG, "onCameraError: " + e.message)
            }

            override fun onCameraConfigurationChanged(cameraID: Int, displayOrientation: Int) {
                drawHelper.cameraDisplayOrientation = displayOrientation
                Log.i(TAG, "onCameraConfigurationChanged: $cameraID  $displayOrientation")
            }
        }

        cameraHelper = CameraHelper.Builder()
                .previewViewSize(Point(single_camera_texture_preview.measuredWidth, single_camera_texture_preview.measuredHeight))
                .rotation(windowManager.defaultDisplay.rotation)
                .specificCameraId(rgbCameraID ?: Camera.CameraInfo.CAMERA_FACING_FRONT)
                .isMirror(false)
                .previewOn(single_camera_texture_preview)
                .cameraListener(cameraListener)
                .build()
        cameraHelper.init()
        cameraHelper.start()
    }

    private fun drawPreviewInfo(facePreviewInfoList: List<FacePreviewInfo>) {
        val drawInfoList: MutableList<DrawInfo> = ArrayList()
        for (i in facePreviewInfoList.indices) {
            val name = faceHelper?.getName(facePreviewInfoList[i].trackId)
            val liveness = livenessMap[facePreviewInfoList[i].trackId]
            val recognizeStatus = requestFeatureStatusMap[facePreviewInfoList[i].trackId]

            // ?????????????????????????????????????????????
            var color = RecognizeColor.COLOR_UNKNOWN
            if (recognizeStatus != null) {
                if (recognizeStatus == RequestFeatureStatus.FAILED) {
                    color = RecognizeColor.COLOR_FAILED
                }
                if (recognizeStatus == RequestFeatureStatus.SUCCEED) {
                    color = RecognizeColor.COLOR_SUCCESS
                }
            }
            if (liveness != null && liveness == LivenessInfo.NOT_ALIVE) {
                color = RecognizeColor.COLOR_FAILED
            }
            drawInfoList.add(DrawInfo(drawHelper.adjustRect(facePreviewInfoList[i].faceInfo.rect),
                    GenderInfo.UNKNOWN, AgeInfo.UNKNOWN_AGE, liveness
                    ?: LivenessInfo.UNKNOWN, color,
                    name ?: facePreviewInfoList[i].trackId.toString()))
        }
        drawHelper.draw(single_camera_face_rect_view, drawInfoList)
    }

    /**
     * ?????? FAIL_RETRY_INTERVAL ????????????????????????
     *
     * @param requestId ??????ID
     */
    private fun retryRecognizeDelayed(requestId: Int) {
        requestFeatureStatusMap[requestId] = RequestFeatureStatus.FAILED
        Observable.timer(FAIL_RETRY_INTERVAL, TimeUnit.MILLISECONDS)
                .subscribe(object : Observer<Long?> {
                    var disposable: Disposable? = null
                    override fun onSubscribe(d: Disposable) {
                        disposable = d
                        delayFaceTaskCompositeDisposable.add(disposable!!)
                    }

                    override fun onError(e: Throwable) {
                        e.printStackTrace()
                    }

                    override fun onComplete() {
                        // ????????????????????????????????????FAILED????????????????????????????????????????????????
                        faceHelper?.setName(requestId, Integer.toString(requestId))
                        requestFeatureStatusMap[requestId] = RequestFeatureStatus.TO_RETRY
                        delayFaceTaskCompositeDisposable.remove(disposable!!)
                    }

                    override fun onNext(t: Long) {

                    }
                })
    }

    //??????????????????????????????
    private fun registerFace(nv21: ByteArray, facePreviewInfoList: List<FacePreviewInfo>?) {
        if (registerStatus == REGISTER_STATUS_READY && facePreviewInfoList != null && facePreviewInfoList.isNotEmpty()) {
            registerStatus = REGISTER_STATUS_PROCESSING
            Observable.create<Boolean> { emitter ->
                val userName = (application as MMAApplication).getProp(Constants.USERNAME)
                val success = FaceServer.getInstance().registerNv21(this, nv21.clone(), previewSize!!.width, previewSize!!.height,
                        facePreviewInfoList[0].faceInfo, if (TextUtils.isEmpty(userName)) "registered " + faceHelper?.trackedFaceCount else userName)
                emitter.onNext(success)
            }
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : Observer<Boolean> {
                        override fun onSubscribe(d: Disposable) {}
                        override fun onNext(success: Boolean) {
                            val result = if (success) "register success!" else "register failed!"
                            showToast(result)
                            registerStatus = REGISTER_STATUS_DONE
                        }

                        override fun onError(e: Throwable) {
                            e.printStackTrace()
                            showToast("register failed!")
                            registerStatus = REGISTER_STATUS_DONE
                        }

                        override fun onComplete() {}
                    })
        }
    }

    /**
     * ???????????????????????????
     *
     * @param facePreviewInfoList ?????????trackId??????
     */
    private fun clearLeftFace(facePreviewInfoList: List<FacePreviewInfo>?) {
        if (compareResultList != null) {
            for (i in compareResultList!!.indices.reversed()) {
                if (!requestFeatureStatusMap.containsKey(compareResultList!![i].trackId)) {
                    compareResultList?.removeAt(i)
                    adapter!!.notifyItemRemoved(i)
                }
            }
        }
        if (facePreviewInfoList == null || facePreviewInfoList.isEmpty()) {
            requestFeatureStatusMap.clear()
            livenessMap.clear()
            livenessErrorRetryMap.clear()
            extractErrorRetryMap.clear()
            if (getFeatureDelayedDisposables != null) {
                getFeatureDelayedDisposables.clear()
            }
            return
        }
        val keys = requestFeatureStatusMap.keys()
        while (keys.hasMoreElements()) {
            val key = keys.nextElement()
            var contained = false
            for (facePreviewInfo in facePreviewInfoList) {
                if (facePreviewInfo.trackId == key) {
                    contained = true
                    break
                }
            }
            if (!contained) {
                requestFeatureStatusMap.remove(key)
                livenessMap.remove(key)
                livenessErrorRetryMap.remove(key)
                extractErrorRetryMap.remove(key)
            }
        }
    }

    /**
     * ?????? FAIL_RETRY_INTERVAL ????????????????????????
     *
     * @param requestId ??????ID
     */
    private fun retryLivenessDetectDelayed(requestId: Int) {
        Observable.timer(FAIL_RETRY_INTERVAL, TimeUnit.MILLISECONDS)
                .subscribe(object : Observer<Long?> {
                    var disposable: Disposable? = null
                    override fun onSubscribe(d: Disposable) {
                        disposable = d
                        delayFaceTaskCompositeDisposable.add(disposable!!)
                    }

                    override fun onNext(aLong: Long) {}
                    override fun onError(e: Throwable) {
                        e.printStackTrace()
                    }

                    override fun onComplete() {
                        // ????????????????????????UNKNOWN????????????????????????????????????????????????
                        if (livenessDetect) {
                            faceHelper?.setName(requestId, Integer.toString(requestId))
                        }
                        livenessMap[requestId] = LivenessInfo.UNKNOWN
                        delayFaceTaskCompositeDisposable.remove(disposable!!)
                    }
                })
    }

    private fun searchFace(frFace: FaceFeature, requestId: Int) {
        Observable
                .create<List<CompareResult>> { emitter ->
                    val compareResult = FaceServer.getInstance().getTopNOfFaceLib(frFace, SIMILAR_THRESHOLD)
                    emitter.onNext(compareResult)
                }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Observer<List<CompareResult>> {
                    override fun onSubscribe(d: Disposable) {}

                    override fun onNext(compareResult: List<CompareResult>) {
                        if (compareResult.isNullOrEmpty()) {
                            requestFeatureStatusMap[requestId] = RequestFeatureStatus.FAILED
                            faceHelper?.setName(requestId, "??????????????????$SIMILAR_THRESHOLD ??????")
                            retryRecognizeDelayed(requestId)
                        } else {
                            requestFeatureStatusMap[requestId] = RequestFeatureStatus.SUCCEED
                            faceHelper?.setName(requestId, "???????????????$SIMILAR_THRESHOLD ??????${compareResult.size} ???")
                            compareResultList?.clear()
                            compareResult.forEach {
                                it.trackId = requestId
                                //???map??????????????????????????????
                                it.voiceSimilar = queryVoiceSimiWithUsrName(it.userName) ?: 0f
                                compareResultList?.add(it)
                                adapter?.notifyDataSetChanged()
                                Log.d(TAG, "find similar person: $compareResultList")
                            }
                        }
                    }

                    override fun onError(e: Throwable) {
                        faceHelper?.setName(requestId, getString(R.string.recognize_failed_notice, "NOT_REGISTERED"))
                        retryRecognizeDelayed(requestId)
                    }

                    override fun onComplete() {}
                })
    }

    private fun queryVoiceSimiWithUsrName(userName: String?) = voiceRecMap[userName]


    private fun initEngine() {
        ftEngine = FaceEngine()
        ftInitCode = ftEngine.init(this, DetectMode.ASF_DETECT_MODE_VIDEO, ConfigUtil.getFtOrient(this),
                16, MAX_DETECT_NUM, FaceEngine.ASF_FACE_DETECT)

        frEngine = FaceEngine()
        frInitCode = frEngine.init(this, DetectMode.ASF_DETECT_MODE_IMAGE, DetectFaceOrientPriority.ASF_OP_0_ONLY,
                16, MAX_DETECT_NUM, FaceEngine.ASF_FACE_RECOGNITION)

        flEngine = FaceEngine()
        flInitCode = flEngine.init(this, DetectMode.ASF_DETECT_MODE_IMAGE, DetectFaceOrientPriority.ASF_OP_0_ONLY,
                16, MAX_DETECT_NUM, FaceEngine.ASF_LIVENESS)

        Log.i(TAG, "initEngine:  init: $ftInitCode")

        if (ftInitCode != ErrorInfo.MOK) {
            val error = getString(R.string.specific_engine_init_failed, "ftEngine", ftInitCode)
            Log.i(TAG, "initEngine: $error")
            showToast(error)
        }
        if (frInitCode != ErrorInfo.MOK) {
            val error = getString(R.string.specific_engine_init_failed, "frEngine", frInitCode)
            Log.i(TAG, "initEngine: $error")
            showToast(error)
        }
        if (flInitCode != ErrorInfo.MOK) {
            val error = getString(R.string.specific_engine_init_failed, "flEngine", flInitCode)
            Log.i(TAG, "initEngine: $error")
            showToast(error)
        }
    }

    private fun unInitEngine(): Unit {
        if (ftInitCode == ErrorInfo.MOK) {
            synchronized(ftEngine) {
                val ftUnInitCode = ftEngine.unInit()
                Log.i(TAG, "unInitEngine: $ftUnInitCode")
            }
        }
        if (frInitCode == ErrorInfo.MOK) {
            synchronized(frEngine) {
                val frUnInitCode = frEngine.unInit()
                Log.i(TAG, "unInitEngine: $frUnInitCode")
            }
        }
        if (flInitCode == ErrorInfo.MOK) {
            synchronized(flEngine) {
                val flUnInitCode = flEngine.unInit()
                Log.i(TAG, "unInitEngine: $flUnInitCode")
            }
        }
    }

    override fun onDestroy() {
        cameraHelper.release()
        unInitEngine()
        getFeatureDelayedDisposables.clear()
        delayFaceTaskCompositeDisposable.clear()
        faceHelper?.trackedFaceCount?.let { ConfigUtil.setTrackedFaceCount(this, it) }
        faceHelper?.release()
        FaceServer.getInstance().unInit()

        mIdVerifier.destroy()
        super.onDestroy()
    }

    /**
     * ???map???key?????????value???1??????
     *
     * @param countMap map
     * @param key      key
     * @return ???1??????value
     */
    fun increaseAndGetValue(countMap: ConcurrentHashMap<Int, Int>, key: Int): Int {
        var value = countMap[key]
        if (value == null) {
            value = 0
        }
        countMap[key] = ++value
        return value
    }

    /**
     * ???????????????
     */
    private val mPressTouchListener = OnTouchListener { v, event ->
        if (null == mIdVerifier) {
            // ???????????????????????? 21001 ?????????????????????????????? http://bbs.xfyun.cn/forum.php?mod=viewthread&tid=9688
            showTip("?????????????????????????????? libmsc.so ??????????????????????????? createUtility ???????????????")
            return@OnTouchListener false
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> if (!mIsWorking) {
                vocalSearch()
                mIsWorking = true
                mCanIdentify = true
                if (mCanIdentify) {
                    try {
                        mPcmRecorder = PcmRecorder(SAMPLE_RATE, 40)
                        mPcmRecorder?.startRecording(mPcmRecordListener)
                    } catch (e: SpeechError) {
                        e.printStackTrace()
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                v.performClick()
                if (mCanIdentify) {
                    showProDialog("?????????...")
                }
                mIdVerifier.stopWrite("ivp")
                mPcmRecorder?.stopRecord(true)
                mIsWorking = false
            }
            else -> {
            }
        }
        false
    }

    /**
     * ??????????????????
     */
    private val mPcmRecordListener: PcmRecordListener = object : PcmRecordListener {
        override fun onRecordStarted(success: Boolean) {}
        override fun onRecordReleased() {}
        override fun onRecordBuffer(data: ByteArray, offset: Int, length: Int) {
            val params = StringBuffer()
            // ???????????????????????????????????????????????????
            params.append("ptxt=$mIdentifyNumPwd,")
            params.append("pwdt=$mPwdType,")
            params.append(",group_id=${Constants.VoiceGroupId},topc=3")
            mIdVerifier.writeData("ivp", params.toString(), data, 0, length)
        }

        override fun onError(e: SpeechError) {
            dismissProDialog()
            mCanIdentify = false
        }
    }

    private fun vocalSearch() {
        mIdVerifier.setParameter(SpeechConstant.PARAMS, null)
        // ??????????????????
        // ??????????????????
        mIdVerifier.setParameter(SpeechConstant.MFV_SCENES, "ivp")
        // ??????????????????
        // ??????????????????
        mIdVerifier.setParameter(SpeechConstant.MFV_SST, "identify")
        // ?????????ID
        // ?????????ID
        mIdVerifier.setParameter("group_id", Constants.VoiceGroupId)
        // ??????????????????????????????
        // ??????????????????????????????
        mIdVerifier.startWorking(mSearchListener)
    }

    /**
     * ?????????????????????
     */
    private val mSearchListener: IdentityListener = object : IdentityListener {
        override fun onResult(result: IdentityResult, islast: Boolean) {
            Log.d(TAG, result.resultString)
            dismissProDialog()
            mIsWorking = false
            handleResult(result)
        }

        override fun onEvent(eventType: Int, arg1: Int, arg2: Int, obj: Bundle?) {
            if (SpeechEvent.EVENT_VOLUME == eventType) {
                showTip("?????????$arg1")
            } else if (SpeechEvent.EVENT_VAD_EOS == eventType) {
                showTip("????????????")
            }
        }

        override fun onError(error: SpeechError) {
            mCanIdentify = false
            dismissProDialog()
            mIsWorking = false
            showTip(error.getPlainDescription(true))
        }
    }

    /**
     * ???????????????????????????
     */
    private fun handleResult(result: IdentityResult) {
        result.resultString.run {
            JSONObject(this)
        }.let {
            if (it.getInt("ret") == ErrorCode.SUCCESS) {
                val ifv_result = it.getJSONObject("ifv_result")
                val candidates = ifv_result.getJSONArray("candidates")
                for (i in 0 until candidates.length()) {
                    val obj = candidates.getJSONObject(i)
                    obj.optString("user").apply {
                        val score = obj.optDouble("score").toFloat()
                        val usrName = mUserDatabaseHelper.queryUserWithPhoneNumber(this)
                        usrName?.let {
                            voiceRecMap[it] =  score
                        }
                        Log.d(TAG, "handleResult: userName: $this score: $score")
                    }
                    refreshAdapter()
                    adapter?.notifyDataSetChanged()
                }
            }
        }
    }

    private fun refreshAdapter() {
        adapter?.resultList?.let {
            it.forEach { cr ->
                voiceRecMap[cr.userName]?.let {
                    cr.voiceSimilar = it
                }
            }
        }
        adapter?.notifyDataSetChanged()
    }

    private fun dismissProDialog() {
        mProDialog?.dismiss()
    }

    private fun showProDialog(msg: String) {
        mProDialog?.setMessage(msg)
        mProDialog?.show()
    }
}