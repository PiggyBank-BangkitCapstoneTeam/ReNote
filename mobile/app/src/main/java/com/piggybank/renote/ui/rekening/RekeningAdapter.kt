package com.piggybank.renote.ui.rekening

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.piggybank.renote.R

class RekeningAdapter(
    private val rekeningList: List<Rekening>,
    private val onArrowClick: (Rekening) -> Unit,
    private val formatCurrency: (Long) -> String
) : RecyclerView.Adapter<RekeningAdapter.RekeningViewHolder>() {

    class RekeningViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.rekening_name)
        val balanceTextView: TextView = itemView.findViewById(R.id.account_amount)
        val arrowIcon: View = itemView.findViewById(R.id.arrow_icon)
        val statusTextView: TextView = itemView.findViewById(R.id.rekening_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RekeningViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rekening, parent, false)
        return RekeningViewHolder(view)
    }

    override fun onBindViewHolder(holder: RekeningViewHolder, position: Int) {
        val rekening = rekeningList[position]

        holder.nameTextView.text = rekening.name
        holder.balanceTextView.text = formatCurrency(rekening.uang)

        val isActive = rekening.uang > 0
        holder.statusTextView.visibility = if (isActive) View.VISIBLE else View.GONE
        holder.statusTextView.text = holder.itemView.context.getString(R.string.status_active)

        holder.arrowIcon.setOnClickListener {
            onArrowClick(rekening)
        }
    }

    override fun getItemCount() = rekeningList.size
}
