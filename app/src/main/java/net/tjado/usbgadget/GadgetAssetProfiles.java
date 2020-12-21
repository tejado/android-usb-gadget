package net.tjado.usbgadget;

import android.app.Application;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class GadgetAssetProfiles {

    private Application app;

    public GadgetAssetProfiles(Application app) {
        this.app = app;
    }

    public String[] getAllGadgetProfiles() {
        String [] gadgetProfileList;

        try {
            gadgetProfileList = this.app.getAssets().list("usbGadgetProfiles/");
        } catch (IOException e) {
            gadgetProfileList = null;
        }

        return gadgetProfileList;
    }

    public String[] getAllFunctionProfiles() {
        String [] gadgetProfileList;

        try {
            gadgetProfileList = this.app.getAssets().list("usbFunctionProfiles/");
        } catch (IOException e) {
            gadgetProfileList = null;
        }

        return gadgetProfileList;
    }

    public String getProfileFromAsset(String profileFolder, String assetFile) {
        try {
            InputStream is = this.app.getAssets().open(String.format("%s/%s", profileFolder, assetFile));
            Scanner scanner = new Scanner(is);

            StringBuffer sb = new StringBuffer();
            while (scanner.hasNext()) {
                sb.append(scanner.nextLine()).append("\n");
            }

            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getProfileGadget(String assetFile) {
        return getProfileFromAsset("usbGadgetProfiles", assetFile);
    }

    public String getProfileFunction(String assetFile, String gadgetPath) {
        if (!GadgetShellApi.isValidGadgetPath(gadgetPath)) {
            return null;
        }

        Map<String, String> tokens = new HashMap<>();
        tokens.put("gadgetPath", gadgetPath);
        String profile = getProfileFromAsset("usbFunctionProfiles", assetFile);

        return parseAsset(profile, tokens);
    }

    public String parseAsset(String profile, Map<String, String> tokens) {
        if (profile == null || profile.equals("")) {
            return "";
        }

        if (tokens != null && tokens.size() > 0) {
            for (Map.Entry<String, String> token : tokens.entrySet()) {
                profile = profile.replace("____" + token.getKey() + "____", token.getValue());
            }
        }

        return profile;
    }
}
