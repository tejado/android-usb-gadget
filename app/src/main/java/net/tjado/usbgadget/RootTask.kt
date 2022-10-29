package net.tjado.usbgadget

import androidx.core.util.Pair as APair

class RootTask : CoroutineTask<Void?, APair<*, *>> {
    private var listener: OnRootTaskListener?
    private var mCommands: Array<String>

    fun interface OnRootTaskListener {
        fun onRootTaskFinish(response: APair<*, *>?)
    }

    constructor(command: String, listener: OnRootTaskListener?) {
        mCommands = arrayOf(command)
        this.listener = listener
    }

    constructor(commands: Array<String>, listener: OnRootTaskListener?) {
        mCommands = commands
        this.listener = listener
    }

    override fun doInBackground(vararg params: Void?): APair<*, *> {
        return ExecuteAsRootUtil.execute(mCommands)
    }

    override fun onPostExecute(result: APair<*, *>) {
        if (listener != null) {
            listener!!.onRootTaskFinish(result)
        }
    }
}