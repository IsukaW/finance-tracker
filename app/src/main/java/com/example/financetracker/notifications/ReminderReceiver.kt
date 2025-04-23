package com.example.financetracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.financetracker.utils.PreferenceManager
import com.example.financetracker.utils.TransactionStorage
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ReminderReceiver", "Received reminder broadcast")
        
        val prefManager = PreferenceManager(context)
        // Only proceed if notifications are enabled
        if (!prefManager.areNotificationsEnabled()) {
            Log.d("ReminderReceiver", "Notifications disabled, skipping reminder")
            return
        }
        
        val transactionStorage = TransactionStorage(context)
        val notificationHelper = NotificationHelper(context)
        
        // Calculate monthly expenses
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val transactions = transactionStorage.getTransactions()
        
        val monthlyExpenses = transactions
            .filter {
                val cal = Calendar.getInstance().apply { timeInMillis = it.date }
                it.isExpense && cal.get(Calendar.MONTH) == currentMonth
            }
            .sumOf { it.amount }
        
        val budget = prefManager.getMonthlyBudget()
        if (budget > 0) {
            // Show budget warning notification
            notificationHelper.showBudgetWarning(monthlyExpenses, budget)
        }
    }
}