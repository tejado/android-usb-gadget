package net.tjado.usbgadget;

import java.util.HashMap;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

public class GadgetObject {
    protected HashMap<String, String> values = new HashMap<>();
    protected SortedSet<String> functions = new TreeSet<>();
    protected SortedSet<String> activeFunctions = new TreeSet<>();

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

    public SortedSet<String> getFunctions(String key) {
        return functions;
    }

    public void addActiveFunction(String f) {
        activeFunctions.add(f);
    }

    public SortedSet<String> getActiveFunctions(String key) {
        return activeFunctions;
    }

    public Boolean isActivated() {
        String udc = getValue("udc");
        if (udc.equals("") || udc.equals("not set")) {
            return false;
        } else {
            return true;
        }
    }

    public String getFormattedFunctions() {
        StringBuffer sb =  new StringBuffer();
        String color = "";

        Iterator<String> it = functions.iterator();
        while( it.hasNext() ) {
            String item = it.next();

            if( activeFunctions.contains(item)) {
                color = "#008000";
            } else {
                color = "#ff0000";
            }

            sb.append(String.format("<font color=%s>%s</font><br />", color, item));
        }

        return sb.toString();
    }
}