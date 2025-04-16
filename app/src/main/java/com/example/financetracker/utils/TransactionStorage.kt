package com.example.financetracker.utils

import android.content.Context
import com.example.financetracker.models.Transaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TransactionStorage(private val context: Context) {
    private val gson = Gson()
    private val sharedPreferences = context.getSharedPreferences(
        "transaction_data", Context.MODE_PRIVATE
    )

    fun saveTransactions(transactions: List<Transaction>) {
        val json = gson.toJson(transactions)
        sharedPreferences.edit().putString("all_transactions", json).apply()
    }

    fun getTransactions(): List<Transaction> {
        val json = sharedPreferences.getString("all_transactions", null) ?: return emptyList()
        val type = object : TypeToken<List<Transaction>>() {}.type
        return gson.fromJson(json, type)
    }

    fun addTransaction(transaction: Transaction) {
        val transactions = getTransactions().toMutableList()
        transactions.add(transaction)
        saveTransactions(transactions)
    }

    fun updateTransaction(transaction: Transaction) {
        val transactions = getTransactions().toMutableList()
        val index = transactions.indexOfFirst { it.id == transaction.id }
        if (index != -1) {
            transactions[index] = transaction
            saveTransactions(transactions)
        }
    }

    fun deleteTransaction(id: String) {
        val transactions = getTransactions().toMutableList()
        transactions.removeIf { it.id == id }
        saveTransactions(transactions)
    }
}