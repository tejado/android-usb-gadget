package net.tjado.usbgadget;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class LogFragment extends Fragment {

    public LogFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_log, container, false);
        TextView logText = (TextView) v.findViewById(R.id.log);

        logText.setText(Log.getLog());
        Log.onLogChangeListener = () -> {
            getActivity().runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    logText.setText(Log.getLog());
                }
            });

        };

        return v;
    }
}