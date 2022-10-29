package net.tjado.usbgadget

import android.content.Context
import android.content.SharedPreferences


class GadgetManagerPreferences(ctx: Context) {

    val sp: SharedPreferences

    companion object{
        var listenerRegistered: Boolean = false

        const val NAME = "GADGET_PREFERENCES"

        const val BOOT_CREATE_PREFIX = "GM_BOOT_CREATE_"
        const val BOOT_ACTIVATE = "GM_BOOT_ACTIVATE"
        const val BOOT_ACTIVATE_DEFAULT = "System Default"
    }

    init {
        sp = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    }

    fun setOnChangeListener(uiCallback: () -> Unit) {
        if(listenerRegistered) {
            return
        }
        listenerRegistered = true

        val sharedPreferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key.startsWith(BOOT_CREATE_PREFIX)) {
                val gadgetProfile = key.removePrefix(BOOT_CREATE_PREFIX)
                if (!isAddedDuringBoot(gadgetProfile) && gadgetProfile == getActiveBootGadget()) {
                    uiCallback()
                }
            }
        }

        sp.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
    }

    fun getActiveBootGadget(): String? {
        return sp.getString(
            BOOT_ACTIVATE,
            BOOT_ACTIVATE_DEFAULT
        )
    }

    fun setActiveBootGadget(gadgetName: String) : Boolean {
        if(isAddedDuringBoot(gadgetName) || gadgetName == BOOT_ACTIVATE_DEFAULT) {
            with(sp.edit()) {
                putString(BOOT_ACTIVATE, gadgetName)
                apply()
            }

            return true
        }

        return false
    }

    fun isAddedDuringBoot(gadgetName: String): Boolean {
        return sp.getBoolean(
            "${BOOT_CREATE_PREFIX}${gadgetName}",
            false
        )
    }

    fun setStateDuringBoot(gadgetName: String, added: Boolean) {
        with (sp.edit()) {
            putBoolean("${BOOT_CREATE_PREFIX}${gadgetName}", added)
            apply()
        }

        if (!added && gadgetName == getActiveBootGadget()) {
            setActiveBootGadget(BOOT_ACTIVATE_DEFAULT)
        }
    }
}