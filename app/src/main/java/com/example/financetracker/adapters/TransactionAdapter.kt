package com.example.financetracker.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.financetracker.R
import com.example.financetracker.models.Transaction
import com.example.financetracker.utils.PreferenceManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionAdapter(
    private val transactions: List<Transaction>,
    private val onItemClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageCategory: ImageView = view.findViewById(R.id.imageCategoryIcon)
        val textTitle: TextView = view.findViewById(R.id.textTitle)
        val textCategory: TextView = view.findViewById(R.id.textCategory)
        val textDate: TextView = view.findViewById(R.id.textDate)
        val textAmount: TextView = view.findViewById(R.id.textAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        val context = holder.itemView.context
        val preferenceManager = PreferenceManager(context)

        holder.textTitle.text = transaction.title
        holder.textCategory.text = transaction.category

        // Format date
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        holder.textDate.text = dateFormat.format(Date(transaction.date))

        // Format amount with currency and set color
        val sign = if (transaction.isExpense) "-" else "+"
        val currency = preferenceManager.getCurrencyType()
        holder.textAmount.text = String.format("%s%s%.2f", sign, currency, transaction.amount)

        val colorRes = if (transaction.isExpense)
            android.R.color.holo_red_dark else android.R.color.holo_green_dark
        holder.textAmount.setTextColor(ContextCompat.getColor(context, colorRes))

        // Set category icon
        val iconRes = getCategoryIcon(transaction.category)
        holder.imageCategory.setImageResource(iconRes)

        // Set click listener
        holder.itemView.setOnClickListener {
            onItemClick(transaction)
        }
    }

    override fun getItemCount() = transactions.size

    private fun getCategoryIcon(category: String): Int {
        return when (category.lowercase()) {
            "food" -> R.drawable.ic_food
            "transport" -> R.drawable.ic_transport
            "bills" -> R.drawable.ic_bills
            "entertainment" -> R.drawable.ic_entertainment
            "shopping" -> R.drawable.ic_shopping
            "health" -> R.drawable.ic_health
            "education" -> R.drawable.ic_education
            else -> R.drawable.ic_others
        }
    }
}