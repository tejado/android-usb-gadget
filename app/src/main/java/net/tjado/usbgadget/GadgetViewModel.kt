package net.tjado.usbgadget

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import java.util.ArrayList

class GadgetViewModel(application: Application) : AndroidViewModel(
    application
) {
    val gadgets: MutableLiveData<List<GadgetObject>>
    private val rootGranted: MutableLiveData<Boolean>

    private val shellApi: GadgetShellApi
    private val assets: GadgetAssetProfiles

    val gadgetProfileList: Array<String>

    init {
        gadgets = MutableLiveData()
        gadgets.value = ArrayList()

        rootGranted = MutableLiveData()
        rootGranted.value = true

        shellApi = GadgetShellApi()
        assets = GadgetAssetProfiles(getApplication())
        gadgetProfileList = assets.allGadgetProfiles
        updateGadgetData()
    }

    fun hasRootPermissions(): MutableLiveData<Boolean> {
        return rootGranted
    }

    fun reloadGadgetData() {
        val data = gadgets.value!!
        gadgets.value = data
    }

    fun updateGadgetData() {
        shellApi.updateGadgetData(gadgets, rootGranted)
    }

    fun loadGadgetProfile(gadget: String) {
        val profile = assets.getProfileGadget(gadget)
        if (profile != null) {
            shellApi.exec(profile) {
                // update gadget data if new gadget was loaded
                updateGadgetData()
            }
        }
    }

    fun loadFunctionProfile(gadget: GadgetObject, function: String) {
        val profile = assets.getProfileFunction(function, gadget.getValue("gadget_path"))
        if (profile != null) {
            shellApi.exec(profile) {
                // update gadget data if new gadget was loaded
                updateGadgetData()
            }
        }
    }
}