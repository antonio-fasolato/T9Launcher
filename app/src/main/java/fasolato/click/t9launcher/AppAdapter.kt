package fasolato.click.t9launcher

import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable? = null,
    val description: String = ""
)

class AppAdapter(
    private var apps: List<AppInfo>,
    private val onAppClick: (AppInfo) -> Unit,
    private val onAppLongClick: (AppInfo, View) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {

    private var currentDigits: String = ""

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
        val app = apps[position]
        holder.icon.setImageDrawable(app.icon)
        holder.name.text = buildHighlightedName(app.name, currentDigits)
        holder.itemView.setOnClickListener { onAppClick(app) }
        holder.itemView.setOnLongClickListener { view ->
            onAppLongClick(app, view)
            true
        }
    }

    override fun getItemCount() = apps.size

    fun updateApps(newApps: List<AppInfo>, digits: String = "") {
        apps = newApps
        currentDigits = digits
        notifyDataSetChanged()
    }

    private fun buildHighlightedName(name: String, digits: String): SpannableString {
        val spannable = SpannableString(name)
        if (digits.isEmpty()) return spannable
        val positions = T9Matcher.matchPositions(name, digits) ?: return spannable
        val highlightColor = 0xFFFFEB3B.toInt()
        for (idx in positions) {
            spannable.setSpan(BackgroundColorSpan(highlightColor), idx, idx + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(StyleSpan(Typeface.BOLD), idx, idx + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return spannable
    }
}
