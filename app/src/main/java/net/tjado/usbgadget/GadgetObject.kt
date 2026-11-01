package net.tjado.usbgadget

import net.tjado.usbgadget.RootTask.OnRootTaskListener


class GadgetObject {
    var values = HashMap<String, String>()
    var functions = ArrayList<String>()
        private set
    var activeFunctions = ArrayList<String>()
        private set
    private val hidMinors = HashMap<String, String>()
    private val keyboardFunctions = HashSet<String>()
    private val ctapFunctions = HashSet<String>()
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

    fun setHidMinor(function: String, minor: String) {
        hidMinors[function] = minor
    }

    fun setHidType(function: String, protocol: String, descriptorStart: String) {
        if (protocol == HID_PROTOCOL_KEYBOARD) {
            keyboardFunctions.add(function)
        }
        if (descriptorStart == FIDO_USAGE_PAGE_ITEM) {
            ctapFunctions.add(function)
        }
    }

    private fun activeNode(candidates: Set<String>): String? {
        if (!isActivated) {
            return null
        }
        return candidates.firstOrNull { activeFunctions.contains(it) }?.let { getHidNode(it) }
    }

    val activeKeyboardNode: String?
        get() = activeNode(keyboardFunctions)

    val activeCtapNode: String?
        get() = activeNode(ctapFunctions)

    /** The node depends on creation order across all gadgets, so it must not be assumed. */
    fun getHidNode(function: String): String? {
        return hidMinors[function]?.let { "/dev/hidg$it" }
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

    private companion object {
        const val HID_PROTOCOL_KEYBOARD = "1"
        const val FIDO_USAGE_PAGE_ITEM = "06d0f1"
    }

    fun activate(onRootTaskFinished: OnRootTaskListener? = null): Boolean {
        return shellApi.activate(getValue("gadget_path"), onRootTaskFinished)
    }

    fun deactivate(onRootTaskFinished: OnRootTaskListener? = null): Boolean {
        return shellApi.deactivate(getValue("gadget_path"), onRootTaskFinished)
    }

    fun activateFunction(
        function: String,
        reactivateGadget: Boolean,
        onRootTaskFinished: OnRootTaskListener? = null
    ): Boolean {
        val gadgetPath = getValue("gadget_path")
        val gadgetConfigPath = getValue("config_path")

        val status = shellApi.activateFunction(
            gadgetPath,
            gadgetConfigPath,
            function,
            isActivated && reactivateGadget,
            onRootTaskFinished
        )

        if (status) {
            // add activated function to activeFunctions to avoid refreshing configFS data
            activeFunctions.add(function)
        }

        return status
    }

    fun deactivateFunction(
        function: String,
        reactivateGadget: Boolean,
        onRootTaskFinished: OnRootTaskListener? = null
    ): Boolean {
        val gadgetPath = getValue("gadget_path")
        val gadgetConfigPath = getValue("config_path")

        val status = shellApi.deactivateFunction(
            gadgetPath,
            gadgetConfigPath,
            function,
            isActivated && reactivateGadget,
            onRootTaskFinished
        )

        if (status) {
            // remove deactivated function from activeFunctions to avoid refreshing configFS data
            activeFunctions.removeAll { anObject -> function == anObject }
        }

        return status
    }
}