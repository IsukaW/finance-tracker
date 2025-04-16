package com.example.financetracker.utils

import android.content.Context

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
}