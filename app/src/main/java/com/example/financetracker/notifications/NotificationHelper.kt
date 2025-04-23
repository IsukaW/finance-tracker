package com.example.financetracker.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.financetracker.R
import com.example.financetracker.activities.MainActivity
import com.example.financetracker.utils.PreferenceManager

class NotificationHelper(private val context: Context) {
    
    private val prefManager = PreferenceManager(context)
    
    companion object {
        const val CHANNEL_ID = "budget_alerts"
        const val BUDGET_NOTIFICATION_ID = 1001
        const val PERMISSION_REQUEST_CODE = 100
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        // Create the notification channel only on API 26+ (Android 8.0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Budget Alerts"
            val descriptionText = "Notifications about your budget status"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                enableLights(true)
            }
            
            // Register the channel with the system
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d("NotificationHelper", "Created notification channel: $CHANNEL_ID")
        }
    }
    
    fun showBudgetWarning(currentExpenses: Double, budget: Double, forceShow: Boolean = false) {
        // Check if notifications are enabled in preferences or force show is true
        if (!prefManager.areNotificationsEnabled() && !forceShow) {
            Log.d("NotificationHelper", "Notifications disabled in preferences, skipping budget warning")
            return
        }
        
        val percentage = (currentExpenses / budget) * 100
        val currency = prefManager.getCurrencyType()
        
        // Only show notification if above 80% of budget or if force show is true
        if (percentage < 80 && !forceShow) {
            Log.d("NotificationHelper", "Budget usage below threshold: ${percentage.toInt()}%, skipping notification")
            return
        }
        
        val title = if (percentage >= 100) 
            "Budget Exceeded!" 
        else 
            "Budget Alert"
            
        val message = if (percentage >= 100)
            "You've spent $currency${currentExpenses.toInt()} and exceeded your monthly budget of $currency${budget.toInt()}"
        else
            "You've spent $currency${currentExpenses.toInt()} which is ${percentage.toInt()}% of your monthly budget"
            
        // Create an intent for when the notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build the notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // Using system icon to ensure it exists
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500)) // Add vibration pattern
            
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            
            // Check for notification permission on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == 
                    android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    notificationManager.notify(BUDGET_NOTIFICATION_ID, builder.build())
                    Log.d("NotificationHelper", "Budget notification shown: $message")
                } else {
                    Log.e("NotificationHelper", "Notification permission not granted")
                }
            } else {
                notificationManager.notify(BUDGET_NOTIFICATION_ID, builder.build())
                Log.d("NotificationHelper", "Budget notification shown: $message")
            }
        } catch (e: SecurityException) {
            Log.e("NotificationHelper", "Failed to show notification", e)
        }
    }
    
    // This method can be used to directly show a notification bypassing permission checks
    // Useful for debugging notification issues
    fun showDebugNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) 
            .setContentTitle("Debug Notification")
            .setContentText("This is a test notification to verify if notifications are working.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            
        notificationManager.notify(1002, builder.build())
        Log.d("NotificationHelper", "Debug notification shown")
    }
}