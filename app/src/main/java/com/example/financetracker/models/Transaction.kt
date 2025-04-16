package com.example.financetracker.models

import java.util.UUID

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    var title: String,
    var amount: Double,
    var category: String,
    var date: Long,
    var isExpense: Boolean
)