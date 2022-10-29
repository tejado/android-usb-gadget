package net.tjado.usbgadget

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootConfiguration : BroadcastReceiver() {
    private val TAG = "DEVICE_BOOT"

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            Log.i(TAG, "Intent ACTION_LOCKED_BOOT_COMPLETED")
            val gmPreferences = GadgetManagerPreferences(context)

            val shellApi = GadgetShellApi()
            if(!shellApi.hasRootSync()) {
                Log.e(TAG, "No root permissions... aborting")
                return
            }
            Log.i(TAG, "Seems root is available... proceeding...")

            val assets = GadgetAssetProfiles(context.applicationContext as Application)
            val activeGadget = gmPreferences.getActiveBootGadget()

            for(profile in assets.allGadgetProfiles) {
                if( gmPreferences.isAddedDuringBoot(profile) ) {
                    Log.i(TAG, "Adding gadget $profile")

                    shellApi.exec(assets.getProfileGadget(profile)!!) { response ->
                        if (response != null && response.first == true && profile == activeGadget) {
                            Log.i(TAG, "Activating gadget $profile")
                            shellApi.activate("/config/usb_gadget/${profile}", null)
                        } else if (response != null && response.first == false) {
                            Log.e(TAG, "Activation failed: ${response.second}")
                        }
                    }
                }
            }

            Log.i(TAG, "Synchronous processing finished. ")
        }
    }
}