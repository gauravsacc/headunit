package ca.yyx.hu.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import ca.yyx.hu.App

import ca.yyx.hu.location.GpsLocationService

/**
 * @author algavris
 * *
 * @date 18/12/2016.
 */
class BootCompleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        val h = Handler()
        h.postDelayed({
            App.get(context).startService(GpsLocationService.intent(context))
        }, 10000)
    }
}
