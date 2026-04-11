package dev.okhsunrog.vpnhide.test

import android.graphics.Color
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.NetworkInterface

class MainActivity : AppCompatActivity() {

    private lateinit var resultsLayout: LinearLayout
    private lateinit var summaryText: TextView
    private val resultViews = mutableListOf<Pair<String, TextView>>()

    companion object {
        private const val TAG = "VPNHideTest"
        private val VPN_PREFIXES = listOf("tun", "wg", "ppp", "tap")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        resultsLayout = findViewById(R.id.resultsLayout)
        summaryText = findViewById(R.id.summaryText)
        val runButton = findViewById<Button>(R.id.runAllButton)

        setupRows()
        runButton.setOnClickListener { runAllChecks() }
    }

    private fun setupRows() {
        val checks = listOf(
            "1. ioctl SIOCGIFFLAGS tun0",
            "2. ioctl SIOCGIFCONF enum",
            "3. getifaddrs enum",
            "4. /proc/net/route (native)",
            "5. /proc/net/if_inet6 (native)",
            "6. netlink RTM_GETLINK",
            "7. hasTransport(VPN)",
            "8. hasCapability(NOT_VPN)",
            "9. getTransportInfo != VpnTransportInfo",
            "10. NetworkInterface enum",
            "11. ActiveNetwork VPN transport",
            "12. LinkProperties ifname",
            "13. DNS servers (info)",
            "14. http.proxyHost",
            "15. /proc/net/route (Java)",
        )

        for (name in checks) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(6), 0, dp(6))
            }

            val label = TextView(this).apply {
                text = name
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val result = TextView(this).apply {
                text = "—"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            row.addView(label)
            row.addView(result)
            resultsLayout.addView(row)
            resultViews.add(name to result)
        }
    }

    private fun runAllChecks() {
        // Clear
        for ((_, tv) in resultViews) {
            tv.text = "..."
            tv.setTextColor(Color.GRAY)
        }

        val results = mutableListOf<String>()

        // Native checks 1-6
        results.add(runSafe { NativeChecks.checkIoctlSiocgifflags() })
        results.add(runSafe { NativeChecks.checkIoctlSiocgifconf() })
        results.add(runSafe { NativeChecks.checkGetifaddrs() })
        results.add(runSafe { NativeChecks.checkProcNetRoute() })
        results.add(runSafe { NativeChecks.checkProcNetIfInet6() })
        results.add(runSafe { NativeChecks.checkNetlinkGetlink() })

        // Java checks 7-15
        results.add(checkHasTransportVpn())
        results.add(checkHasCapabilityNotVpn())
        results.add(checkTransportInfo())
        results.add(checkNetworkInterfaceEnum())
        results.add(checkActiveNetworkVpn())
        results.add(checkLinkPropertiesIfname())
        results.add(checkDnsServers())
        results.add(checkProxyHost())
        results.add(checkProcNetRouteJava())

        // Update UI
        var passed = 0
        var total = 0
        for (i in results.indices) {
            val (_, tv) = resultViews[i]
            val r = results[i]
            tv.text = r

            val isInfo = r.startsWith("INFO")
            if (isInfo) {
                tv.setTextColor(Color.GRAY)
            } else if (r == "PASS") {
                tv.setTextColor(Color.parseColor("#4CAF50"))
                passed++
                total++
            } else {
                tv.setTextColor(Color.parseColor("#F44336"))
                total++
            }
        }

        summaryText.text = "$passed/$total passed"
        Log.i(TAG, "Results: $passed/$total passed")
    }

    private fun runSafe(block: () -> String): String {
        return try {
            block()
        } catch (e: Exception) {
            Log.e(TAG, "Check failed with exception", e)
            "FAIL: ${e.message}"
        }
    }

    // 7. hasTransport(TRANSPORT_VPN)
    private fun checkHasTransportVpn(): String {
        val cm = getSystemService(ConnectivityManager::class.java)
        val net = cm.activeNetwork ?: return "PASS"
        val caps = cm.getNetworkCapabilities(net) ?: return "PASS"
        val hasVpn = caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        Log.i(TAG, "hasTransport(VPN) = $hasVpn")
        return if (!hasVpn) "PASS" else "FAIL: VPN transport present"
    }

    // 8. hasCapability(NET_CAPABILITY_NOT_VPN)
    private fun checkHasCapabilityNotVpn(): String {
        val cm = getSystemService(ConnectivityManager::class.java)
        val net = cm.activeNetwork ?: return "PASS"
        val caps = cm.getNetworkCapabilities(net) ?: return "PASS"
        val notVpn = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
        Log.i(TAG, "hasCapability(NOT_VPN) = $notVpn")
        return if (notVpn) "PASS" else "FAIL: NOT_VPN capability missing"
    }

    // 9. getTransportInfo != VpnTransportInfo
    private fun checkTransportInfo(): String {
        val cm = getSystemService(ConnectivityManager::class.java)
        val net = cm.activeNetwork ?: return "PASS"
        val caps = cm.getNetworkCapabilities(net) ?: return "PASS"
        val info = caps.transportInfo
        Log.i(TAG, "transportInfo = $info (class: ${info?.javaClass?.name})")
        if (Build.VERSION.SDK_INT >= 31 && info != null) {
            val className = info.javaClass.name
            if (className.contains("VpnTransportInfo")) {
                return "FAIL: VpnTransportInfo present"
            }
        }
        return "PASS"
    }

    // 10. NetworkInterface.getNetworkInterfaces
    private fun checkNetworkInterfaceEnum(): String {
        val vpnIfaces = mutableListOf<String>()
        try {
            val ifaces = NetworkInterface.getNetworkInterfaces() ?: return "PASS"
            for (iface in ifaces) {
                Log.i(TAG, "NetworkInterface: ${iface.name}")
                if (VPN_PREFIXES.any { iface.name.startsWith(it) }) {
                    vpnIfaces.add(iface.name)
                }
            }
        } catch (e: Exception) {
            return "FAIL: ${e.message}"
        }
        return if (vpnIfaces.isEmpty()) "PASS" else "FAIL: ${vpnIfaces.joinToString()}"
    }

    // 11. ActiveNetwork + getNetworkCapabilities - VPN transport
    private fun checkActiveNetworkVpn(): String {
        val cm = getSystemService(ConnectivityManager::class.java)
        val net = cm.activeNetwork ?: return "PASS"
        val caps = cm.getNetworkCapabilities(net) ?: return "PASS"
        val hasVpn = caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        Log.i(TAG, "activeNetwork VPN transport = $hasVpn")
        return if (!hasVpn) "PASS" else "FAIL: active network has VPN transport"
    }

    // 12. LinkProperties.getInterfaceName
    private fun checkLinkPropertiesIfname(): String {
        val cm = getSystemService(ConnectivityManager::class.java)
        val net = cm.activeNetwork ?: return "PASS"
        val lp = cm.getLinkProperties(net) ?: return "PASS"
        val ifname = lp.interfaceName ?: return "PASS"
        Log.i(TAG, "LinkProperties ifname = $ifname")
        return if (VPN_PREFIXES.none { ifname.startsWith(it) }) {
            "PASS"
        } else {
            "FAIL: ifname=$ifname"
        }
    }

    // 13. DNS servers (informational)
    private fun checkDnsServers(): String {
        val cm = getSystemService(ConnectivityManager::class.java)
        val net = cm.activeNetwork ?: return "INFO: no active network"
        val lp = cm.getLinkProperties(net) ?: return "INFO: no link properties"
        val servers = lp.dnsServers.joinToString { it.hostAddress ?: "?" }
        Log.i(TAG, "DNS servers: $servers")
        return "INFO: $servers"
    }

    // 14. http.proxyHost
    private fun checkProxyHost(): String {
        val host = System.getProperty("http.proxyHost")
        Log.i(TAG, "http.proxyHost = $host")
        return if (host.isNullOrEmpty()) "PASS" else "FAIL: proxyHost=$host"
    }

    // 15. /proc/net/route from Java
    private fun checkProcNetRouteJava(): String {
        return try {
            val vpnLines = mutableListOf<String>()
            BufferedReader(InputStreamReader(java.io.FileInputStream("/proc/net/route"))).use { br ->
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    if (VPN_PREFIXES.any { line!!.contains(it) }) {
                        vpnLines.add(line!!.take(40))
                    }
                }
            }
            if (vpnLines.isEmpty()) "PASS" else "FAIL: VPN lines: ${vpnLines.joinToString("; ")}"
        } catch (e: Exception) {
            "FAIL: ${e.message}"
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
