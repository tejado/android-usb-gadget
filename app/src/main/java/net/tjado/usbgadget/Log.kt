package net.tjado.usbgadget

import android.util.Log as ALog

class Log {
    fun interface OnLogChangeListener {
        fun onLogChanged()
    }

    fun setOnDataChangeListener(onLogChangeListener: OnLogChangeListener?) {
        Log.onLogChangeListener = onLogChangeListener
    }

    companion object {
        private var log = StringBuffer()
        var onLogChangeListener: OnLogChangeListener? = null

        // error
        fun e(tag: String, msg: String) {
            ALog.e(tag, msg)
            internalLog(tag, msg)
        }

        fun e(tag: String, msg: String, t: Throwable?) {
            ALog.e(tag, msg, t)
            internalLog(tag, msg, t)
        }

        // debug
        fun d(tag: String, msg: String) {
            ALog.d(tag, msg)
            internalLog(tag, msg)
        }

        fun d(tag: String, msg: String, t: Throwable?) {
            ALog.d(tag, msg, t)
            internalLog(tag, msg, t)
        }

        // warning
        fun w(tag: String, msg: String) {
            ALog.w(tag, msg)
            internalLog(tag, msg)
        }

        fun w(tag: String, msg: String, t: Throwable?) {
            ALog.w(tag, msg, t)
            internalLog(tag, msg, t)
        }

        // info
        fun i(tag: String, msg: String) {
            ALog.i(tag, msg)
            internalLog(tag, msg)
        }

        fun i(tag: String, msg: String, t: Throwable?) {
            ALog.i(tag, msg, t)
            internalLog(tag, msg, t)
        }

        fun internalLog(tag: String, msg: String, t: Throwable? = null) {
            log.append(String.format("%s - %s", tag, msg))
            if (t != null) {
                log.append(": ")
                log.append(t.message)
            }
            log.append(System.getProperty("line.separator"))
            onLogChangeListener?.onLogChanged()
        }

        fun getLog(): String {
            return log.toString()
        }
    }
}