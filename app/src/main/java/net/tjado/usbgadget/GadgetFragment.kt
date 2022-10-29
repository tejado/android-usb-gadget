package net.tjado.usbgadget


import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle

import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.fragment.app.Fragment
import net.tjado.usbgadget.databinding.FragmentGadgetBinding
import net.tjado.usbgadget.databinding.RowFunctionBinding


class GadgetFragment(private var gadget: GadgetObject) : Fragment() {
    private var _binding: FragmentGadgetBinding? = null
    private val binding get() = _binding!!

    private lateinit var gadgetViewModel: GadgetViewModel
    private lateinit var functionProfileList: Array<String>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGadgetBinding.inflate(inflater, container, false)
        val view = binding.root

        gadgetViewModel = ViewModelProvider(requireActivity()).get(GadgetViewModel::class.java)
        gadgetViewModel.gadgets.observe(viewLifecycleOwner) { item: List<GadgetObject> ->
            for (g in item) {
                if (g.getValue("gadget_path") == gadget.getValue("gadget_path")) {
                    gadget = g
                    refreshData()
                }
            }
        }

        gadgetViewModel.hasRootPermissions().observe(viewLifecycleOwner) {
                item -> binding.flipper.displayedChild = if (item) 0 else 1
        }

        val assets = GadgetAssetProfiles(requireActivity().application)
        functionProfileList = assets.allFunctionProfiles

        val addFunction = view.findViewById<Button>(R.id.btn_function_add)
        addFunction.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Add Function to Gadget")
                .setCancelable(true)
                .setItems(functionProfileList) { _, which ->
                    gadgetViewModel.loadFunctionProfile(
                        gadget,
                        functionProfileList[which]
                    )
                }.show()
        }

        refreshData()
        return view
    }

    private fun refreshData() {
        binding.tvGadgetManufacturer.text = gadget.getValue("manufacturer")
        binding.tvGadgetProduct.text = gadget.getValue("product")
        binding.tvGadgetSn.text = gadget.getValue("serialnumber")
        binding.tvGadgetPath.text = gadget.getValue("gadget_path")
        binding.tvGadgetUdc.text = gadget.getValue("udc")
        binding.tvGadgetStatus.isChecked = gadget.isActivated
        binding.tvGadgetStatus.setOnClickListener {
            if (binding.tvGadgetStatus.isChecked) {
                gadget.activate { gadgetViewModel.updateGadgetData() }
            } else {
                gadget.deactivate { gadgetViewModel.updateGadgetData() }
            }
        }

        binding.listFunctons.removeAllViews()

        val functions = gadget.functions
        for (functionName in functions) {
            val rowBinding = RowFunctionBinding.inflate(layoutInflater)
            rowBinding.name.text = functionName

            rowBinding.status.isChecked = gadget.activeFunctions.contains(functionName)
            rowBinding.status.setOnClickListener {
                if (rowBinding.status.isChecked) {
                    gadget.activateFunction(functionName, false)
                } else {
                    gadget.deactivateFunction(functionName, true)
                }
                gadgetViewModel.reloadGadgetData()
            }

            binding.listFunctons.addView(rowBinding.root)
        }
    }
}