package fasolato.click.t9launcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SettingsAdapter(
    private var entries: List<SettingsEntry>,
    private val onEntryClick: (SettingsEntry) -> Unit
) : RecyclerView.Adapter<SettingsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.ivAppIcon)
        val name: TextView = view.findViewById(R.id.tvAppName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        holder.icon.setImageResource(R.drawable.ic_settings_entry)
        holder.name.text = entry.name
        holder.itemView.setOnClickListener { onEntryClick(entry) }
    }

    override fun getItemCount() = entries.size

    fun updateEntries(newEntries: List<SettingsEntry>) {
        entries = newEntries
        notifyDataSetChanged()
    }
}
