package dev.okhsunrog.vpnhide

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Minimal status screen.
 *
 * The module is controlled entirely by LSPosed / Vector's per-app scope
 * setting — there is no in-app picker.
 *
 * To use:
 *   1. Install the APK
 *   2. Open Vector / LSPosed manager → Modules → enable VPN Hide
 *   3. Set the module's scope to the apps you want VPN hidden from
 *   4. Force-stop those apps so they re-fork with hooks active
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.title = getString(R.string.app_name)

        findViewById<TextView>(R.id.status_text).text =
            getString(R.string.status_instructions)
    }
}
