package dev.okhsunrog.vpnhide.test

object NativeChecks {
    init {
        System.loadLibrary("vpnhide_test")
    }

    external fun checkIoctlSiocgifflags(): String
    external fun checkIoctlSiocgifconf(): String
    external fun checkGetifaddrs(): String
    external fun checkProcNetRoute(): String
    external fun checkProcNetIfInet6(): String
    external fun checkNetlinkGetlink(): String
}
