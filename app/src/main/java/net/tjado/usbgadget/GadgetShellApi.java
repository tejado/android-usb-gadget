package net.tjado.usbgadget;

import androidx.lifecycle.MutableLiveData;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

public class GadgetShellApi {

    private static String tag = "GadgetShellApi";

    public static Boolean isValidGadgetPath(String gadgetPath) {
        if (gadgetPath != null && gadgetPath.startsWith("/config/")) {
            Log.d(tag, String.format("Gadget Path is valid", gadgetPath));
            return true;
        }

        Log.w(tag, String.format("Gadget Path is not valid (%s)", gadgetPath));
        return false;
    }

    public Boolean exec(String command) {
        return this.exec(new String[]{command}, null);
    }

    public Boolean exec(String[] commands) {
        return this.exec(commands, null);
    }

    public Boolean exec(String command, RootTask.OnRootTaskListener onRootTaskFinished) {
        return this.exec(new String[]{command}, onRootTaskFinished);
    }

    public Boolean exec(String[] commands, RootTask.OnRootTaskListener onRootTaskFinished) {
        RootTask mTask = new RootTask(commands, onRootTaskFinished);
        mTask.execute();

        return true;
    }

    public void updateDeviceInfo(MutableLiveData<TreeMap<String, String>> returnDeviceData) {

        String command = "echo KERNEL_VERSION=`(uname -r |cut -d '-' -f1 )` && (gunzip -c /proc/config.gz | grep -i configfs | sed 's/# //; s/ is not set/=NOT_SET/')\n";
        this.exec(command, response -> {
            Boolean status = (Boolean) response.first;
            String result = (String) response.second;

            BufferedReader bufReader = new BufferedReader(new StringReader(result));
            TreeMap<String, String> deviceData = new TreeMap<>(new DeviceInfoMapComparator());

            try {
                String line;
                while ((line = bufReader.readLine()) != null) {
                    String[] parts = line.split("=",2);
                    deviceData.put(parts[0].toLowerCase(), parts[1]);
                }
            }
            catch (Exception e) {
                Log.d("root", e.getMessage());
                e.printStackTrace();
            }

            returnDeviceData.setValue(deviceData);
        });
    }


    public void updateGadgetData(MutableLiveData<List<GadgetObject>> returnGadgetList, MutableLiveData<Boolean> returnRootState) {
        List<GadgetObject> data = new ArrayList<>();
        String cmd = "for dir in /config/usb_gadget/*/; do echo GADGET_PATH=$dir; cd $dir/configs/; echo CONFIG_PATH=\"$dir/configs/`ls -1 | head -1`/\"; cd $dir; if [ \"$?\" -ne \"0\" ]; then echo \"Error - not able to change dir to $dir... exit\"; exit 1; fi; echo UDC=$(cat UDC); find ./configs/ -type l -exec sh -c 'echo FUNCTIONS_ACTIVE=$(basename $(readlink \"$@\"))' _ {} \\;; for f in ./functions/*/; do echo FUNCTIONS=$(basename $f); done; cd ./strings/0x409/; for vars in *; do echo ${vars}=$(cat $vars); done; echo \"=============\"; done; \n";

        this.exec(cmd, response -> {

            Boolean status = (Boolean) response.first;
            String result = (String) response.second;

            returnRootState.setValue(status);

            if (result == null) {
                return;
            }
            BufferedReader bufReader = new BufferedReader(new StringReader(result));
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
            returnGadgetList.setValue(data);

        });
    }

    public Boolean activate(String gadgetPath, RootTask.OnRootTaskListener onRootTaskFinished) {
        if (!GadgetShellApi.isValidGadgetPath(gadgetPath)) {
            return false;
        }

        // deactivate all
        String cmdDeactivateUsbAll = "find /config/usb_gadget/  -name UDC -type f -exec sh -c 'echo \"\" >  \"$@\"' _ {} \\;\n";
        // activate specific one
        String cmdActivateUsb = String.format("getprop sys.usb.controller > %s/UDC\n", gadgetPath);

        String[] commands = new String[]{cmdDeactivateUsbAll, cmdActivateUsb};
        return this.exec(commands, onRootTaskFinished);
    }

    public Boolean deactivate(String gadgetPath, RootTask.OnRootTaskListener onRootTaskFinished) {
        if (!GadgetShellApi.isValidGadgetPath(gadgetPath)) {
            return false;
        }

        String command = String.format("echo \"\" > %s/UDC\n", gadgetPath);
        return this.exec(command, onRootTaskFinished);
    }

    public Boolean activateFunction(String gadgetPath, String gadgetConfigPath, String function, Boolean activateGadget) {
        if (!GadgetShellApi.isValidGadgetPath(gadgetPath) || !GadgetShellApi.isValidGadgetPath(gadgetConfigPath)) {
            return false;
        }

        RootTask.OnRootTaskListener onRootTaskFinished = null;
        if (activateGadget) {
            onRootTaskFinished = response -> {
                this.activate(gadgetPath, null);
            };
        }

        String command = String.format("ln -s %s/functions/%3$s %s/%3$s\n", gadgetPath, gadgetConfigPath, function);
        return this.exec(command, onRootTaskFinished);
    }

    public Boolean deactivateFunction(String gadgetPath, String gadgetConfigPath, String function, Boolean activateGadget) {
        if (!GadgetShellApi.isValidGadgetPath(gadgetPath) || !GadgetShellApi.isValidGadgetPath(gadgetConfigPath)) {
            return false;
        }

        RootTask.OnRootTaskListener onRootTaskFinished = null;
        if (activateGadget) {
            onRootTaskFinished = response -> {
                this.activate(gadgetPath, null);
            };
        }

        String command = String.format("rm %1$s/%2$s\n", gadgetConfigPath, function);
        return this.exec(command, onRootTaskFinished);
    }
}
