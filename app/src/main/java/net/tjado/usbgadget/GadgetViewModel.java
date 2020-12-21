package net.tjado.usbgadget;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

public class GadgetViewModel extends AndroidViewModel {

    private Application application;

    private MutableLiveData<List<GadgetObject>> gadgetData;
    private MutableLiveData<Boolean> rootGranted;

    GadgetShellApi gsa;
    GadgetAssetProfiles gap;

    String [] gadgetProfileList;

    public GadgetViewModel(@NonNull Application app) {
        super(app);
        application = app;

        gadgetData = new MutableLiveData<>();
        gadgetData.setValue(new ArrayList<>());

        rootGranted = new MutableLiveData<>();
        rootGranted.setValue(true);

        this.gsa = new GadgetShellApi();
        this.gap = new GadgetAssetProfiles(getApplication());
        gadgetProfileList = gap.getAllGadgetProfiles();
        updateGadgetData();
    }

    public MutableLiveData<Boolean> hasRootPermissions() {
        return rootGranted;
    }

    public MutableLiveData<List<GadgetObject>> getGadgets() {
        return gadgetData;
    }

    public void reloadGadgetData() {
        List<GadgetObject> data = gadgetData.getValue();
        gadgetData.setValue(data);
    }

    public void updateGadgetData() {
        this.gsa.updateGadgetData(gadgetData, rootGranted);
    }

    public void loadGadgetProfile(String gadget) {
        String profile = this.gap.getProfileGadget(gadget);

        this.gsa.exec(profile, response -> {
            // update gadget data if new gadget was loaded
            updateGadgetData();
        });
    }

    public void loadFunctionProfile(GadgetObject gadget, String function) {
        String profile = this.gap.getProfileFunction(function, gadget.getValue("gadget_path"));

        this.gsa.exec(profile, response -> {
            // update gadget data if new gadget was loaded
            updateGadgetData();
        });
    }

}