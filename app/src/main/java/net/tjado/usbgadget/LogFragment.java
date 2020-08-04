package net.tjado.usbgadget;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ViewFlipper;

import java.util.ArrayList;
import java.util.List;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class LogFragment extends Fragment {

    private GadgetAdapter gadgetAdapter;
    private RecyclerView gadgetRecyclerView;
    private GadgetViewModel gadgetViewModel;
    private List<GadgetObject> gadgetData;
    private ViewFlipper rootFlipper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_log, container, false);
        TextView logText = (TextView) v.findViewById(R.id.log);

        gadgetViewModel = new ViewModelProvider(getActivity()).get(GadgetViewModel.class);

        gadgetViewModel.getLog().observe(getViewLifecycleOwner(), item -> {
            logText.setText(item);
        });

        logText.setText(Log.getLog());
        return v;
    }
}