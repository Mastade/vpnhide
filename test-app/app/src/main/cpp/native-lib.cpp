#include <jni.h>
#include <android/log.h>

#include <cerrno>
#include <cstring>
#include <string>
#include <vector>

#include <unistd.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <net/if.h>
#include <ifaddrs.h>
#include <linux/netlink.h>
#include <linux/rtnetlink.h>
#include <linux/if_link.h>

#define TAG "VPNHideTest"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

static const char* VPN_PREFIXES[] = {"tun", "wg", "ppp", "tap"};
static const int NUM_PREFIXES = sizeof(VPN_PREFIXES) / sizeof(VPN_PREFIXES[0]);

static bool is_vpn_iface(const char* name) {
    for (int i = 0; i < NUM_PREFIXES; i++) {
        if (strncmp(name, VPN_PREFIXES[i], strlen(VPN_PREFIXES[i])) == 0) {
            return true;
        }
    }
    return false;
}

static jstring to_jstring(JNIEnv* env, const std::string& s) {
    return env->NewStringUTF(s.c_str());
}

// 1. ioctl SIOCGIFFLAGS on tun0
extern "C" JNIEXPORT jstring JNICALL
Java_dev_okhsunrog_vpnhide_test_NativeChecks_checkIoctlSiocgifflags(JNIEnv* env, jobject) {
    int fd = socket(AF_INET, SOCK_DGRAM, 0);
    if (fd < 0) {
        return to_jstring(env, "FAIL: cannot create socket: " + std::string(strerror(errno)));
    }

    struct ifreq ifr{};
    strncpy(ifr.ifr_name, "tun0", IFNAMSIZ - 1);

    int ret = ioctl(fd, SIOCGIFFLAGS, &ifr);
    int err = errno;
    close(fd);

    if (ret < 0 && err == ENODEV) {
        LOGI("SIOCGIFFLAGS tun0: ENODEV (hooked or no VPN)");
        return to_jstring(env, "PASS");
    } else if (ret == 0) {
        LOGI("SIOCGIFFLAGS tun0: succeeded (flags=0x%x) - VPN visible!", ifr.ifr_flags);
        return to_jstring(env, "FAIL: tun0 exists (flags=0x" +
            std::to_string(ifr.ifr_flags) + ")");
    } else {
        return to_jstring(env, "FAIL: ioctl error: " + std::string(strerror(err)));
    }
}

// 2. ioctl SIOCGIFCONF - enumerate interfaces
extern "C" JNIEXPORT jstring JNICALL
Java_dev_okhsunrog_vpnhide_test_NativeChecks_checkIoctlSiocgifconf(JNIEnv* env, jobject) {
    int fd = socket(AF_INET, SOCK_DGRAM, 0);
    if (fd < 0) {
        return to_jstring(env, "FAIL: cannot create socket: " + std::string(strerror(errno)));
    }

    char buf[4096];
    struct ifconf ifc{};
    ifc.ifc_len = sizeof(buf);
    ifc.ifc_buf = buf;

    if (ioctl(fd, SIOCGIFCONF, &ifc) < 0) {
        int err = errno;
        close(fd);
        return to_jstring(env, "FAIL: ioctl error: " + std::string(strerror(err)));
    }
    close(fd);

    std::vector<std::string> vpn_found;
    struct ifreq* it = ifc.ifc_req;
    int count = ifc.ifc_len / sizeof(struct ifreq);

    for (int i = 0; i < count; i++) {
        const char* name = it[i].ifr_name;
        LOGI("SIOCGIFCONF: found interface '%s'", name);
        if (is_vpn_iface(name)) {
            vpn_found.push_back(name);
        }
    }

    if (vpn_found.empty()) {
        return to_jstring(env, "PASS");
    }
    std::string detail = "FAIL: VPN interfaces found:";
    for (auto& n : vpn_found) detail += " " + n;
    return to_jstring(env, detail);
}

// 3. getifaddrs
extern "C" JNIEXPORT jstring JNICALL
Java_dev_okhsunrog_vpnhide_test_NativeChecks_checkGetifaddrs(JNIEnv* env, jobject) {
    struct ifaddrs* addrs = nullptr;
    if (getifaddrs(&addrs) != 0) {
        return to_jstring(env, "FAIL: getifaddrs error: " + std::string(strerror(errno)));
    }

    std::vector<std::string> vpn_found;
    for (struct ifaddrs* ifa = addrs; ifa; ifa = ifa->ifa_next) {
        if (ifa->ifa_name) {
            LOGI("getifaddrs: found interface '%s'", ifa->ifa_name);
            if (is_vpn_iface(ifa->ifa_name)) {
                // Deduplicate
                bool dup = false;
                for (auto& n : vpn_found) if (n == ifa->ifa_name) { dup = true; break; }
                if (!dup) vpn_found.push_back(ifa->ifa_name);
            }
        }
    }
    freeifaddrs(addrs);

    if (vpn_found.empty()) {
        return to_jstring(env, "PASS");
    }
    std::string detail = "FAIL: VPN interfaces found:";
    for (auto& n : vpn_found) detail += " " + n;
    return to_jstring(env, detail);
}

