package com.example.financetracker.activities

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.financetracker.R
import com.example.financetracker.databinding.ActivityAddEditTransactionBinding
import com.example.financetracker.models.Transaction
import com.example.financetracker.utils.TransactionStorage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class AddEditTransactionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddEditTransactionBinding
    private lateinit var transactionStorage: TransactionStorage

    private var transactionId: String? = null
    private var isEdit = false
    private var selectedDate = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        transactionStorage = TransactionStorage(this)

        setupCategorySpinner()
        setupDatePicker()

        // Check if editing existing transaction
        transactionId = intent.getStringExtra("transaction_id")
        if (transactionId != null) {
            isEdit = true
            loadTransactionData()
        }

        binding.buttonSave.setOnClickListener { saveTransaction() }
    }

    private fun setupCategorySpinner() {
        val categories = resources.getStringArray(R.array.categories)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter
    }

    private fun setupDatePicker() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.editTextDate.setText(dateFormat.format(selectedDate.time))

        binding.editTextDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    selectedDate.set(Calendar.YEAR, year)
                    selectedDate.set(Calendar.MONTH, month)
                    selectedDate.set(Calendar.DAY_OF_MONTH, day)

                    binding.editTextDate.setText(dateFormat.format(selectedDate.time))
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun loadTransactionData() {
        val transaction = transactionStorage.getTransactions()
            .find { it.id == transactionId }

        if (transaction != null) {
            binding.editTextTitle.setText(transaction.title)
            binding.editTextAmount.setText(transaction.amount.toString())

            // Set date
            selectedDate.timeInMillis = transaction.date
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            binding.editTextDate.setText(dateFormat.format(selectedDate.time))

            // Set category
            val categories = resources.getStringArray(R.array.categories)
            val position = categories.indexOf(transaction.category)
            if (position != -1) {
                binding.spinnerCategory.setSelection(position)
            }

            // Set transaction type
            binding.radioGroupType.check(
                if (transaction.isExpense) R.id.radioExpense else R.id.radioIncome
            )

            // Show delete button
            binding.buttonDelete.visibility = View.VISIBLE
            binding.buttonDelete.setOnClickListener { deleteTransaction() }
        }
    }

    private fun saveTransaction() {
        // Validate input
        val title = binding.editTextTitle.text.toString().trim()
        if (title.isEmpty()) {
            binding.editTextTitle.error = "Title is required"
            return
        }

        val amountStr = binding.editTextAmount.text.toString().trim()
        if (amountStr.isEmpty()) {
            binding.editTextAmount.error = "Amount is required"
            return
        }

        val amount: Double
        try {
            amount = amountStr.toDouble()
            if (amount <= 0) {
                binding.editTextAmount.error = "Amount must be greater than zero"
                return
            }
        } catch (e: NumberFormatException) {
            binding.editTextAmount.error = "Invalid amount"
            return
        }

        val category = binding.spinnerCategory.selectedItem.toString()
        val isExpense = binding.radioGroupType.checkedRadioButtonId == R.id.radioExpense

        val transaction = Transaction(
            id = if (isEdit) transactionId!! else UUID.randomUUID().toString(),
            title = title,
            amount = amount,
            category = category,
            date = selectedDate.timeInMillis,
            isExpense = isExpense
        )

        if (isEdit) {
            transactionStorage.updateTransaction(transaction)
        } else {
            transactionStorage.addTransaction(transaction)
        }

        finish()
    }

    private fun deleteTransaction() {
        if (transactionId.isNullOrEmpty()) {
            // Should never happen, but just in case
            finish()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                try {
                    transactionStorage.deleteTransaction(transactionId!!)
                    finish()
                } catch (e: Exception) {
                    AlertDialog.Builder(this)
                        .setTitle("Error")
                        .setMessage("Failed to delete transaction. Please try again.")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}