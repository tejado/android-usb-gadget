package net.tjado.usbgadget

import android.app.AlertDialog
import android.app.Application
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import net.tjado.usbgadget.databinding.FragmentGadgetManagerBinding


class GadgetManagerFragment : Fragment() {
    private var _binding: FragmentGadgetManagerBinding? = null
    private val binding get() = _binding!!

    private lateinit var gmPreferences: GadgetManagerPreferences

    private var gadgetProfilesAdapter: GadgetManagerProfilesAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGadgetManagerBinding.inflate(inflater, container, false)
        val view = binding.root

        gmPreferences = GadgetManagerPreferences(requireContext())

        // Load list of available gadget profiles
        val gadgetProfileList = GadgetAssetProfiles(context?.applicationContext as Application).allGadgetProfiles

        // create a new gadget list which also includes a system default
        val spinnerGadgetList = arrayOfNulls<String>(gadgetProfileList.size + 1)
        spinnerGadgetList[0] = GadgetManagerPreferences.BOOT_ACTIVATE_DEFAULT
        System.arraycopy(gadgetProfileList, 0, spinnerGadgetList, 1, gadgetProfileList.size)

        // initialize adapter for RecyclerView
        gadgetProfilesAdapter = GadgetManagerProfilesAdapter(requireActivity(), gadgetProfileList)
        binding.rvGadgetProfiles.adapter = gadgetProfilesAdapter
        val gridLayoutManager = GridLayoutManager(activity, 1)
        binding.rvGadgetProfiles.layoutManager = gridLayoutManager

        // initialize spinner
        val adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, spinnerGadgetList)
        adapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice)
        binding.spinnerBootActivate.adapter = adapter

        // set current active boot gadget as spinner selection
        binding.spinnerBootActivate.setSelection(spinnerGadgetList.indexOf(gmPreferences.getActiveBootGadget()), false)

        // UI callback when preference is changed in RecyclerView
        // (in case gadget gets deactivated, it can't be active anymore)
        gmPreferences.setOnChangeListener { binding.spinnerBootActivate.setSelection(0, true) }

        // increase the spinner click surface
        binding.spinnerLayout.setOnClickListener {
            binding.spinnerBootActivate.performClick()
        }

        // When item gets selected in the spinner, check if the gadget can be activated
        binding.spinnerBootActivate.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val activeBootGadget = gmPreferences.setActiveBootGadget(spinnerGadgetList[position].toString())
                if(!activeBootGadget) {
                    AlertDialog.Builder(requireContext())
                        .setTitle(R.string.warning)
                        .setMessage(R.string.diag_warning_boot_mismatch)
                        .setCancelable(false)
                        .setNegativeButton(R.string.btn_cancel, null).show()

                    binding.spinnerBootActivate.setSelection(spinnerGadgetList.indexOf(gmPreferences.getActiveBootGadget()), false)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.warning)
            .setMessage(R.string.diag_warning_boot)
            .setCancelable(false)
            .setNegativeButton(R.string.btn_understood, null).show()

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}