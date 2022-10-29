package net.tjado.usbgadget

import java.util.Comparator

class DeviceInfoMapComparator : Comparator<String> {
    override fun compare(s1: String, s2: String): Int {
        return if (s1.equals("KERNEL_VERSION", ignoreCase = true)) {
            -1
        } else if (s2.equals("KERNEL_VERSION", ignoreCase = true)) {
            1
        } else {
            s1.compareTo(s2)
        }
    }
}