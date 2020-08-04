package net.tjado.usbgadget;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Pair;

public class RootTask extends AsyncTask<Void, Void, String> {
    OnRootTaskListener listener;
    Context mContext;
    String[] mCommands;

    public interface OnRootTaskListener {
        public void OnRootTaskFinish(String Response);
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
    protected String doInBackground(Void... params) {
        Pair cr = ExecuteAsRootUtil.execute(mCommands);
        return (String) cr.second;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(String result) {
        if( listener != null ) {
            listener.OnRootTaskFinish(result);
        }
    }
}
