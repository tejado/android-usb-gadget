package net.tjado.usbgadget;

import java.util.Comparator;
import java.util.TreeMap;

public class DeviceInfoMapComparator implements Comparator<String> {

    @Override
    public int compare(String s1, String s2) {
        if (s1.equalsIgnoreCase("KERNEL_VERSION")) {
            return -1;
        } else if (s2.equalsIgnoreCase("KERNEL_VERSION")) {
            return 1;
        } else {
            return s1.compareTo(s2);
        }
    }
}
