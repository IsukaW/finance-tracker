package com.example.financetracker

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.example.financetracker.utils.PreferenceManager

class FinanceTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Disable dark mode as we haven't designed for it
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Initialize preferences if first launch
        val sharedPrefs = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        if (sharedPrefs.getBoolean("first_launch", true)) {
            val prefManager = PreferenceManager(this)
            prefManager.saveCurrencyType("$")
            prefManager.saveMonthlyBudget(1000.0)
            prefManager.saveNotificationsEnabled(true)

            sharedPrefs.edit().putBoolean("first_launch", false).apply()
        }
    }
}