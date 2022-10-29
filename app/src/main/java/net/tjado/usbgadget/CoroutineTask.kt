package net.tjado.usbgadget

import kotlinx.coroutines.*

// gist by Shanmuga Santhosh A
// https://gist.github.com/shanmugasanthosh7/417f413f359d10c8f4581542a11b3f91/
abstract class CoroutineTask<Params, APair> {

    protected open fun onPreExecute() {}

    protected abstract fun doInBackground(vararg params: Params): APair

    protected open fun onPostExecute(result: APair) {}

    private val mUiScope by lazy { CoroutineScope(Dispatchers.Main) }

    private lateinit var mJob: Job

    fun execute(vararg params: Params) {
        mJob = mUiScope.launch {
            // run on UI/Main thread
            onPreExecute()

            // execute on worker thread
            val result = async(Dispatchers.IO) { doInBackground(*params) }

            // return result on UI/Main thread
            onPostExecute(result.await())
        }
    }

    fun cancel() {
        if (::mJob.isInitialized && mJob.isActive) mJob.cancel()
    }

    fun cancelAll() {
        if (mUiScope.isActive) mUiScope.cancel()
    }
}