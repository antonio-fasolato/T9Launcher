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
    val icon: Drawable
)

class AppAdapter(
    private var apps: List<AppInfo>,
    private val onAppClick: (AppInfo) -> Unit,
    private val onAppLongClick: (AppInfo, View) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {

    private var currentDigits: String = ""

    private val t9Map = mapOf(
        '2' to "abc",
        '3' to "def",
        '4' to "ghi",
        '5' to "jkl",
        '6' to "mno",
        '7' to "pqrs",
        '8' to "tuv",
        '9' to "wxyz"
    )

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
        holder.name.text = buildHighlightedName(holder.name.context, app.name, currentDigits)
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

    private fun wordMatchesT9(word: String, digits: String): Boolean {
        if (word.length < digits.length) return false
        for (i in digits.indices) {
            val digit = digits[i]
            val letters = t9Map[digit]
            val matches = (letters != null && word[i] in letters) || word[i] == digit
            if (!matches) return false
        }
        return true
    }

    private fun buildHighlightedName(context: android.content.Context, name: String, digits: String): SpannableString {
        val spannable = SpannableString(name)
        if (digits.isEmpty()) return spannable

        val highlightColor = 0xFFFFEB3B.toInt()
        val delimiter = Regex("[\\s\\-_.]+")
        var pos = 0
        while (pos <= name.length) {
            val delimMatch = delimiter.find(name, pos)
            val wordEnd = delimMatch?.range?.first ?: name.length
            val word = name.substring(pos, wordEnd)
            if (wordMatchesT9(word.lowercase(), digits)) {
                spannable.setSpan(BackgroundColorSpan(highlightColor), pos, pos + digits.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(StyleSpan(Typeface.BOLD), pos, pos + digits.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                break
            }
            pos = delimMatch?.range?.last?.plus(1) ?: (name.length + 1)
        }

        return spannable
    }
}
