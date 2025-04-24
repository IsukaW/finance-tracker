package com.example.financetracker.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.financetracker.R
import com.example.financetracker.adapters.TransactionAdapter
import com.example.financetracker.databinding.ActivityMainBinding
import com.example.financetracker.models.Transaction
import com.example.financetracker.notifications.NotificationHelper
import com.example.financetracker.utils.MonthChangeTracker
import com.example.financetracker.utils.PreferenceManager
import com.example.financetracker.utils.TransactionStorage
import com.example.financetracker.utils.UserRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var transactionStorage: TransactionStorage
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var userRepository: UserRepository

    private val allTransactions = mutableListOf<Transaction>()
    private val displayedTransactions = mutableListOf<Transaction>()
    
    // Month selection options
    private val monthOptions = arrayOf(
        "Current Month", 
        "Last Month", 
        "Last 3 Months", 
        "Last 6 Months", 
        "This Year", 
        "All Transactions",
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        transactionStorage = TransactionStorage(this)
        preferenceManager = PreferenceManager(this)
        notificationHelper = NotificationHelper(this)
        userRepository = UserRepository(this)

        // Check if user is logged in
        if (!userRepository.isUserLoggedIn()) {
            navigateToLogin()
            return
        }

        // Load all transactions first
        allTransactions.addAll(transactionStorage.getTransactions())
        
        // Setup UI components
        setupMonthSelector()
        setupRecyclerView()
        setupFAB()
        setupNavigation()
        
        // Filter transactions to current month by default
        filterTransactionsBySelection("Current Month")
        
        // Request notification permissions if needed
        requestNotificationPermissions()
    }

    private fun setupMonthSelector() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, monthOptions)
        (binding.monthSelectorDropdown as? AutoCompleteTextView)?.setAdapter(adapter)
        
        binding.monthSelectorDropdown.setOnItemClickListener { _, _, position, _ ->
            val selectedOption = monthOptions[position]
            filterTransactionsBySelection(selectedOption)
        }
    }
    
    private fun filterTransactionsBySelection(selection: String) {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        val filteredTransactions = when(selection) {
            "Current Month" -> {
                allTransactions.filter { transaction ->
                    val transactionCal = Calendar.getInstance().apply { timeInMillis = transaction.date }
                    transactionCal.get(Calendar.MONTH) == currentMonth && 
                    transactionCal.get(Calendar.YEAR) == currentYear
                }
            }
            "Last Month" -> {
                calendar.add(Calendar.MONTH, -1)
                val lastMonth = calendar.get(Calendar.MONTH)
                val lastMonthYear = calendar.get(Calendar.YEAR)
                
                allTransactions.filter { transaction ->
                    val transactionCal = Calendar.getInstance().apply { timeInMillis = transaction.date }
                    transactionCal.get(Calendar.MONTH) == lastMonth && 
                    transactionCal.get(Calendar.YEAR) == lastMonthYear
                }
            }
            "Last 3 Months" -> {
                calendar.add(Calendar.MONTH, -3)
                val threeMonthsAgo = calendar.timeInMillis
                
                allTransactions.filter { transaction ->
                    transaction.date >= threeMonthsAgo
                }
            }
            "Last 6 Months" -> {
                calendar.add(Calendar.MONTH, -6)
                val sixMonthsAgo = calendar.timeInMillis
                
                allTransactions.filter { transaction ->
                    transaction.date >= sixMonthsAgo
                }
            }
            "This Year" -> {
                allTransactions.filter { transaction ->
                    val transactionCal = Calendar.getInstance().apply { timeInMillis = transaction.date }
                    transactionCal.get(Calendar.YEAR) == currentYear
                }
            }
            "All Transactions" -> {
                allTransactions
            }
            else -> {
                // Handle specific month selections (January through December)
                val selectedMonthIndex = monthOptions.indexOf(selection) - 6  // Adjust for the first 6 options
                
                allTransactions.filter { transaction ->
                    val transactionCal = Calendar.getInstance().apply { timeInMillis = transaction.date }
                    transactionCal.get(Calendar.MONTH) == selectedMonthIndex
                }
            }
        }
        
        // Update displayed transactions
        displayedTransactions.clear()
        displayedTransactions.addAll(filteredTransactions)
        
        // Update UI with filtered data
        updateTransactionsList(filteredTransactions)
        updateSummary(filteredTransactions)
        updateTransactionsHeader(selection, filteredTransactions.size)
    }
    
    private fun updateTransactionsList(transactions: List<Transaction>) {
        if (::transactionAdapter.isInitialized) {
            transactionAdapter.updateTransactions(transactions)
        }
    }
    
    private fun updateTransactionsHeader(filterType: String, count: Int) {
        binding.textTransactionsHeader.text = "$filterType Transactions ($count)"
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(displayedTransactions) { transaction ->
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

    private fun setupMonthDisplay() {
        // Update the month selector to show current month by default
        binding.monthSelectorDropdown.setText("Current Month", false)
    }

    override fun onResume() {
        super.onResume()
        
        // Check for month change
        val monthChangeTracker = MonthChangeTracker(this)
        if (monthChangeTracker.hasMonthChanged()) {
            monthChangeTracker.handleMonthChange()
            refreshForNewMonth()
        }
        
        // Reload transactions and apply current filter
        reloadTransactions()
    }

    private fun refreshForNewMonth() {
        Toast.makeText(this, "New month started! Your monthly stats have been reset.", Toast.LENGTH_LONG).show()
        setupMonthDisplay()
    }

    private fun reloadTransactions() {
        // Reload all transactions from storage
        allTransactions.clear()
        allTransactions.addAll(transactionStorage.getTransactions())
        
        // Re-apply current filter
        val currentSelection = binding.monthSelectorDropdown.text.toString()
        filterTransactionsBySelection(currentSelection)
    }

    private fun updateSummary(transactions: List<Transaction>) {
        // Calculate summary based on filtered transactions
        val income = transactions
            .filter { !it.isExpense }
            .sumOf { it.amount }

        val expenses = transactions
            .filter { it.isExpense }
            .sumOf { it.amount }

        val balance = income - expenses
        
        val currencySymbol = preferenceManager.getCurrencyType()

        // Update UI with calculated values
        binding.textIncomeAmount.text = String.format("%s%.2f", currencySymbol, income)
        binding.textExpensesAmount.text = String.format("%s%.2f", currencySymbol, expenses)
        binding.textBalance.text = String.format("Balance: %s%.2f", currencySymbol, balance)
        
        // Update budget progress
        updateBudgetProgress(expenses)
    }
    
    private fun updateBudgetProgress(currentExpenses: Double) {
        val budget = preferenceManager.getMonthlyBudget()
        if (budget <= 0) {
            binding.progressBudget.progress = 0
            binding.textBudgetStatus.text = "Budget: 0%"
            binding.textBudgetStatus.setTextColor(
                ContextCompat.getColor(this, android.R.color.darker_gray)
            )
            return
        }

        val percentage = (currentExpenses / budget) * 100

        binding.progressBudget.progress = percentage.toInt().coerceAtMost(100)

        when {
            percentage >= 100 -> {
                binding.textBudgetStatus.text = "Budget Exceeded!"
                binding.textBudgetStatus.setTextColor(
                    ContextCompat.getColor(this, android.R.color.holo_red_dark)
                )
                // Show notification for budget exceeded
                if (binding.monthSelectorDropdown.text.toString() == "Current Month") {
                    notificationHelper.showBudgetWarning(currentExpenses, budget, true)
                }
            }
            percentage >= 80 -> {
                binding.textBudgetStatus.text = "Approaching Budget Limit"
                binding.textBudgetStatus.setTextColor(
                    ContextCompat.getColor(this, android.R.color.holo_orange_dark)
                )
                // Show notification for approaching budget
                if (binding.monthSelectorDropdown.text.toString() == "Current Month") {
                    notificationHelper.showBudgetWarning(currentExpenses, budget)
                }
            }
            else -> {
                binding.textBudgetStatus.text = "Budget: ${percentage.toInt()}%"
                binding.textBudgetStatus.setTextColor(
                    ContextCompat.getColor(this, android.R.color.holo_green_dark)
                )
            }
        }
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
                filterTransactionsBySelection(binding.monthSelectorDropdown.text.toString())
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
            R.id.menu_logout -> {
                logoutUser()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun logoutUser() {
        userRepository.logoutUser()
        navigateToLogin()
    }
    
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        // Clear the back stack
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun checkBudget() {
        // This method is now replaced with updateBudgetProgress
    }
}