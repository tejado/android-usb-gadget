package net.tjado.usbgadget;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ViewPager2FragmentStateAdapter extends FragmentStateAdapter {

    ViewPager2FragmentStateAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new OverviewFragment();
            case 1:
                return new DeviceInfoFragment();
            case 2:
                return new LogFragment();
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
