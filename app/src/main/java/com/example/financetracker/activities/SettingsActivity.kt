package com.example.financetracker.activities

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.financetracker.R
import com.example.financetracker.databinding.ActivitySettingsBinding
import com.example.financetracker.models.User
import com.example.financetracker.notifications.ReminderReceiver
import com.example.financetracker.utils.BackupManager
import com.example.financetracker.utils.PreferenceManager
import com.example.financetracker.utils.TransactionStorage
import com.example.financetracker.utils.UserRepository
import java.util.Calendar

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var transactionStorage: TransactionStorage
    private lateinit var backupManager: BackupManager
    private lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        preferenceManager = PreferenceManager(this)
        transactionStorage = TransactionStorage(this)
        backupManager = BackupManager(this)
        userRepository = UserRepository(this)

        loadUserProfile()
        setupCurrencySpinner()
        loadSettings()
        setupNavigation()
        setupProfileButtons()

        binding.buttonSave.setOnClickListener { saveSettings() }
        binding.buttonBackup.setOnClickListener { backupData() }
        binding.buttonRestore.setOnClickListener { showRestoreDialog() }
        binding.buttonRefreshMonth.setOnClickListener { showResetMonthConfirmation() }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun loadUserProfile() {
        // Get current user from UserRepository
        val currentUser = userRepository.getCurrentUser()
        
        if (currentUser != null) {
            // Set user name and email in the UI
            binding.textViewUserName.text = currentUser.name
            binding.textViewUserEmail.text = currentUser.email
            
            // Set first letter of name as avatar text
            val firstLetter = currentUser.name.firstOrNull()?.toString() ?: "?"
            // You could set a text drawable here if needed
            
            // Log to verify data is being loaded
            android.util.Log.d("SettingsActivity", "Loaded user: ${currentUser.name}, ${currentUser.email}")
        } else {
            // Handle case where user is not logged in
            android.util.Log.e("SettingsActivity", "Current user is null")
            navigateToLogin()
        }
    }
    
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
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
        
        // Create a list of formatted backup names for display
        val formattedBackups = backupFiles.map { fileName ->
            backupManager.getFormattedBackupDate(fileName)
        }.toTypedArray()
        
        // Show dialog with restore and delete options
        val dialog = AlertDialog.Builder(this)
            .setTitle("Backup Files")
            .setItems(formattedBackups) { _, which ->
                showBackupOptionsDialog(backupFiles[which], formattedBackups[which])
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
    
    private fun showBackupOptionsDialog(fileName: String, displayName: String) {
        val options = arrayOf("Restore", "Delete")
        
        AlertDialog.Builder(this)
            .setTitle(displayName)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> restoreData(fileName)
                    1 -> confirmDeleteBackup(fileName, displayName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun confirmDeleteBackup(fileName: String, displayName: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Backup")
            .setMessage("Are you sure you want to delete this backup?\n\n$displayName")
            .setPositiveButton("Delete") { _, _ ->
                deleteBackup(fileName)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteBackup(fileName: String) {
        val success = backupManager.deleteBackupFile(fileName)
        if (success) {
            Toast.makeText(this, "Backup deleted successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to delete backup", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupNavigation() {
        // Home
        binding.navHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        
        // Analysis
        binding.navAnalysis.setOnClickListener {
            startActivity(Intent(this, AnalysisActivity::class.java))
            finish()
        }
        
        // Settings is already selected (current activity)
    }

    private fun setupProfileButtons() {
        // Set up logout button
        binding.buttonLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
        
        // Set up edit profile button
        binding.buttonEditProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }
    }
    
    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                logoutUser()
            }
            .setNegativeButton("No", null)
            .show()
    }
    
    private fun logoutUser() {
        userRepository.logoutUser()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        navigateToLogin()
    }
    
    private fun showEditProfileDialog() {
        // This would typically open a dialog or activity to edit profile details
        // For now, we'll just show a toast message
        Toast.makeText(this, "Edit profile functionality will be implemented soon", Toast.LENGTH_SHORT).show()
        
        // In a real implementation, you might do something like:
        // val intent = Intent(this, EditProfileActivity::class.java)
        // startActivity(intent)
    }

    private fun showResetMonthConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Reset Monthly Data")
            .setMessage("This will clear all current month's transactions and reset your monthly statistics. Are you sure?")
            .setPositiveButton("Reset") { _, _ ->
                resetMonthlyData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun resetMonthlyData() {
        // Get all transactions
        val allTransactions = transactionStorage.getTransactions()
        
        // Get current month and year
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        // Filter out transactions from the current month
        val filteredTransactions = allTransactions.filter {
            val transactionCalendar = Calendar.getInstance().apply { timeInMillis = it.date }
            transactionCalendar.get(Calendar.MONTH) != currentMonth || 
            transactionCalendar.get(Calendar.YEAR) != currentYear
        }
        
        // Save the filtered transactions (without current month's data)
        transactionStorage.saveTransactions(filteredTransactions)
        
        // Reset monthly statistics in preferences
        preferenceManager.resetMonthlyStats()
        
        // Show success message
        Toast.makeText(
            this,
            "Monthly data has been reset successfully!",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onResume() {
        super.onResume()
        // Refresh user profile details in case they were updated
        loadUserProfile()
    }
}