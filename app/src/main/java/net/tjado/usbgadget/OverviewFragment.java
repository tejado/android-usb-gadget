package net.tjado.usbgadget;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewFlipper;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class OverviewFragment extends Fragment {

    private GadgetAdapter gadgetAdapter;
    private RecyclerView gadgetRecyclerView;
    private GadgetViewModel gadgetViewModel;
    private List<GadgetObject> gadgetData;
    private ViewFlipper rootFlipper;

    public OverviewFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_overview, container, false);
        gadgetViewModel = new ViewModelProvider(getActivity()).get(GadgetViewModel.class);

        rootFlipper = v.findViewById(R.id.flipper);
        gadgetRecyclerView = (RecyclerView) v.findViewById(R.id.rv_gadgets);
        gadgetData = new ArrayList<>();

        gadgetViewModel.getGadgets().observe(getViewLifecycleOwner(), item -> {
            gadgetData.clear();
            gadgetData.addAll(item);

            if (gadgetAdapter != null) {
                gadgetAdapter.notifyDataSetChanged();
            }
        });

        gadgetViewModel.hasRootPermissions().observe(getViewLifecycleOwner(), item -> {
            rootFlipper.setDisplayedChild(item ? 0 : 1);
        });

        gadgetAdapter = new GadgetAdapter(getActivity(), gadgetData, new GadgetAdapterClickInterface() {
            @Override public void onGadgetClicked(GadgetObject gadget) {
                GadgetFragment openFragment = new GadgetFragment(gadget);
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(((ViewGroup)getView().getParent()).getId(), openFragment, "GadgetFragment")
                        .addToBackStack(null)
                        .commit();
            }
            @Override public void onStatusChange(GadgetObject gadget, Boolean newStatus) {
                if(newStatus) {
                    gadget.activate(response -> {
                        gadgetViewModel.updateGadgetData();
                    });
                } else {
                    gadget.deactivate(response -> {
                        gadgetViewModel.updateGadgetData();
                    });
                }
            }
        });

        gadgetRecyclerView.setAdapter(gadgetAdapter);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(),1);
        gadgetRecyclerView.setLayoutManager(gridLayoutManager);

        return v;
    }
}