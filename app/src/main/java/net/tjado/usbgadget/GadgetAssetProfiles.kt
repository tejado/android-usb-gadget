package net.tjado.usbgadget

import android.app.Application
import android.text.TextUtils


class GadgetAssetProfiles(private val app: Application) {

    val allGadgetProfiles: Array<String>
        get() {
            return app.assets.list("usbGadgetProfiles/") as Array<String>
        }

    val allFunctionProfiles: Array<String>
        get() {
            return app.assets.list("usbFunctionProfiles/") as Array<String>
        }

    fun getProfileFromAsset(profileFolder: String, assetFile: String): String {
        val inputStream = app.assets.open("$profileFolder/$assetFile")
        return inputStream.bufferedReader().use { it.readText() }
    }

    fun getProfileGadget(assetFile: String): String? {
        return getProfileGadget(assetFile, assetFile)
    }

    fun getProfileGadget(assetFile: String, gadgetName: String): String? {
        if (TextUtils.isEmpty(gadgetName)) {
            return null
        }

        val tokens: MutableMap<String, String> = HashMap()
        tokens["gadgetName"] = gadgetName
        val profile = getProfileFromAsset("usbGadgetProfiles", assetFile)
        return parseAsset(profile, tokens)
    }

    fun getProfileFunction(assetFile: String, gadgetPath: String): String? {
        if (!GadgetShellApi.isValidGadgetPath(gadgetPath)) {
            return null
        }

        val tokens: MutableMap<String, String> = HashMap()
        tokens["gadgetPath"] = gadgetPath
        val profile = getProfileFromAsset("usbFunctionProfiles", assetFile)
        return parseAsset(profile, tokens)
    }

    fun parseAsset(profile: String, tokens: Map<String, String>): String {
        if (TextUtils.isEmpty(profile)) {
            return ""
        }

        var parsedProfile = profile
        if (tokens.isNotEmpty()) {
            for ((key, value) in tokens) {
                parsedProfile = parsedProfile.replace("____" + key + "____", value)
            }
        }

        return parsedProfile
    }
}