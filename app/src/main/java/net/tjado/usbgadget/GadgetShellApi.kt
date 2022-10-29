package net.tjado.usbgadget

import android.text.TextUtils
import androidx.lifecycle.MutableLiveData
import net.tjado.usbgadget.ExecuteAsRootUtil.canRunRootCommands
import net.tjado.usbgadget.RootTask.OnRootTaskListener
import java.io.BufferedReader
import java.io.StringReader
import java.util.*
import androidx.core.util.Pair as APair


class GadgetShellApi {
    fun exec(command: String): Boolean {
        return this.exec(arrayOf(command), null)
    }

    fun exec(command: String, onRootTaskFinished: OnRootTaskListener?): Boolean {
        return this.exec(arrayOf(command), onRootTaskFinished)
    }

    fun exec(commands: Array<String>, onRootTaskFinished: OnRootTaskListener? = null): Boolean {
        val mTask = RootTask(commands, onRootTaskFinished)
        mTask.execute()
        return true
    }

    fun hasRootSync(): Boolean {
        //return (Boolean) ExecuteAsRootUtil.execute("id").first;
        return canRunRootCommands()
    }

    fun updateDeviceInfo(returnDeviceData: MutableLiveData<TreeMap<String, String>?>) {
        val command = "echo KERNEL_VERSION=`(uname -r |cut -d '-' -f1 )` && (gunzip -c /proc/config.gz | grep -i configfs | sed 's/# //; s/ is not set/=NOT_SET/')\n"

        this.exec(command) { response: APair<*, *>? ->
            val status = response!!.first as Boolean
            val result = response.second as String
            if (!status || TextUtils.isEmpty(result)) {
                return@exec
            }

            val bufReader = BufferedReader(StringReader(result))
            val deviceData = TreeMap<String, String>(DeviceInfoMapComparator())
            try {
                bufReader.forEachLine {  line ->
                    val parts = line.split("=".toRegex(), 2).toTypedArray()
                    deviceData[parts[0].lowercase(Locale.getDefault())] = parts[1]
                }
            } catch (e: Exception) {
                Log.d("root", e.message!!)
                e.printStackTrace()
            }

            returnDeviceData.setValue(deviceData)
        }
    }

    fun updateGadgetData(
        returnGadgetList: MutableLiveData<List<GadgetObject>>,
        returnRootState: MutableLiveData<Boolean>
    ) {
        val data: MutableList<GadgetObject> = ArrayList()
        val cmd = "for dir in /config/usb_gadget/*/; do echo GADGET_PATH=\$dir; cd \$dir/configs/; echo CONFIG_PATH=\"\$dir/configs/`ls -1 | head -1`/\"; cd \$dir; if [ \"$?\" -ne \"0\" ]; then echo \"Error - not able to change dir to \$dir... exit\"; exit 1; fi; echo UDC=$(cat UDC); find ./configs/ -type l -exec sh -c 'echo FUNCTIONS_ACTIVE=$(basename $(readlink \"$@\"))' _ {} \\;; for f in `ls -1 ./functions/`; do echo FUNCTIONS=\$f; done; cd ./strings/0x409/; for vars in *; do echo \${vars}=$(cat \$vars); done; echo \"=============\"; done; \n"

        this.exec(cmd) { response: APair<*, *>? ->
            val status = response!!.first as Boolean
            val result = response.second as String

            returnRootState.value = status

            if (TextUtils.isEmpty(result)) {
                return@exec
            }

            val bufReader = BufferedReader(StringReader(result))
            data.clear()
            var gadget = GadgetObject()

            try {
                bufReader.forEachLine {  line ->
                    if (line == "=============") {
                        data.add(gadget)
                        gadget = GadgetObject()
                        return@forEachLine
                    }

                    val parts = line.split("=".toRegex(), 2).toTypedArray()

                    if (parts[0] == "FUNCTIONS") {
                        gadget.addFunction(parts[1])
                    } else if (parts[0] == "FUNCTIONS_ACTIVE") {
                        gadget.addActiveFunction(parts[1])
                    } else {
                        gadget.setValue(parts[0].lowercase(Locale.getDefault()), parts[1])
                    }
                }
            } catch (e: Exception) {
                Log.d("root", e.message!!)
                e.printStackTrace()
            }

            // refresh gadget data
            returnGadgetList.setValue(data)
        }
    }

    fun activate(gadgetPath: String?, onRootTaskFinished: OnRootTaskListener?): Boolean {
        if (!isValidGadgetPath(gadgetPath)) {
            return false
        }

        // deactivate all
        val cmdDeactivateUsbAll =
            "find /config/usb_gadget/  -name UDC -type f -exec sh -c 'echo \"\" >  \"$@\"' _ {} \\;\n"
        // activate specific one
        val cmdActivateUsb = String.format("getprop sys.usb.controller > %s/UDC\n", gadgetPath)
        val commands = arrayOf(cmdDeactivateUsbAll, cmdActivateUsb)
        return this.exec(commands, onRootTaskFinished)
    }

    fun deactivate(gadgetPath: String?, onRootTaskFinished: OnRootTaskListener?): Boolean {
        if (!isValidGadgetPath(gadgetPath)) {
            return false
        }
        val command = String.format("echo \"\" > %s/UDC\n", gadgetPath)
        return this.exec(command, onRootTaskFinished)
    }

    fun activateFunction(
        gadgetPath: String?,
        gadgetConfigPath: String?,
        function: String?,
        activateGadget: Boolean
    ): Boolean {
        if (!isValidGadgetPath(gadgetPath) || !isValidGadgetPath(gadgetConfigPath)) {
            return false
        }
        var onRootTaskFinished: OnRootTaskListener? = null
        if (activateGadget) {
            onRootTaskFinished =
                OnRootTaskListener { activate(gadgetPath, null) }
        }
        val command = String.format(
            "ln -s %s/functions/%3\$s %s/%3\$s\n",
            gadgetPath,
            gadgetConfigPath,
            function
        )
        return this.exec(command, onRootTaskFinished)
    }

    fun deactivateFunction(
        gadgetPath: String?,
        gadgetConfigPath: String?,
        function: String?,
        activateGadget: Boolean
    ): Boolean {
        if (!isValidGadgetPath(gadgetPath) || !isValidGadgetPath(gadgetConfigPath)) {
            return false
        }
        var onRootTaskFinished: OnRootTaskListener? = null
        if (activateGadget) {
            onRootTaskFinished =
                OnRootTaskListener { activate(gadgetPath, null) }
        }
        val command = String.format("rm %1\$s/%2\$s\n", gadgetConfigPath, function)
        return this.exec(command, onRootTaskFinished)
    }

    companion object {
        private const val tag = "GadgetShellApi"

        fun isValidGadgetPath(gadgetPath: String?): Boolean {
            if (gadgetPath != null && gadgetPath.startsWith("/config/")) {
                Log.d(tag, String.format("Gadget Path is valid", gadgetPath))
                return true
            }
            Log.w(tag, String.format("Gadget Path is not valid (%s)", gadgetPath))
            return false
        }
    }
}