// Helper: read a proc file and check for VPN interface lines
static std::string check_proc_file(const char* path) {
    int fd = open(path, O_RDONLY);
    if (fd < 0) {
        return "FAIL: cannot open " + std::string(path) + ": " + strerror(errno);
    }

    char buf[8192];
    std::string content;
    ssize_t n;
    while ((n = read(fd, buf, sizeof(buf) - 1)) > 0) {
        buf[n] = '\0';
        content += buf;
    }
    close(fd);

    std::vector<std::string> vpn_lines;
    size_t pos = 0;
    while (pos < content.size()) {
        size_t eol = content.find('\n', pos);
        if (eol == std::string::npos) eol = content.size();
        std::string line = content.substr(pos, eol - pos);
        pos = eol + 1;

        // Check if line contains a VPN interface name
        for (int i = 0; i < NUM_PREFIXES; i++) {
            if (line.find(VPN_PREFIXES[i]) != std::string::npos) {
                vpn_lines.push_back(line.substr(0, 40));
                break;
            }
        }
    }

    if (vpn_lines.empty()) {
        return "PASS";
    }
    std::string detail = "FAIL: VPN lines found in " + std::string(path) + ":";
    for (auto& l : vpn_lines) detail += "\n  " + l;
    return detail;
}

// 4. /proc/net/route
extern "C" JNIEXPORT jstring JNICALL
Java_dev_okhsunrog_vpnhide_test_NativeChecks_checkProcNetRoute(JNIEnv* env, jobject) {
    return to_jstring(env, check_proc_file("/proc/net/route"));
}

// 5. /proc/net/if_inet6
extern "C" JNIEXPORT jstring JNICALL
Java_dev_okhsunrog_vpnhide_test_NativeChecks_checkProcNetIfInet6(JNIEnv* env, jobject) {
    return to_jstring(env, check_proc_file("/proc/net/if_inet6"));
}

// 6. Netlink RTM_GETLINK
extern "C" JNIEXPORT jstring JNICALL
Java_dev_okhsunrog_vpnhide_test_NativeChecks_checkNetlinkGetlink(JNIEnv* env, jobject) {
    int fd = socket(AF_NETLINK, SOCK_RAW, NETLINK_ROUTE);
    if (fd < 0) {
        return to_jstring(env, "FAIL: cannot create netlink socket: " + std::string(strerror(errno)));
    }

    struct sockaddr_nl sa{};
    sa.nl_family = AF_NETLINK;
    if (bind(fd, (struct sockaddr*)&sa, sizeof(sa)) < 0) {
        int err = errno;
        close(fd);
        return to_jstring(env, "FAIL: bind error: " + std::string(strerror(err)));
    }

    // Build RTM_GETLINK request
    struct {
        struct nlmsghdr nlh;
        struct ifinfomsg ifm;
    } req{};

    req.nlh.nlmsg_len = NLMSG_LENGTH(sizeof(struct ifinfomsg));
    req.nlh.nlmsg_type = RTM_GETLINK;
    req.nlh.nlmsg_flags = NLM_F_REQUEST | NLM_F_DUMP;
    req.nlh.nlmsg_seq = 1;
    req.ifm.ifi_family = AF_UNSPEC;

    if (send(fd, &req, req.nlh.nlmsg_len, 0) < 0) {
        int err = errno;
        close(fd);
        return to_jstring(env, "FAIL: send error: " + std::string(strerror(err)));
    }

    // Receive and parse
    char buf[32768];
    std::vector<std::string> vpn_found;
    bool done = false;

    while (!done) {
        ssize_t len = recv(fd, buf, sizeof(buf), 0);
        if (len <= 0) break;

        for (struct nlmsghdr* nh = (struct nlmsghdr*)buf;
             NLMSG_OK(nh, (size_t)len);
             nh = NLMSG_NEXT(nh, len)) {

            if (nh->nlmsg_type == NLMSG_DONE) {
                done = true;
                break;
            }
            if (nh->nlmsg_type == NLMSG_ERROR) {
                done = true;
                break;
            }
            if (nh->nlmsg_type != RTM_NEWLINK) continue;

            struct ifinfomsg* ifi = (struct ifinfomsg*)NLMSG_DATA(nh);
            struct rtattr* rta = IFLA_RTA(ifi);
            int rta_len = IFLA_PAYLOAD(nh);

            while (RTA_OK(rta, rta_len)) {
                if (rta->rta_type == IFLA_IFNAME) {
                    const char* name = (const char*)RTA_DATA(rta);
                    LOGI("netlink RTM_GETLINK: found interface '%s'", name);
                    if (is_vpn_iface(name)) {
                        vpn_found.push_back(name);
                    }
                }
                rta = RTA_NEXT(rta, rta_len);
            }
        }
    }

    close(fd);

    if (vpn_found.empty()) {
        return to_jstring(env, "PASS");
    }
    std::string detail = "FAIL: VPN interfaces found:";
    for (auto& n : vpn_found) detail += " " + n;
    return to_jstring(env, detail);
}
