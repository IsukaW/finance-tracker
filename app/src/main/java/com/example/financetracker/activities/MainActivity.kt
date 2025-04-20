package com.example.financetracker.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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

    private var transactions = mutableListOf<Transaction>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        transactionStorage = TransactionStorage(this)
        preferenceManager = PreferenceManager(this)
        notificationHelper = NotificationHelper(this)

        setupRecyclerView()
        setupFAB()
        setupBottomNavigation()
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

    private fun setupBottomNavigation() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home -> {
                    // Already in MainActivity, no need to do anything
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
                else -> false
            }
        }
        
        // Set home as selected by default
        binding.bottomNavigationView.selectedItemId = R.id.menu_home
    }

    private fun loadTransactions() {
        transactions.clear()
        transactions.addAll(transactionStorage.getTransactions())
        transactionAdapter = TransactionAdapter(transactions) { transaction ->
            val intent = Intent(this, AddEditTransactionActivity::class.java)
            intent.putExtra("transaction_id", transaction.id)
            startActivity(intent)
        }
        binding.recyclerTransactions.adapter = transactionAdapter
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

        binding.textIncome.text = String.format("Income: %s%.2f",
            preferenceManager.getCurrencyType(), income)
        binding.textExpenses.text = String.format("Expenses: %s%.2f",
            preferenceManager.getCurrencyType(), expenses)
        binding.textBalance.text = String.format("Balance: %s%.2f",
            preferenceManager.getCurrencyType(), balance)
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
                notificationHelper.showBudgetWarning(monthlyExpenses, budget)
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

    override fun onResume() {
        super.onResume()
        loadTransactions()
        updateSummary()
        checkBudget()
    }
}