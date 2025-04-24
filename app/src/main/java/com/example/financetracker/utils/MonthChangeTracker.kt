package com.example.financetracker.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.util.Calendar

/**
 * Utility class to track month changes and trigger appropriate actions
 * when a new month begins.
 */
class MonthChangeTracker(private val context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "month_tracker_preferences", Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_LAST_MONTH = "last_month"
        private const val KEY_LAST_YEAR = "last_year"
        private const val TAG = "MonthChangeTracker"
    }
    
    /**
     * Checks if the current month is different from the last recorded month.
     * @return true if a month change is detected, false otherwise
     */
    fun hasMonthChanged(): Boolean {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        val lastMonth = sharedPreferences.getInt(KEY_LAST_MONTH, -1)
        val lastYear = sharedPreferences.getInt(KEY_LAST_YEAR, -1)
        
        // If this is the first run, save current month and year
        if (lastMonth == -1 || lastYear == -1) {
            saveCurrentMonth(currentMonth, currentYear)
            return false
        }
        
        // Check if month has changed
        val hasChanged = (currentMonth != lastMonth || currentYear != lastYear)
        
        if (hasChanged) {
            Log.d(TAG, "Month changed from $lastMonth/$lastYear to $currentMonth/$currentYear")
            saveCurrentMonth(currentMonth, currentYear)
        }
        
        return hasChanged
    }
    
    /**
     * Saves the current month and year to preferences
     */
    private fun saveCurrentMonth(month: Int, year: Int) {
        sharedPreferences.edit()
            .putInt(KEY_LAST_MONTH, month)
            .putInt(KEY_LAST_YEAR, year)
            .apply()
        
        Log.d(TAG, "Saved current month/year: $month/$year")
    }
    
    /**
     * Handles all necessary actions when a new month begins
     */
    fun handleMonthChange() {
        Log.d(TAG, "Handling month change...")
        
        // Archive previous month's transactions or tag them as "previous month"
        // Reset monthly budget progress
        // Trigger notifications about new month
        // Other month-change related tasks
        
        val transactionStorage = TransactionStorage(context)
        transactionStorage.archivePreviousMonthTransactions()
        
        val preferenceManager = PreferenceManager(context)
        preferenceManager.resetMonthlyStats()
    }
}
