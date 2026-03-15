package fasolato.click.t9launcher

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
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

        // Finestra a tutto schermo con sfondo trasparente
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        // Click sull'area trasparente sopra il card → chiudi
        findViewById<FrameLayout>(R.id.flBackground).setOnClickListener { finish() }

        tvSearchDisplay = findViewById(R.id.tvSearchDisplay)

        val rvApps = findViewById<RecyclerView>(R.id.rvApps)
        adapter = AppAdapter(emptyList()) { app -> launchApp(app) }
        rvApps.layoutManager = GridLayoutManager(this, 4)
        rvApps.adapter = adapter

        setupKeyboard()
        loadApps()
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
