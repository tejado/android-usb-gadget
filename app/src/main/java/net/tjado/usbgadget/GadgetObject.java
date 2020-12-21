package net.tjado.usbgadget;

import java.util.ArrayList;
import java.util.HashMap;

public class GadgetObject {
    protected HashMap<String, String> values = new HashMap<>();
    protected ArrayList<String> functions = new ArrayList<>();
    protected ArrayList<String> activeFunctions = new ArrayList<>();
    protected GadgetShellApi gsa = new GadgetShellApi();

    public GadgetObject() {
    }

    public void setValue(String key, String value) {
        values.put(key, value);
    }

    public String getValue(String key) {
        try {
            return values.get(key);
        }
        catch (Exception e) {
            return "not set";
        }
    }

    public void addFunction(String f) {
        functions.add(f);
    }

    public ArrayList<String> getFunctions() {
        return functions;
    }

    public void addActiveFunction(String f) {
        activeFunctions.add(f);
    }

    public ArrayList<String> getActiveFunctions() {
        return activeFunctions;
    }

    public Boolean isActivated() {
        String udc = getValue("udc");
        return !udc.equals("") && !udc.equals("not set");
    }

    public String getFormattedFunctions() {
        StringBuffer sb =  new StringBuffer();
        String color = "#ff0000";

        for(String function : functions) {
            if( activeFunctions.contains(function)) {
                color = "#008000";
            }
            sb.append(String.format("<font color=%s>%s</font><br />", color, function));
        }

        return sb.toString();
    }

    public Boolean activate() {
        return activate(null);
    }

    public Boolean activate(RootTask.OnRootTaskListener onRootTaskFinished) {
        return this.gsa.activate(getValue("gadget_path"), onRootTaskFinished);
    }

    public Boolean deactivate() {
        return deactivate(null);
    }

    public Boolean deactivate(RootTask.OnRootTaskListener onRootTaskFinished) {
        return this.gsa.deactivate(getValue("gadget_path"), onRootTaskFinished);
    }


    public Boolean activateFunction(String function, Boolean reactivateGadget) {
        String gadgetPath = getValue("gadget_path");
        String gadgetConfigPath = getValue("config_path");

        Boolean status = this.gsa.activateFunction(gadgetPath, gadgetConfigPath, function, (isActivated() && reactivateGadget));
        if (status) {
            // add activated function to activeFunctions to avoid refreshing configFS data
            activeFunctions.add(function);
        }

        return status;
    }

    public Boolean deactivateFunction(String function, Boolean reactivateGadget) {
        String gadgetPath = getValue("gadget_path");
        String gadgetConfigPath = getValue("config_path");

        Boolean status = this.gsa.deactivateFunction(gadgetPath, gadgetConfigPath, function, (isActivated() && reactivateGadget));
        if (status) {
            // remove deactivated function from activeFunctions to avoid refreshing configFS data
            activeFunctions.removeIf(function::equals);
        }

        return status;
    }
}