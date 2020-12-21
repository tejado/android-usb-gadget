package net.tjado.usbgadget;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;

public class GadgetFragment extends Fragment {

    private GadgetViewModel gadgetViewModel;
    private ViewFlipper rootFlipper;
    private GadgetObject gadget;
    private String[] functionProfileList;
    private View view;

    public GadgetFragment(GadgetObject g) {
        this.gadget = g;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_gadget, container, false);
        gadgetViewModel = new ViewModelProvider(getActivity()).get(GadgetViewModel.class);

        rootFlipper = view.findViewById(R.id.flipper);

        gadgetViewModel.getGadgets().observe(getViewLifecycleOwner(), item -> {
            for (GadgetObject g : item) {
                if (g.getValue("gadget_path").equals(this.gadget.getValue("gadget_path"))) {
                    this.gadget = g;
                    refreshData();
                }
            }
        });

        gadgetViewModel.hasRootPermissions().observe(getViewLifecycleOwner(), item -> {
            rootFlipper.setDisplayedChild(item ? 0 : 1);
        });

        GadgetAssetProfiles gap = new GadgetAssetProfiles(getActivity().getApplication());
        functionProfileList = gap.getAllFunctionProfiles();

        Button addFunction = view.findViewById(R.id.btn_function_add);
        addFunction.setOnClickListener(v -> {
            final AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getContext());
            alertBuilder.setTitle("Add Function to Gadget");
            alertBuilder.setCancelable(true);

            alertBuilder.setItems(functionProfileList, (dialog, which) -> {
                gadgetViewModel.loadFunctionProfile(gadget, functionProfileList[which]);
            });

            alertBuilder.show();
        });

        
        refreshData();

        return view;
    }

    private void refreshData() {
        View v = view;
        TextView manufacturer = v.findViewById(R.id.tv_gadget_manufacturer);
        TextView product = v.findViewById(R.id.tv_gadget_product);
        TextView serialnumber = v.findViewById(R.id.tv_gadget_sn);
        TextView udc = v.findViewById(R.id.tv_gadget_udc);
        TextView path = v.findViewById(R.id.tv_gadget_path);
        Switch status = v.findViewById(R.id.tv_gadget_status);

        manufacturer.setText(gadget.getValue("manufacturer"));
        product.setText(gadget.getValue("product"));
        serialnumber.setText(gadget.getValue("serialnumber"));
        path.setText(gadget.getValue("gadget_path"));
        udc.setText(gadget.getValue("udc"));

        status.setChecked(gadget.isActivated());
        status.setOnClickListener((buttonView) -> {
            if(status.isChecked()) {
                gadget.activate(response -> {
                    gadgetViewModel.updateGadgetData();
                });
            } else {
                gadget.deactivate(response -> {
                    gadgetViewModel.updateGadgetData();
                });
            }
        });

        LinearLayout list = v.findViewById(R.id.list_functons);
        list.removeAllViews();

        ArrayList<String> functions = gadget.getFunctions();
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
        }
    }
}