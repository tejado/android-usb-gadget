package net.tjado.usbgadget

class RootTask : CoroutineTask<Void?, RootResult> {
    private var listener: OnRootTaskListener?
    private var mCommands: Array<String>

    fun interface OnRootTaskListener {
        fun onRootTaskFinish(response: RootResult)
    }

    constructor(command: String, listener: OnRootTaskListener?) {
        mCommands = arrayOf(command)
        this.listener = listener
    }

    constructor(commands: Array<String>, listener: OnRootTaskListener?) {
        mCommands = commands
        this.listener = listener
    }

    override fun doInBackground(vararg params: Void?): RootResult {
        return ExecuteAsRootUtil.execute(mCommands)
    }

    override fun onPostExecute(result: RootResult) {
        if (listener != null) {
            listener!!.onRootTaskFinish(result)
        }
    }
}