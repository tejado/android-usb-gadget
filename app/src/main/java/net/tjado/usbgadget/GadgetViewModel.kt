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
    val commandError = MutableLiveData<String?>()

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

    /** Refreshes from ConfigFS, as the kernel may have refused the change, and reports errors. */
    fun onCommandFinished(result: RootResult) {
        if (result.error.isNotBlank()) {
            commandError.value = result.error
        }
        updateGadgetData()
    }

    fun loadGadgetProfile(gadget: String) {
        val profile = assets.getProfileGadget(gadget)
        if (profile != null) {
            shellApi.exec(profile, ::onCommandFinished)
        }
    }

    fun loadFunctionProfile(gadget: GadgetObject, function: String) {
        val gadgetPath = gadget.getValue("gadget_path")
        val profile = assets.getProfileFunction(function, gadgetPath)
        if (profile != null) {
            shellApi.execWithGadgetUnbound(gadgetPath, gadget.isActivated, profile, ::onCommandFinished)
        }
    }
}