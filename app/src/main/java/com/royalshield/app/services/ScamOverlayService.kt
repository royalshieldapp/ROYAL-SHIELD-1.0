package com.royalshield.app.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.royalshield.app.R

/**
 * Service to show a floating overlay when a suspicious call is detected.
 */
class ScamOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val phoneNumber = intent?.getStringExtra("phone_number") ?: "Unknown"
        val riskScore = intent?.getIntExtra("risk_score", 0) ?: 0
        
        showOverlay(phoneNumber, riskScore)
        return START_NOT_STICKY
    }

    private fun showOverlay(phoneNumber: String, riskScore: Int) {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        val layoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        // In a real app, you'd have a specific layout XML. 
        // For this task, I'll programmatically adjust a generic layout if it exists or use a simple one.
        // Assuming we have a layout 'layout_scam_overlay' or creating one.
        
        try {
            overlayView = layoutInflater.inflate(R.layout.layout_scam_overlay, null)
        } catch (e: Exception) {
            // Fallback if layout doesn't exist yet (for the demo)
            return 
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            y = 100
        }

        overlayView?.apply {
            findViewById<TextView>(R.id.tv_scam_phone)?.text = phoneNumber
            findViewById<TextView>(R.id.tv_risk_percentage)?.text = "$riskScore%"
            
            findViewById<Button>(R.id.btn_close_overlay)?.setOnClickListener {
                stopSelf()
            }
        }

        windowManager?.addView(overlayView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (overlayView != null) {
            windowManager?.removeView(overlayView)
        }
    }
}
