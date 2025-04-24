package com.example.financetracker.utils

import android.content.Context
import android.util.Log

class PreferenceManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences(
        "finance_preferences", Context.MODE_PRIVATE
    )

    fun saveMonthlyBudget(budget: Double) {
        sharedPreferences.edit().putFloat("monthly_budget", budget.toFloat()).apply()
    }

    fun getMonthlyBudget(): Double {
        return sharedPreferences.getFloat("monthly_budget", 0f).toDouble()
    }

    fun saveCurrencyType(currency: String) {
        sharedPreferences.edit().putString("currency_type", currency).apply()
    }

    fun getCurrencyType(): String {
        return sharedPreferences.getString("currency_type", "$") ?: "$"
    }

    fun saveNotificationsEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("notifications_enabled", enabled).apply()
    }

    fun areNotificationsEnabled(): Boolean {
        return sharedPreferences.getBoolean("notifications_enabled", true)
    }

    /**
     * Reset monthly statistics when a new month begins
     */
    fun resetMonthlyStats() {
        // Reset monthly expense tracking
        sharedPreferences.edit()
            .putFloat("current_month_expenses", 0f)
            .putBoolean("budget_warning_shown", false)
            .apply()

        Log.d("PreferenceManager", "Reset monthly statistics for new month")
    }
}