package net.tjado.usbgadget;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class DeviceInfoFragment extends Fragment {

    private MutableLiveData<TreeMap<String,String>> deviceData;
    private View v;

    public DeviceInfoFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.v = inflater.inflate(R.layout.fragment_device_info, container, false);

        deviceData = new MutableLiveData<>();
        deviceData.setValue(new TreeMap<>(new DeviceInfoMapComparator()));

        deviceData.observe(getViewLifecycleOwner(), item -> {
            this.loadData(item);
        });

        GadgetShellApi gsa = new GadgetShellApi();
        gsa.updateDeviceInfo(deviceData);

        return v;
    }

    private void loadData(Map<String, String> deviceData) {

        LinearLayout list = this.v.findViewById(R.id.list_device_data);
        list.removeAllViews();

        View viHead = getLayoutInflater().inflate(R.layout.row_device_info, null);

        TextView tvHeadName = viHead.findViewById(R.id.name);
        tvHeadName.setText("Kernel Config Parameter");
        tvHeadName.setTypeface(null, Typeface.BOLD);

        TextView tvHeadValue = viHead.findViewById(R.id.value);
        tvHeadValue.setText("Value");
        tvHeadValue.setTypeface(null, Typeface.BOLD);
        list.addView(viHead);

        for (Map.Entry<String, String> entry : deviceData.entrySet()) {
            View vi = getLayoutInflater().inflate(R.layout.row_device_info, null);

            TextView tvName = vi.findViewById(R.id.name);
            tvName.setText(entry.getKey().toUpperCase());

            TextView tvValue = vi.findViewById(R.id.value);
            String value = entry.getValue();

            String color;
            switch (value) {
                case "y":
                    value = "Yes";
                    color = "#008000";
                    break;
                case "n":
                    value = "No";
                    color = "#ff0000";
                    break;
                case "NOT_SET":
                    value = "Not set";
                    color = "#ff0000";
                    break;
                default:
                    color = "#000000";
            }

            tvValue.setText(Html.fromHtml(String.format("<font color=%s>%s</font>", color, value), Html.FROM_HTML_MODE_LEGACY));

            list.addView(vi);
        }
    }
}