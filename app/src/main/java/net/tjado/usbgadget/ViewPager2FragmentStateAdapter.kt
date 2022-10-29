package net.tjado.usbgadget

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPager2FragmentStateAdapter internal constructor(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            1 -> DeviceInfoFragment()
            2 -> GadgetManagerFragment()
            3 -> LogFragment()
            else -> OverviewFragment()
        }
    }

    override fun getItemCount(): Int {
        return 4
    }
}