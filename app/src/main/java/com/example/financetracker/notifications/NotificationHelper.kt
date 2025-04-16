package com.example.financetracker.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.financetracker.R
import com.example.financetracker.activities.MainActivity

class NotificationHelper(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager

    companion object {
        const val CHANNEL_ID = "finance_tracker_channel"
        const val BUDGET_NOTIFICATION_ID = 1001
        const val DAILY_REMINDER_ID = 1002
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Finance Tracker Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Budget alerts and daily reminders"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showBudgetWarning(spent: Double, budget: Double) {
        val percentage = (spent / budget) * 100

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_expense)
            .setContentTitle("Budget Alert")
            .setContentText("You've spent ${percentage.toInt()}% of your monthly budget")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(BUDGET_NOTIFICATION_ID, notification)
    }

    fun showDailyReminder() {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_expense)
            .setContentTitle("Daily Reminder")
            .setContentText("Don't forget to record today's expenses!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(DAILY_REMINDER_ID, notification)
    }
}