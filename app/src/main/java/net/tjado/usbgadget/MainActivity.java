package net.tjado.usbgadget;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ViewFlipper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends AppCompatActivity {

    private GadgetAdapter gadgetAdapter;
    private RecyclerView gadgetRecyclerView;

    List<GadgetObject> data;
    String [] gadgetProfileList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gadgetRecyclerView = (RecyclerView) findViewById(R.id.rv_gadgets);
        data = new ArrayList<>();

        if( ExecuteAsRootUtil.canRunRootCommands() ) {
            getGadgetData();
        } else {
            ((ViewFlipper) findViewById(R.id.flipper)).showNext();
        }


        try {
            gadgetProfileList = getAssets().list("usbGadgetProfiles/");
        } catch (IOException e) {
            gadgetProfileList = null;
        }

        gadgetAdapter = new GadgetAdapter(this, data);
        gadgetRecyclerView.setAdapter(gadgetAdapter);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this,1);
        gadgetRecyclerView.setLayoutManager(gridLayoutManager);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_add:
                addGadget();
                break;
            case R.id.action_refresh:
                ((ViewFlipper) findViewById(R.id.flipper)).setDisplayedChild(0);
                if( ExecuteAsRootUtil.canRunRootCommands() ) {
                    getGadgetData();
                } else {
                    ((ViewFlipper) findViewById(R.id.flipper)).showNext();
                }
                return true;
            case R.id.info:
                showInfo();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void getGadgetData() {
        String cmd = "for dir in /config/usb_gadget/*/; do echo GADGET_PATH=$dir; cd $dir; if [ \"$?\" -ne \"0\" ]; then echo \"Error - not able to change dir to $dir... exit\"; exit 1; fi; echo UDC=$(cat UDC); find ./configs/ -type l -exec sh -c 'echo FUNCTIONS_ACTIVE=$(basename $(readlink \"$@\"))' _ {} \\;; for f in ./functions/*/; do echo FUNCTIONS=$(basename $f); done; cd ./strings/0x409/; for vars in *; do echo ${vars}=$(cat $vars); done; echo \"=============\"; done; \n";

        RootTask mTask = new RootTask(this.getApplicationContext(), cmd, response -> {
            //mTextView.setText((String) Response);
            BufferedReader bufReader = new BufferedReader(new StringReader(response));
            data.clear();
            GadgetObject gadget = new GadgetObject();

            try {
                String line;
                while ((line = bufReader.readLine()) != null) {
                    if(line.equals("=============")) {
                        data.add(gadget);
                        gadget = new GadgetObject();
                        continue;
                    }

                    String[] parts = line.split("=",2);

                    if( parts[0].equals("FUNCTIONS") ) {
                        gadget.addFunction(parts[1]);
                    } else if( parts[0].equals("FUNCTIONS_ACTIVE") ) {
                        gadget.addActiveFunction(parts[1]);
                    } else {
                        gadget.setValue(parts[0].toLowerCase(), parts[1]);
                    }
                }
            }
            catch (Exception e) {
                Log.d("root", e.getMessage());
                e.printStackTrace();
            }

            gadgetAdapter.notifyDataSetChanged();

        });
        mTask.execute();
    }

    protected void showInfo() {
        final AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle(R.string.info_title);
        alertBuilder.setCancelable(false);

        alertBuilder.setMessage(R.string.info_message);
        alertBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialoginterface, int i) {
            }});

        alertBuilder.show();
    }

    protected void addGadget() {
        final AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle("Add Gadget");
        alertBuilder.setCancelable(true);

        alertBuilder.setItems(gadgetProfileList, (dialog, which) -> loadGadgetProfileFromAsset(gadgetProfileList[which]));

        alertBuilder.show();
    }

    protected String getGadgetProfile(String assetFile) {
        BufferedReader reader = null;
        try {
            InputStream is = getAssets().open(String.format("usbGadgetProfiles/%s", assetFile));
            Scanner scanner = new Scanner(is);

            StringBuffer sb = new StringBuffer();
            while (scanner.hasNext()) {
                sb.append(scanner.nextLine() + "\n");
            }
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected Boolean loadGadgetProfileFromAsset(String assetFile) {
        String profile = getGadgetProfile(assetFile);

        if (profile == null || profile.equals("")) {
            return false;
        }

        RootTask mTask = new RootTask(this.getApplicationContext(), profile, response -> {
            getGadgetData();
        });
        mTask.execute();

        return true;
    }

    protected void runShell() {
        String cmd = "find /config/usb_gadget/ -exec ls -la {} \\; 2>&1\n";
        RootTask mTask = new RootTask(this.getApplicationContext(), cmd, response -> {
            //mTextView.setText((String) response);
        });
        mTask.execute();

        //Pair cr = ExecuteAsRootUtil.execute("find /config/usb_gadget/ -exec ls -la {} \\; 2>&1\n");
        //mTextView.setText((String) cr.second);
    }
}