package fasolato.click.t9launcher

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class OptionsActivity : AppCompatActivity() {

    private lateinit var options: OptionsRepository
    private lateinit var switchInstalled: SwitchCompat
    private lateinit var etMinutes: EditText
    private lateinit var switchLaunched: SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_options)

        supportActionBar?.apply {
            title = getString(R.string.options_title)
            setDisplayHomeAsUpEnabled(true)
        }

        options = OptionsRepository(this)

        switchInstalled = findViewById(R.id.switchShowRecentlyInstalled)
        etMinutes = findViewById(R.id.etRecentlyInstalledMinutes)
        val llMinutes = findViewById<LinearLayout>(R.id.llRecentlyInstalledMinutes)
        switchLaunched = findViewById(R.id.switchShowRecentlyLaunched)

        switchInstalled.isChecked = options.showRecentlyInstalled
        etMinutes.setText(options.recentlyInstalledMinutes.toString())
        llMinutes.visibility = if (options.showRecentlyInstalled) View.VISIBLE else View.GONE
        switchLaunched.isChecked = options.showRecentlyLaunched

        switchInstalled.setOnCheckedChangeListener { _, isChecked ->
            options.showRecentlyInstalled = isChecked
            llMinutes.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        switchLaunched.setOnCheckedChangeListener { _, isChecked ->
            options.showRecentlyLaunched = isChecked
        }
    }

    override fun onPause() {
        super.onPause()
        val minutes = etMinutes.text.toString().toIntOrNull()
        if (minutes != null && minutes > 0) {
            options.recentlyInstalledMinutes = minutes
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
