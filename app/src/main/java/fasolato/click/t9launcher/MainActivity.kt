package fasolato.click.t9launcher

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

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

    private val currentDigits = StringBuilder()
    private var allApps: List<AppInfo> = emptyList()
    private lateinit var adapter: AppAdapter
    private lateinit var tvSearchDisplay: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        // Click sull'area trasparente → chiudi
        findViewById<FrameLayout>(R.id.flBackground).setOnClickListener { finishAndRemoveTask() }

        // Posiziona la card vicino all'icona sorgente dopo il layout
        val llCard = findViewById<LinearLayout>(R.id.llCard)
        llCard.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                llCard.viewTreeObserver.removeOnGlobalLayoutListener(this)
                positionCard(llCard)
            }
        })

        tvSearchDisplay = findViewById(R.id.tvSearchDisplay)

        val rvApps = findViewById<RecyclerView>(R.id.rvApps)
        adapter = AppAdapter(emptyList()) { app -> launchApp(app) }
        rvApps.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvApps.adapter = adapter

        setupKeyboard()
        loadApps()
    }

    override fun onStop() {
        super.onStop()
        finishAndRemoveTask()
    }

    private fun positionCard(card: LinearLayout) {
        val dm = resources.displayMetrics
        val screenWidth = dm.widthPixels
        val screenHeight = dm.heightPixels
        val cardWidth = card.width
        val cardHeight = card.height
        val margin = (60 * dm.density).toInt()

        val sourceBounds = intent.sourceBounds
        var left: Int
        var top: Int

        if (sourceBounds != null) {
            left = sourceBounds.centerX() - cardWidth / 2
            top = sourceBounds.bottom + margin
            if (top + cardHeight > screenHeight - margin) {
                top = sourceBounds.top - cardHeight - margin
            }
        } else {
            left = (screenWidth - cardWidth) / 2
            top = (screenHeight - cardHeight) / 2
        }

        left = left.coerceIn(margin, screenWidth - cardWidth - margin)
        top = top.coerceIn(margin, screenHeight - cardHeight - margin)

        val params = card.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.TOP or Gravity.START
        params.leftMargin = left
        params.topMargin = top
        card.layoutParams = params
    }

    private fun setupKeyboard() {
        val digitButtons = mapOf(
            R.id.btn1 to '1',
            R.id.btn2 to '2',
            R.id.btn3 to '3',
            R.id.btn4 to '4',
            R.id.btn5 to '5',
            R.id.btn6 to '6',
            R.id.btn7 to '7',
            R.id.btn8 to '8',
            R.id.btn9 to '9',
            R.id.btn0 to '0'
        )

        for ((id, digit) in digitButtons) {
            findViewById<Button>(id).setOnClickListener {
                if (digit in t9Map) {
                    currentDigits.append(digit)
                    updateSearch()
                }
            }
        }

        findViewById<Button>(R.id.btnBackspace).setOnClickListener {
            if (currentDigits.isNotEmpty()) {
                currentDigits.deleteCharAt(currentDigits.length - 1)
                updateSearch()
            }
        }

        findViewById<Button>(R.id.btnClear).setOnClickListener {
            currentDigits.clear()
            updateSearch()
        }
    }

    private fun loadApps() {
        Thread {
            val pm = packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val loaded = pm.queryIntentActivities(intent, 0)
                .filter { it.activityInfo.packageName != packageName }
                .map { ri ->
                    AppInfo(
                        name = ri.loadLabel(pm).toString(),
                        packageName = ri.activityInfo.packageName,
                        icon = ri.loadIcon(pm)
                    )
                }
                .sortedBy { it.name.lowercase() }

            runOnUiThread {
                allApps = loaded
                updateSearch()
            }
        }.start()
    }

    private fun updateSearch() {
        val digits = currentDigits.toString()
        tvSearchDisplay.text = digits

        val filtered = if (digits.isEmpty()) allApps
        else allApps.filter { matchesT9(it.name, digits) }

        adapter.updateApps(filtered)
    }

    /**
     * Restituisce true se almeno una parola del nome dell'app inizia con
     * la sequenza di lettere corrispondente alle cifre T9.
     */
    private fun matchesT9(appName: String, digits: String): Boolean {
        val words = appName.lowercase().split(Regex("[\\s\\-_.]+"))
        return words.any { wordMatchesT9(it, digits) }
    }

    private fun wordMatchesT9(word: String, digits: String): Boolean {
        if (word.length < digits.length) return false
        for (i in digits.indices) {
            val letters = t9Map[digits[i]] ?: return false
            if (word[i] !in letters) return false
        }
        return true
    }

    private fun launchApp(app: AppInfo) {
        val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            Toast.makeText(this, "Impossibile avviare ${app.name}", Toast.LENGTH_SHORT).show()
        }
    }
}
