package net.tjado.usbgadget;

import android.os.AsyncTask;
import android.util.Pair;

public class RootTask extends AsyncTask<Void, Void, Pair> {
    OnRootTaskListener listener;
    String[] mCommands;

    public interface OnRootTaskListener {
        void OnRootTaskFinish(Pair response);
    }

    public RootTask(String command, OnRootTaskListener listener) {
        mCommands = new String[] {command};
        this.listener = listener;
    }

    public RootTask(String[] commands, OnRootTaskListener listener) {
        mCommands = commands;
        this.listener = listener;
    }

    @Override
    protected Pair doInBackground(Void... params) {
        return ExecuteAsRootUtil.execute(mCommands);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(Pair result) {
        if( listener != null ) {
            listener.OnRootTaskFinish(result);
        }
    }
}
