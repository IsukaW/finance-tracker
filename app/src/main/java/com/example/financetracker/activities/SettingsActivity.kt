package com.example.financetracker.activities

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.financetracker.R
import com.example.financetracker.databinding.ActivitySettingsBinding
import com.example.financetracker.notifications.ReminderReceiver
import com.example.financetracker.utils.BackupManager
import com.example.financetracker.utils.PreferenceManager
import com.example.financetracker.utils.TransactionStorage
import java.util.Calendar

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var transactionStorage: TransactionStorage
    private lateinit var backupManager: BackupManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager(this)
        transactionStorage = TransactionStorage(this)
        backupManager = BackupManager(this)

        setupCurrencySpinner()
        loadSettings()
        setupBottomNavigation()

        binding.buttonSave.setOnClickListener { saveSettings() }
        binding.buttonBackup.setOnClickListener { backupData() }
        binding.buttonRestore.setOnClickListener { showRestoreDialog() }
    }

    private fun setupCurrencySpinner() {
        val currencies = resources.getStringArray(R.array.currencies)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCurrency.adapter = adapter
    }

    private fun loadSettings() {
        // Load budget
        val budget = preferenceManager.getMonthlyBudget()
        if (budget > 0) {
            binding.editTextBudget.setText(budget.toString())
        }

        // Load currency
        val currency = preferenceManager.getCurrencyType()
        val currencies = resources.getStringArray(R.array.currencies)
        val currencyPosition = currencies.indexOf(currency)
        if (currencyPosition != -1) {
            binding.spinnerCurrency.setSelection(currencyPosition)
        }

        // Load notification setting
        binding.switchNotifications.isChecked = preferenceManager.areNotificationsEnabled()
    }

    private fun saveSettings() {
        // Save budget
        val budgetStr = binding.editTextBudget.text.toString().trim()
        if (budgetStr.isNotEmpty()) {
            try {
                val budget = budgetStr.toDouble()
                preferenceManager.saveMonthlyBudget(budget)
            } catch (e: NumberFormatException) {
                binding.editTextBudget.error = "Invalid budget amount"
                return
            }
        } else {
            preferenceManager.saveMonthlyBudget(0.0)
        }

        // Save currency
        val currency = binding.spinnerCurrency.selectedItem.toString()
        preferenceManager.saveCurrencyType(currency)

        // Save notification setting
        val notificationsEnabled = binding.switchNotifications.isChecked
        preferenceManager.saveNotificationsEnabled(notificationsEnabled)

        if (notificationsEnabled) {
            scheduleReminder()
        } else {
            cancelReminder()
        }

        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun scheduleReminder() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Set reminder for 8:00 PM every day
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)

            // If current time is past 8:00 PM, schedule for tomorrow
            if (timeInMillis < System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun cancelReminder() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
    }

    private fun backupData() {
        val transactions = transactionStorage.getTransactions()
        val success = backupManager.exportData(transactions)

        if (success) {
            Toast.makeText(this, "Backup created successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to create backup", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRestoreDialog() {
        val backupFiles = backupManager.getBackupFiles()

        if (backupFiles.isEmpty()) {
            Toast.makeText(this, "No backup files found", Toast.LENGTH_SHORT).show()
            return
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Restore Backup")
            .setItems(backupFiles.toTypedArray()) { _, which ->
                restoreData(backupFiles[which])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun restoreData(fileName: String) {
        val transactions = backupManager.importData(fileName)

        if (transactions != null) {
            transactionStorage.saveTransactions(transactions)
            Toast.makeText(this, "Data restored successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to restore data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.menu_analysis -> {
                    startActivity(Intent(this, AnalysisActivity::class.java))
                    finish()
                    true
                }
                R.id.menu_settings -> {
                    // Already in SettingsActivity, no need to do anything
                    true
                }
                else -> false
            }
        }
        
        // Set settings as selected
        binding.bottomNavigationView.selectedItemId = R.id.menu_settings
    }
}