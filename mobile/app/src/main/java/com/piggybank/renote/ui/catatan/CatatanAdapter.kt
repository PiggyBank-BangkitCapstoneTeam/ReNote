package com.piggybank.renote.ui.catatan

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.piggybank.renote.databinding.ItemCatatanBinding
import java.text.NumberFormat
import java.util.Locale

class CatatanAdapter(
    private val onEditClick: (Catatan) -> Unit
) : RecyclerView.Adapter<CatatanAdapter.CatatanViewHolder>() {

    private var catatanList = listOf<Catatan>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CatatanViewHolder {
        val binding = ItemCatatanBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CatatanViewHolder(binding, onEditClick)
    }

    override fun onBindViewHolder(holder: CatatanViewHolder, position: Int) {
        val catatan = catatanList[position]
        holder.bind(catatan)
    }

    override fun getItemCount(): Int = catatanList.size

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newList: List<Catatan>) {
        catatanList = newList
        notifyDataSetChanged()
    }

    class CatatanViewHolder(
        private val binding: ItemCatatanBinding,
        private val onEditClick: (Catatan) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(catatan: Catatan) {
            binding.categoryTextView.text = catatan.kategori
            binding.descriptionTextView.text = catatan.deskripsi

            val numberFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            val nominal = catatan.nominal.toDoubleOrNull() ?: 0.0
            val formattedNominal = if (nominal >= 0) {
                "+ ${numberFormat.format(nominal).replace("Rp", "Rp ")}"
            } else {
                "- ${numberFormat.format(-nominal).replace("Rp", "Rp ")}"
            }
            binding.amountTextView.text = formattedNominal
            binding.arrowIcon.setOnClickListener {
                onEditClick(catatan)
            }
        }
    }
}
