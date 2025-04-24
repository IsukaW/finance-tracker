package com.example.financetracker.utils

import android.content.Context
import android.util.Log
import com.example.financetracker.models.Transaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

class TransactionStorage(private val context: Context) {
    private val gson = Gson()
    private val sharedPreferences = context.getSharedPreferences(
        "transaction_data", Context.MODE_PRIVATE
    )

    fun saveTransactions(transactions: List<Transaction>) {
        val json = gson.toJson(transactions)
        sharedPreferences.edit().putString("all_transactions", json).apply()
        Log.d("TransactionStorage", "Saved ${transactions.size} transactions")
    }

    fun getTransactions(): List<Transaction> {
        val json = sharedPreferences.getString("all_transactions", null) ?: return emptyList()
        val type = object : TypeToken<List<Transaction>>() {}.type
        val transactions: List<Transaction> = gson.fromJson(json, type) ?: emptyList()
        Log.d("TransactionStorage", "Retrieved ${transactions.size} transactions")
        return transactions.filter { it.id.isNotEmpty() } // Filter out any invalid transactions
    }

    fun addTransaction(transaction: Transaction) {
        if (transaction.id.isEmpty()) {
            Log.e("TransactionStorage", "Attempted to add transaction with empty ID")
            return
        }
        val transactions = getTransactions().toMutableList()
        // Check if transaction with same ID already exists
        if (transactions.none { it.id == transaction.id }) {
            transactions.add(transaction)
            saveTransactions(transactions)
            Log.d("TransactionStorage", "Added transaction with ID: ${transaction.id}")
        } else {
            Log.e("TransactionStorage", "Transaction with ID ${transaction.id} already exists")
        }
    }

    fun updateTransaction(transaction: Transaction) {
        if (transaction.id.isEmpty()) {
            Log.e("TransactionStorage", "Attempted to update transaction with empty ID")
            return
        }
        val transactions = getTransactions().toMutableList()
        val index = transactions.indexOfFirst { it.id == transaction.id }
        if (index != -1) {
            transactions[index] = transaction
            saveTransactions(transactions)
            Log.d("TransactionStorage", "Updated transaction with ID: ${transaction.id}")
        } else {
            Log.e("TransactionStorage", "Transaction with ID ${transaction.id} not found for update")
        }
    }

    fun deleteTransaction(id: String) {
        if (id.isEmpty()) {
            Log.e("TransactionStorage", "Attempted to delete transaction with empty ID")
            return
        }
        val transactions = getTransactions().toMutableList()
        val initialSize = transactions.size
        transactions.removeIf { it.id == id }
        
        if (transactions.size < initialSize) {
            saveTransactions(transactions)
            Log.d("TransactionStorage", "Deleted transaction with ID: $id")
        } else {
            Log.e("TransactionStorage", "Transaction with ID $id not found for deletion")
        }
    }

    /**
     * Archives transactions from the previous month by tagging them.
     * This maintains the data but marks it as belonging to a previous period.
     */
    fun archivePreviousMonthTransactions() {
        val transactions = getTransactions()
        
        // Get current month and year
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        // Count how many transactions will be archived
        val previousMonthTransactions = transactions.filter {
            val transactionCalendar = Calendar.getInstance()
            transactionCalendar.timeInMillis = it.date
            
            val transactionMonth = transactionCalendar.get(Calendar.MONTH)
            val transactionYear = transactionCalendar.get(Calendar.YEAR)
            
            // Check if transaction is from a previous month
            (transactionMonth != currentMonth || transactionYear != currentYear)
        }
        
        Log.d("TransactionStorage", "Found ${previousMonthTransactions.size} transactions from previous months")
        
        // In a real implementation, you might:
        // 1. Move them to a separate "archive" storage
        // 2. Tag them in the database as "archived"
        // 3. Create monthly summary records
        
        // For this implementation, we're just logging the count
        // If needed, we could modify the transactions and save them back
    }
}