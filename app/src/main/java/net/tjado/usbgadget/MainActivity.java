package net.tjado.usbgadget;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity {

    private GadgetViewModel gadgetViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewPager2 viewPager2 = findViewById(R.id.viewpager);
        viewPager2.setAdapter(new ViewPager2FragmentStateAdapter(this));

        TabLayout tabLayout = findViewById(R.id.tabs);
        TabLayoutMediator tabLayoutMediator = new TabLayoutMediator(tabLayout, viewPager2,
                new TabLayoutMediator.TabConfigurationStrategy() {
                    @Override
                    public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                        switch (position) {
                            case (0):
                                tab.setText("Gadgets");
                                break;
                            case (1):
                                tab.setText("Logs");
                                break;
                        }
                    }
                });
        tabLayoutMediator.attach();

        gadgetViewModel = new ViewModelProvider(this).get(GadgetViewModel.class);
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
            case R.id.action_refresh:
                refreshGadgets();
                return true;
            case R.id.action_add:
                addGadget();
                break;
            case R.id.info:
                showInfo();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void refreshGadgets() {
        gadgetViewModel.updateGadgetData();
    }

    protected void addGadget() {
        final AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle("Add Gadget");
        alertBuilder.setCancelable(true);

        alertBuilder.setItems(gadgetViewModel.gadgetProfileList, (dialog, which) -> gadgetViewModel.loadGadgetProfileFromAsset(gadgetViewModel.gadgetProfileList[which]));

        alertBuilder.show();
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
}