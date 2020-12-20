package net.tjado.usbgadget;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class DeviceInfoFragment extends Fragment {

    public DeviceInfoFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_device_info, container, false);
        TextView logText = (TextView) v.findViewById(R.id.log);

        logText.setText(Log.getLog());
        Log.onLogChangeListener = () -> {
            logText.setText(Log.getLog());
        };

        GadgetShellApi gsa = new GadgetShellApi();
        gsa.updateDeviceInfo(response -> {
            this.loadData();
        });

        return v;
    }

    private void loadData() {
    /*
        for(String functionName : functions) {
            View vi = getLayoutInflater().inflate(R.layout.row_function, null);

            TextView tv = vi.findViewById(R.id.name);
            tv.setText(functionName);

            Switch f_status = vi.findViewById(R.id.status);
            f_status.setChecked( gadget.getActiveFunctions().contains(functionName) );
            f_status.setOnClickListener((buttonView) -> {
                if (f_status.isChecked()){
                    gadget.activateFunction(functionName, false);
                } else {
                    gadget.deactivateFunction(functionName, true);
                }
                gadgetViewModel.reloadGadgetData();
            });

            list.addView(vi);
        }*/
    }
}