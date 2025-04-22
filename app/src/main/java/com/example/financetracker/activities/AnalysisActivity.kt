package com.example.financetracker.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.financetracker.R
import com.example.financetracker.databinding.ActivityAnalysisBinding
import com.example.financetracker.models.Transaction
import com.example.financetracker.utils.PreferenceManager
import com.example.financetracker.utils.TransactionStorage
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import java.util.Calendar

class AnalysisActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAnalysisBinding
    private lateinit var transactionStorage: TransactionStorage
    private lateinit var preferenceManager: PreferenceManager

    private val transactions = mutableListOf<Transaction>()
    private val months = arrayOf("January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Remove or comment out this line if toolbar isn't in your layout
        // setSupportActionBar(binding.toolbar)
        // supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // supportActionBar?.setDisplayShowHomeEnabled(true)

        transactionStorage = TransactionStorage(this)
        preferenceManager = PreferenceManager(this)

        setupMonthSpinner()
        loadTransactions()
        setupNavigation()
    }
    
    private fun setupMonthSpinner() {
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, months)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMonth.adapter = adapter
        
        // Set selection without triggering listener
        binding.spinnerMonth.setSelection(currentMonth, false)

        binding.spinnerMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Clear existing chart first
                binding.chartExpenses.clear()
                // Force refresh with the newly selected month
                updateChartForMonth(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadTransactions() {
        transactions.clear()
        transactions.addAll(transactionStorage.getTransactions())
        updateChartForMonth(binding.spinnerMonth.selectedItemPosition)
    }

    private fun updateChartForMonth(month: Int) {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        
        // Make sure we use the correct month from the selection
        val selectedMonth = month
        val selectedMonthName = months[selectedMonth]
        
        // Filter transactions for the selected month (explicitly use selectedMonth)
        val monthlyTransactions = transactions.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.date }
            cal.get(Calendar.MONTH) == selectedMonth && cal.get(Calendar.YEAR) == currentYear && it.isExpense
        }

        // Group by category
        val expensesByCategory = monthlyTransactions.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .filter { it.value > 0 }

        // Update pie chart
        val entries = expensesByCategory.map {
            PieEntry(it.value.toFloat(), it.key)
        }

        if (entries.isEmpty()) {
            binding.chartExpenses.setNoDataText("No expenses for $selectedMonthName")
            binding.chartExpenses.invalidate()
            binding.textTotalExpense.text = String.format("Total: %s%.2f",
                preferenceManager.getCurrencyType(), 0.0)
            return
        }

        val dataSet = PieDataSet(entries, "Expenses by Category")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 14f

        val pieData = PieData(dataSet)
        pieData.setValueFormatter(object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val currency = preferenceManager.getCurrencyType()
                return "$currency${value.toInt()}"
            }
        })

        binding.chartExpenses.data = pieData
        binding.chartExpenses.description.isEnabled = false
        
        // Explicitly set the center text to the selected month name
        binding.chartExpenses.centerText = "Expenses\n$selectedMonthName"
        
        binding.chartExpenses.setCenterTextSize(16f)
        binding.chartExpenses.legend.textSize = 12f
        binding.chartExpenses.animateY(1000)
        binding.chartExpenses.invalidate()

        // Update total expense for the selected month
        val totalExpense = expensesByCategory.values.sum()
        binding.textTotalExpense.text = String.format("Total: %s%.2f",
            preferenceManager.getCurrencyType(), totalExpense)
    }

    private fun setupNavigation() {
        // Home 
        binding.navHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        
        // Analysis is already selected
        
        // Settings
        binding.navSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        }
    }
}