package net.tjado.usbgadget;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewFlipper;

import java.util.ArrayList;
import java.util.List;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class OverviewFragment extends Fragment {

    private GadgetAdapter gadgetAdapter;
    private RecyclerView gadgetRecyclerView;
    private GadgetViewModel gadgetViewModel;
    private List<GadgetObject> gadgetData;
    private ViewFlipper rootFlipper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_overview, container, false);
        gadgetViewModel = new ViewModelProvider(getActivity()).get(GadgetViewModel.class);

        rootFlipper = v.findViewById(R.id.flipper);
        gadgetRecyclerView = (RecyclerView) v.findViewById(R.id.rv_gadgets);
        gadgetData = new ArrayList<>();

        gadgetViewModel.getGadgets().observe(getViewLifecycleOwner(), item -> {
            gadgetData.clear();
            item.forEach(x -> gadgetData.add(x));

            if (gadgetAdapter != null) {
                gadgetAdapter.notifyDataSetChanged();
            }
        });

        gadgetViewModel.hasRootPermissions().observe(getViewLifecycleOwner(), item -> {
            rootFlipper.setDisplayedChild(item ? 0 : 1);
        });

        gadgetAdapter = new GadgetAdapter(getActivity(), gadgetData);
        gadgetRecyclerView.setAdapter(gadgetAdapter);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(),1);
        gadgetRecyclerView.setLayoutManager(gridLayoutManager);

        return v;
    }
}