package ca.yyx.hu.aap

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.support.v4.content.LocalBroadcastManager
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder

import ca.yyx.hu.App
import ca.yyx.hu.R
import ca.yyx.hu.aap.protocol.Screen
import ca.yyx.hu.aap.protocol.messages.TouchEvent
import ca.yyx.hu.aap.protocol.messages.VideoFocusEvent
import ca.yyx.hu.app.SurfaceActivity
import ca.yyx.hu.utils.AppLog
import ca.yyx.hu.utils.LocalIntent
import ca.yyx.hu.utils.Utils
import ca.yyx.hu.view.ProjectionView

class AapProjectionActivity : SurfaceActivity(), SurfaceHolder.Callback {
    private lateinit var projectionView: ProjectionView

    private val disconnectReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            finish()
        }
    }

    private val keyCodeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val event = intent.getParcelableExtra<KeyEvent>(LocalIntent.EXTRA_EVENT)
            onKeyEvent(event.keyCode, event.action == KeyEvent.ACTION_DOWN)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppLog.i("Headunit for Android Auto (tm) - Copyright 2011-2015 Michael A. Reid. All Rights Reserved...")

        projectionView = findViewById<ProjectionView>(R.id.surface)
        projectionView.setSurfaceCallback(this)
        projectionView.setOnTouchListener { _, event ->
            sendTouchEvent(event)
            true
        }
    }

    override fun onPause() {
        super.onPause()
        App.provide(this).hasVideoFocus = false
        LocalBroadcastManager.getInstance(this).unregisterReceiver(disconnectReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(keyCodeReceiver)
    }

    override fun onResume() {
        super.onResume()
        App.provide(this).hasVideoFocus = true
        LocalBroadcastManager.getInstance(this).registerReceiver(disconnectReceiver, LocalIntent.FILTER_DISCONNECT)
        LocalBroadcastManager.getInstance(this).registerReceiver(keyCodeReceiver, LocalIntent.FILTER_KEY_EVENT)
    }

    val transport: AapTransport
        get() = App.provide(this).transport

    override fun surfaceCreated(holder: SurfaceHolder) {

    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        transport.send(VideoFocusEvent(true, true))
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        transport.send(VideoFocusEvent(false, true))
    }

    private fun sendTouchEvent(event: MotionEvent) {

        val x = event.getX(0) / (projectionView.width / Screen.width.toFloat())
        val y = event.getY(0) / (projectionView.height / Screen.height.toFloat())

        if (x < 0 || y < 0 || x >= 65535 || y >= 65535) {
            AppLog.e("Invalid x: $x  y: $y")
            return
        }

        val action = TouchEvent.motionEventToAction(event)
        if (action == -1) {
            AppLog.e("event: $event (Unknown: ${event.actionMasked})  x: $x  y: $y")
            return
        }
        val ts = SystemClock.elapsedRealtime()
        transport.send(TouchEvent(ts, action, x.toInt(), y.toInt()))
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        AppLog.i("KeyCode: %d", keyCode)
        // PRes navigation on the screen
        onKeyEvent(keyCode, true)
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        AppLog.i("KeyCode: %d", keyCode)
        onKeyEvent(keyCode, false)
        return super.onKeyUp(keyCode, event)
    }

    private fun onKeyEvent(keyCode: Int, isPress: Boolean) {
        transport.sendButton(keyCode, isPress)
    }

    companion object {
        const val EXTRA_FOCUS = "focus"

        fun intent(context: Context): Intent {
            val aapIntent = Intent(context, AapProjectionActivity::class.java)
            aapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return aapIntent
        }
    }
}