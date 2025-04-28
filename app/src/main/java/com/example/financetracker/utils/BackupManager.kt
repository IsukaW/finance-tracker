package com.example.financetracker.utils

import android.content.Context
import android.util.Log
import com.example.financetracker.models.Transaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupManager(private val context: Context) {
    private val gson = Gson()

    fun exportData(transactions: List<Transaction>): Boolean {
        return try {
            val json = gson.toJson(transactions)
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(Date())
            val fileName = "finance_backup_$timestamp.json"
            val file = File(context.filesDir, fileName)

            FileOutputStream(file).use {
                it.write(json.toByteArray())
            }
            true
        } catch (e: Exception) {
            Log.e("BackupManager", "Export failed", e)
            false
        }
    }

    fun importData(fileName: String): List<Transaction>? {
        return try {
            val file = File(context.filesDir, fileName)
            if (!file.exists()) return null

            val json = FileInputStream(file).bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<Transaction>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            Log.e("BackupManager", "Import failed", e)
            null
        }
    }

    fun getBackupFiles(): List<String> {
        return try {
            context.filesDir.listFiles { file ->
                file.isFile && file.name.startsWith("finance_backup_") && file.name.endsWith(".json")
            }?.map { it.name } ?: emptyList()
        } catch (e: Exception) {
            Log.e("BackupManager", "Failed to list backup files", e)
            emptyList()
        }
    }
    
    fun deleteBackupFile(fileName: String): Boolean {
        return try {
            val file = File(context.filesDir, fileName)
            if (file.exists() && file.isFile && 
                fileName.startsWith("finance_backup_") && 
                fileName.endsWith(".json")) {
                val result = file.delete()
                Log.d("BackupManager", "Deleted backup file: $fileName, result: $result")
                result
            } else {
                Log.e("BackupManager", "Invalid backup file: $fileName")
                false
            }
        } catch (e: Exception) {
            Log.e("BackupManager", "Failed to delete backup file", e)
            false
        }
    }
    
    fun getFormattedBackupDate(fileName: String): String {
        try {
            // Extract date portion from finance_backup_20230521_153045.json format
            val datePart = fileName.substringAfter("finance_backup_").substringBefore(".json")
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())
            
            val date = dateFormat.parse(datePart)
            return if (date != null) {
                outputFormat.format(date)
            } else {
                fileName
            }
        } catch (e: Exception) {
            Log.e("BackupManager", "Failed to parse backup date", e)
            return fileName
        }
    }
}