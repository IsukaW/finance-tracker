package com.example.financetracker.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.financetracker.R
import com.example.financetracker.adapters.TransactionAdapter
import com.example.financetracker.databinding.ActivityMainBinding
import com.example.financetracker.models.Transaction
import com.example.financetracker.notifications.NotificationHelper
import com.example.financetracker.utils.PreferenceManager
import com.example.financetracker.utils.TransactionStorage
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var transactionStorage: TransactionStorage
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var notificationHelper: NotificationHelper

    private val transactions = mutableListOf<Transaction>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        transactionStorage = TransactionStorage(this)
        preferenceManager = PreferenceManager(this)
        notificationHelper = NotificationHelper(this)

        // Load transactions before setting up RecyclerView
        transactions.addAll(transactionStorage.getTransactions())
        
        setupRecyclerView()
        setupFAB()
        setupNavigation()
        updateSummary()
        checkBudget()
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(transactions) { transaction ->
            // Handle item click - open edit screen
            val intent = Intent(this, AddEditTransactionActivity::class.java)
            intent.putExtra("transaction_id", transaction.id)
            startActivity(intent)
        }

        binding.recyclerTransactions.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = transactionAdapter
        }
    }

    private fun setupFAB() {
        binding.fabAddTransaction.setOnClickListener {
            startActivity(Intent(this, AddEditTransactionActivity::class.java))
        }
    }

    private fun setupNavigation() {
        // Home is already selected (current activity)
        
        // Analysis
        binding.navAnalysis.setOnClickListener {
            startActivity(Intent(this, AnalysisActivity::class.java))
        }
        
        // Settings
        binding.navSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadTransactions()
        updateSummary()
        checkBudget()
        
        // Request notification permissions if needed
        requestNotificationPermissions()
    }

    private fun loadTransactions() {
        val newTransactions = transactionStorage.getTransactions()
        transactions.clear()
        transactions.addAll(newTransactions)
        transactionAdapter.updateTransactions(newTransactions)
        updateSummary()
    }

    private fun updateSummary() {
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)

        val monthlyTransactions = transactions.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.date }
            cal.get(Calendar.MONTH) == currentMonth
        }

        val income = monthlyTransactions
            .filter { !it.isExpense }
            .sumOf { it.amount }

        val expenses = monthlyTransactions
            .filter { it.isExpense }
            .sumOf { it.amount }

        val balance = income - expenses
        
        val currencySymbol = preferenceManager.getCurrencyType()

        // Only update the amount TextViews, not the label TextViews
        binding.textIncomeAmount.text = String.format("%s%.2f", currencySymbol, income)
        binding.textExpensesAmount.text = String.format("%s%.2f", currencySymbol, expenses)
        
        binding.textBalance.text = String.format("Balance: %s%.2f", currencySymbol, balance)
    }

    private fun checkBudget() {
        val budget = preferenceManager.getMonthlyBudget()
        if (budget <= 0) return  // No budget set

        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val monthlyExpenses = transactions
            .filter {
                val cal = Calendar.getInstance().apply { timeInMillis = it.date }
                it.isExpense && cal.get(Calendar.MONTH) == currentMonth
            }
            .sumOf { it.amount }

        val percentage = (monthlyExpenses / budget) * 100

        binding.progressBudget.progress = percentage.toInt().coerceAtMost(100)

        when {
            percentage >= 100 -> {
                binding.textBudgetStatus.text = "Budget Exceeded!"
                binding.textBudgetStatus.setTextColor(
                    ContextCompat.getColor(this, android.R.color.holo_red_dark)
                )
                // Force show notification when budget is exceeded
                notificationHelper.showBudgetWarning(monthlyExpenses, budget, true)
            }
            percentage >= 80 -> {
                binding.textBudgetStatus.text = "Approaching Budget Limit"
                binding.textBudgetStatus.setTextColor(
                    ContextCompat.getColor(this, android.R.color.holo_orange_dark)
                )
                notificationHelper.showBudgetWarning(monthlyExpenses, budget)
            }
            else -> {
                binding.textBudgetStatus.text = "Budget: ${percentage.toInt()}%"
                binding.textBudgetStatus.setTextColor(
                    ContextCompat.getColor(this, android.R.color.holo_green_dark)
                )
            }
        }
    }

    // Add debug method to test notifications directly
    private fun testNotification() {
        notificationHelper.showDebugNotification()
    }

    // Request notification permissions for Android 13+
    private fun requestNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != 
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    NotificationHelper.PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    // Handle permission results
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == NotificationHelper.PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // Permission granted, check budget again to possibly show notification
                checkBudget()
            } else {
                Toast.makeText(
                    this,
                    "Notification permission denied. You won't receive budget alerts.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_home -> {
                startActivity(Intent(this, MainActivity::class.java))
                true
            }

            R.id.menu_analysis -> {
                startActivity(Intent(this, AnalysisActivity::class.java))
                true
            }
            R.id.menu_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}