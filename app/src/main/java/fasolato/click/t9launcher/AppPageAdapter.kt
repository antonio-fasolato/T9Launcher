package fasolato.click.t9launcher

import android.content.pm.PackageManager
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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.ceil

private const val ITEMS_PER_PAGE = 3

class AppPageAdapter(
    private val onAppClick: (AppInfo) -> Unit,
    private val onAppLongClick: (AppInfo, View) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<AppPageAdapter.PageViewHolder>() {

    private var apps: List<AppInfo> = emptyList()
    private var currentDigits: String = ""
    private val iconCache = HashMap<String, Drawable>()
    var packageManager: PackageManager? = null

    private val t9Map = mapOf(
        '2' to "abc", '3' to "def", '4' to "ghi", '5' to "jkl",
        '6' to "mno", '7' to "pqrs", '8' to "tuv", '9' to "wxyz"
    )

    fun updateApps(newApps: List<AppInfo>, digits: String = "") {
        apps = newApps
        currentDigits = digits
        notifyDataSetChanged()
    }

    fun getPageCount(): Int = if (apps.isEmpty()) 0 else ceil(apps.size / ITEMS_PER_PAGE.toDouble()).toInt()

    override fun getItemCount() = getPageCount()

    class PageViewHolder(val container: LinearLayout) : RecyclerView.ViewHolder(container)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val container = LinearLayout(parent.context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        return PageViewHolder(container)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.container.removeAllViews()
        val inflater = LayoutInflater.from(holder.container.context)
        for (i in 0 until ITEMS_PER_PAGE) {
            val appIndex = position * ITEMS_PER_PAGE + i
            val itemView = inflater.inflate(R.layout.item_app, holder.container, false)
            itemView.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            if (appIndex < apps.size) {
                val app = apps[appIndex]
                val iconView = itemView.findViewById<ImageView>(R.id.ivAppIcon)
                val cached = iconCache[app.packageName]
                if (cached != null) {
                    iconView.setImageDrawable(cached)
                } else {
                    iconView.setImageDrawable(null)
                    val pm = packageManager
                    if (pm != null) {
                        Thread {
                            val icon = try { pm.getApplicationIcon(app.packageName) } catch (e: Exception) { null }
                            if (icon != null) {
                                iconCache[app.packageName] = icon
                                iconView.post { iconView.setImageDrawable(icon) }
                            }
                        }.start()
                    }
                }
                itemView.findViewById<TextView>(R.id.tvAppName).text =
                    buildHighlightedName(app.name, currentDigits)
                itemView.setOnClickListener { onAppClick(app) }
                itemView.setOnLongClickListener { onAppLongClick(app, it); true }
            } else {
                itemView.visibility = View.INVISIBLE
            }
            holder.container.addView(itemView)
        }
    }

    private fun wordMatchesT9(word: String, digits: String): Boolean {
        if (word.length < digits.length) return false
        for (i in digits.indices) {
            val digit = digits[i]
            val letters = t9Map[digit]
            if (letters == null || word[i] !in letters) return false
        }
        return true
    }

    private fun buildHighlightedName(name: String, digits: String): SpannableString {
        val spannable = SpannableString(name)
        if (digits.isEmpty()) return spannable
        val highlightColor = 0xFFA78BFA.toInt()
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
