package net.tjado.usbgadget;

import android.app.Application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class GadgetViewModel extends AndroidViewModel {

    private Application application;

    private MutableLiveData<List<GadgetObject>> gadgetData;
    private MutableLiveData<String> log;

    String [] gadgetProfileList;

    public GadgetViewModel(@NonNull Application app) {
        super(app);

        application = app;
        gadgetData = new MutableLiveData<>();
        gadgetData.setValue(new ArrayList<>());

        log = new MutableLiveData<>();

        gadgetProfileList = getAllGadgetProfiles();

        updateGadgetData();
    }

    public Boolean canGetData() {
        return ExecuteAsRootUtil.canRunRootCommands();
    }

    public LiveData<List<GadgetObject>> getGadgets() {
        return gadgetData;
    }

    public LiveData<String> getLog() {
        return log;
    }

    protected String[] getAllGadgetProfiles() {
        String [] gadgetProfileList;

        try {
            gadgetProfileList = application.getAssets().list("usbGadgetProfiles/");
        } catch (IOException e) {
            gadgetProfileList = null;
        }

        return gadgetProfileList;
    }

    public Boolean loadGadgetProfileFromAsset(String assetFile) {
        String profile = getGadgetProfileFromAsset(assetFile);

        if (profile == null || profile.equals("")) {
            return false;
        }

        RootTask mTask = new RootTask(profile, response -> {
            // update gadget data if new gadget was loaded
            updateGadgetData();
        });
        mTask.execute();

        return true;
    }

    protected String getGadgetProfileFromAsset(String assetFile) {
        BufferedReader reader = null;
        try {
            InputStream is =  application.getAssets().open(String.format("usbGadgetProfiles/%s", assetFile));
            Scanner scanner = new Scanner(is);

            StringBuffer sb = new StringBuffer();
            while (scanner.hasNext()) {
                sb.append(scanner.nextLine() + "\n");
            }
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void updateGadgetData() {
        List<GadgetObject> data = new ArrayList<>();
        String cmd = "for dir in /config/usb_gadget/*/; do echo GADGET_PATH=$dir; cd $dir; if [ \"$?\" -ne \"0\" ]; then echo \"Error - not able to change dir to $dir... exit\"; exit 1; fi; echo UDC=$(cat UDC); find ./configs/ -type l -exec sh -c 'echo FUNCTIONS_ACTIVE=$(basename $(readlink \"$@\"))' _ {} \\;; for f in ./functions/*/; do echo FUNCTIONS=$(basename $f); done; cd ./strings/0x409/; for vars in *; do echo ${vars}=$(cat $vars); done; echo \"=============\"; done; \n";

        RootTask mTask = new RootTask(cmd, response -> {
            //mTextView.setText((String) Response);
            if (response == null) {
                return;
            }
            BufferedReader bufReader = new BufferedReader(new StringReader(response));
            data.clear();
            GadgetObject gadget = new GadgetObject();

            try {
                String line;
                while ((line = bufReader.readLine()) != null) {
                    if(line.equals("=============")) {
                        data.add(gadget);
                        gadget = new GadgetObject();
                        continue;
                    }

                    String[] parts = line.split("=",2);

                    if( parts[0].equals("FUNCTIONS") ) {
                        gadget.addFunction(parts[1]);
                    } else if( parts[0].equals("FUNCTIONS_ACTIVE") ) {
                        gadget.addActiveFunction(parts[1]);
                    } else {
                        gadget.setValue(parts[0].toLowerCase(), parts[1]);
                    }
                }
            }
            catch (Exception e) {
                Log.d("root", e.getMessage());
                e.printStackTrace();
            }

            // refresh gadget data
            gadgetData.setValue(data);

            // refresh log
            log.setValue( Log.getLog() );
        });
        mTask.execute();
    }
}