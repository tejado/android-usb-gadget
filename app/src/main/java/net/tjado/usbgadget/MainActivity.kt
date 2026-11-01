package net.tjado.usbgadget

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import net.tjado.usbgadget.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var gadgetViewModel: GadgetViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // Android 15+ enforces edge-to-edge: keep content clear of the system bars and cutouts.
        // The top inset goes on the app bar, so that its color extends behind the status bar.
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or
                    WindowInsetsCompat.Type.ime()
            )
            view.setPadding(bars.left, 0, bars.right, bars.bottom)
            binding.appBar.setPadding(0, bars.top, 0, 0)
            insets
        }

        binding.viewpager.adapter = ViewPager2FragmentStateAdapter(this)

        // reduce sensitivity of ViewPager2
        // https://stackoverflow.com/a/60672891
        try {
            val recyclerViewField = ViewPager2::class.java.getDeclaredField("mRecyclerView")
            recyclerViewField.isAccessible = true
            val recyclerView = recyclerViewField[binding.viewpager] as RecyclerView
            val touchSlopField = RecyclerView::class.java.getDeclaredField("mTouchSlop")
            touchSlopField.isAccessible = true
            val touchSlop = touchSlopField[recyclerView] as Int
            touchSlopField[recyclerView] = touchSlop * 3
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }

        val tabLayoutMediator = TabLayoutMediator(
            binding.tabs, binding.viewpager, true, false
        ) { tab, position ->
            when (position) {
                0 -> tab.text = "Gadgets"
                1 -> tab.text = "Device Info"
                2 -> tab.text = "BOOT"
                3 -> tab.text = "Tools"
                4 -> tab.text = "Logs"
            }
        }
        tabLayoutMediator.attach()

        gadgetViewModel = ViewModelProvider(this).get(GadgetViewModel::class.java)
        gadgetViewModel.commandError.observe(this) { error ->
            if (error != null) {
                gadgetViewModel.commandError.value = null
                AlertDialog.Builder(this)
                    .setTitle(R.string.command_error_title)
                    .setMessage(error)
                    .setPositiveButton("Ok", null)
                    .show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_add ->  {
                addGadget()
                return true
            }
            R.id.action_refresh -> {
                refreshGadgets()
                return true
            }
            R.id.info -> {
                showInfo()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun refreshGadgets() {
        gadgetViewModel.updateGadgetData()
    }

    private fun addGadget() {
        AlertDialog.Builder(this)
            .setTitle("Add Gadget")
            .setCancelable(true)
            .setItems(gadgetViewModel.gadgetProfileList) { _, which ->
                gadgetViewModel.loadGadgetProfile(
                    gadgetViewModel.gadgetProfileList[which]
                )
            }.show()
    }

    private fun showInfo() {
        AlertDialog.Builder(this)
            .setTitle(R.string.info_title)
            .setCancelable(false)
            .setMessage(R.string.info_message)
            .setPositiveButton("Ok", null)
            .show()
    }

    fun setTabStatus(id: Int, isClickable: Boolean) {
       binding.tabs.getTabAt(id)?.view?.isClickable = isClickable
    }
}