import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.piggybank.renote.R
import com.piggybank.renote.databinding.ItemMonthBinding

class MonthAdapter(
    private val months: List<String>,
    private val onMonthSelected: (String) -> Unit
) : RecyclerView.Adapter<MonthAdapter.MonthViewHolder>() {

    private var selectedPosition: Int = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthViewHolder {
        val binding = ItemMonthBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MonthViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MonthViewHolder, position: Int) {
        holder.bind(months[position], position)
    }

    override fun getItemCount(): Int = months.size

    inner class MonthViewHolder(private val binding: ItemMonthBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("NotifyDataSetChanged")
        fun bind(month: String, position: Int) {
            binding.monthName.text = month
            binding.root.isSelected = (position == selectedPosition)

            binding.root.setBackgroundResource(
                if (position == selectedPosition) R.drawable.month_selector
                else R.drawable.item_background_calander
            )

            binding.root.setOnClickListener {
                if (selectedPosition != position) {
                    selectedPosition = position
                    notifyDataSetChanged()
                    onMonthSelected(month)
                }
            }
        }
    }
}
