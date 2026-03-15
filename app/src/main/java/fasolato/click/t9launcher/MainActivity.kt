package fasolato.click.t9launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
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
    private lateinit var launchTracker: LaunchTracker

    private lateinit var appAdapter: AppAdapter
    private lateinit var skeletonAdapter: SkeletonAdapter
    private lateinit var rvApps: RecyclerView
    private lateinit var tvNoResults: TextView

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            loadApps()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        findViewById<FrameLayout>(R.id.flBackground).setOnClickListener { finishAndRemoveTask() }

        val llCard = findViewById<LinearLayout>(R.id.llCard)
        llCard.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                llCard.viewTreeObserver.removeOnGlobalLayoutListener(this)
                positionCard(llCard)
            }
        })

        launchTracker = LaunchTracker(this)
        rvApps = findViewById(R.id.rvApps)
        tvNoResults = findViewById(R.id.tvNoResults)

        appAdapter = AppAdapter(emptyList(), { app -> launchApp(app) }, { app, view -> showAppMenu(app, view) })
        skeletonAdapter = SkeletonAdapter(3)

        rvApps.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvApps.adapter = skeletonAdapter

        setupKeyboard()
        loadApps()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        registerReceiver(packageReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        finishAndRemoveTask()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(packageReceiver)
    }

    private fun positionCard(card: LinearLayout) {
        val dm = resources.displayMetrics
        val screenWidth = dm.widthPixels
        val screenHeight = dm.heightPixels
        val cardWidth = card.width
        val cardHeight = card.height
        val marginEdge = (20 * dm.density).toInt()
        val marginBottom = (140 * dm.density).toInt()

        val sourceBounds = intent.sourceBounds
        val left: Int
        val top: Int

        if (sourceBounds != null) {
            val iconCenterX = sourceBounds.centerX()
            val iconCenterY = sourceBounds.centerY()
            left = if (iconCenterX < screenWidth / 2) marginEdge else screenWidth - cardWidth - marginEdge
            top = if (iconCenterY < screenHeight / 2) marginEdge else screenHeight - cardHeight - marginBottom
        } else {
            left = (screenWidth - cardWidth) / 2
            top = (screenHeight - cardHeight) / 2
        }

        val params = card.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.TOP or Gravity.START
        params.leftMargin = left
        params.topMargin = top
        card.layoutParams = params
    }

    private fun setupKeyboard() {
        val digitButtons = mapOf(
            R.id.btn2 to '2',
            R.id.btn3 to '3',
            R.id.btn4 to '4',
            R.id.btn5 to '5',
            R.id.btn6 to '6',
            R.id.btn7 to '7',
            R.id.btn8 to '8',
            R.id.btn9 to '9'
        )

        for ((id, digit) in digitButtons) {
            findViewById<Button>(id).setOnClickListener {
                currentDigits.append(digit)
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
                rvApps.adapter = appAdapter
                updateSearch()
            }
        }.start()
    }

    private fun updateSearch() {
        val digits = currentDigits.toString()
        val filtered = if (digits.isEmpty()) allApps
        else allApps.filter { matchesT9(it.name, digits) }

        if (filtered.isEmpty() && digits.isNotEmpty()) {
            tvNoResults.visibility = View.VISIBLE
            rvApps.adapter = skeletonAdapter
        } else {
            tvNoResults.visibility = View.GONE
            rvApps.adapter = appAdapter
            val sorted = filtered.sortedWith(
                compareByDescending<AppInfo> { launchTracker.getLaunchCount(it.packageName) }
                    .thenBy { it.name.lowercase() }
            )
            appAdapter.updateApps(sorted, digits)
        }
    }

    private fun matchesT9(name: String, digits: String): Boolean {
        val words = name.lowercase().split(Regex("[\\s\\-_.]+"))
        return words.any { wordMatchesT9(it, digits) }
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

    private fun showAppMenu(app: AppInfo, anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.menu_app_popup, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_app_info -> {
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${app.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                    true
                }
                R.id.action_uninstall -> {
                    startActivity(Intent(Intent.ACTION_DELETE).apply {
                        data = Uri.parse("package:${app.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun launchApp(app: AppInfo) {
        val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
        if (launchIntent != null) {
            launchTracker.recordLaunch(app.packageName)
            startActivity(launchIntent)
        } else {
            Toast.makeText(this, getString(R.string.error_launch_app, app.name), Toast.LENGTH_SHORT).show()
        }
    }
}
