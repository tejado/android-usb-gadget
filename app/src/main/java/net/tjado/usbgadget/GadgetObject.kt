package net.tjado.usbgadget

import net.tjado.usbgadget.RootTask.OnRootTaskListener


class GadgetObject {
    var values = HashMap<String, String>()
    var functions = ArrayList<String>()
        private set
    var activeFunctions = ArrayList<String>()
        private set
    var shellApi = GadgetShellApi()

    fun setValue(key: String, value: String) {
        values[key] = value
    }

    fun getValue(key: String): String {
        return try {
            values.getValue(key)
        } catch (e: Exception) {
            "not set"
        }
    }

    fun addFunction(f: String) {
        functions.add(f)
    }

    fun addActiveFunction(f: String) {
        activeFunctions.add(f)
    }

    val isActivated: Boolean
        get() {
            val udc = getValue("udc")
            return udc != "" && udc != "not set"
        }

    val formattedFunctions: String
        get() {
            val sb = StringBuffer()
            for (function in functions) {
                var color = "#ff0000"
                if (activeFunctions.contains(function)) {
                    color = "#008000"
                }
                sb.append(String.format("<font color=%s>%s</font><br />", color, function))
            }
            return sb.toString()
        }

    fun activate(onRootTaskFinished: OnRootTaskListener? = null): Boolean {
        return shellApi.activate(getValue("gadget_path"), onRootTaskFinished)
    }

    fun deactivate(onRootTaskFinished: OnRootTaskListener? = null): Boolean {
        return shellApi.deactivate(getValue("gadget_path"), onRootTaskFinished)
    }

    fun activateFunction(function: String, reactivateGadget: Boolean): Boolean {
        val gadgetPath = getValue("gadget_path")
        val gadgetConfigPath = getValue("config_path")

        val status = shellApi.activateFunction(
            gadgetPath,
            gadgetConfigPath,
            function,
            isActivated && reactivateGadget
        )

        if (status) {
            // add activated function to activeFunctions to avoid refreshing configFS data
            activeFunctions.add(function)
        }

        return status
    }

    fun deactivateFunction(function: String, reactivateGadget: Boolean): Boolean {
        val gadgetPath = getValue("gadget_path")
        val gadgetConfigPath = getValue("config_path")

        val status = shellApi.deactivateFunction(
            gadgetPath,
            gadgetConfigPath,
            function,
            isActivated && reactivateGadget
        )

        if (status) {
            // remove deactivated function from activeFunctions to avoid refreshing configFS data
            activeFunctions.removeIf { anObject -> function == anObject }
        }

        return status
    }
}