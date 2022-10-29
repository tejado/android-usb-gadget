package net.tjado.usbgadget

import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import android.graphics.Typeface
import android.text.Html
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import net.tjado.usbgadget.databinding.FragmentDeviceInfoBinding
import net.tjado.usbgadget.databinding.RowDeviceInfoBinding
import java.util.*


class DeviceInfoFragment : Fragment() {
    private var _binding: FragmentDeviceInfoBinding? = null
    private val binding get() = _binding!!

    private var deviceData: MutableLiveData<TreeMap<String, String>?>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeviceInfoBinding.inflate(inflater, container, false)
        val view = binding.root

        deviceData = MutableLiveData()
        deviceData!!.value = TreeMap(DeviceInfoMapComparator())
        deviceData!!.observe(viewLifecycleOwner) { item: TreeMap<String, String>? -> loadData(item) }
        val shellAPi = GadgetShellApi()
        shellAPi.updateDeviceInfo(deviceData!!)

        return view
    }

    private fun loadData(deviceData: Map<String, String>?) {
        binding.listDeviceData.removeAllViews()

        val rowHead = RowDeviceInfoBinding.inflate(layoutInflater)
        rowHead.name.text = "Kernel Config Parameter"
        rowHead.name.setTypeface(null, Typeface.BOLD)
        rowHead.value.text = "Value"
        rowHead.value.setTypeface(null, Typeface.BOLD)

        binding.listDeviceData.addView(rowHead.root)

        for (entry in deviceData!!.entries) {
            val rowEntry = RowDeviceInfoBinding.inflate(layoutInflater)
            rowEntry.name.text = entry.key.uppercase(Locale.getDefault())

            var value = entry.value
            var color: String
            when (value) {
                "y" -> {
                    value = "Yes"
                    color = "#008000"
                }
                "n" -> {
                    value = "No"
                    color = "#ff0000"
                }
                "NOT_SET" -> {
                    value = "Not set"
                    color = "#ff0000"
                }
                else -> color = "#000000"
            }
            rowEntry.value.text = Html.fromHtml(
                String.format("<font color=%s>%s</font>", color, value),
                Html.FROM_HTML_MODE_LEGACY
            )

            binding.listDeviceData.addView(rowEntry.root)
        }
    }
}