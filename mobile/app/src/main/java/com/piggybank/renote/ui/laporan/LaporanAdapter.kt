package com.piggybank.renote.ui.laporan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.piggybank.renote.databinding.ItemLaporanBinding

class LaporanAdapter(private var categories: List<Laporan>) :
    RecyclerView.Adapter<LaporanAdapter.LaporanViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LaporanViewHolder {
        val binding = ItemLaporanBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LaporanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LaporanViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size

    fun updateData(newCategories: List<Laporan>) {
        categories = newCategories
        notifyDataSetChanged()
    }

    inner class LaporanViewHolder(private val binding: ItemLaporanBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(laporan: Laporan) {
            binding.categoryName.text = laporan.categoryName
            binding.categoryPercentage.text = laporan.categoryPercentage
        }
    }
}
