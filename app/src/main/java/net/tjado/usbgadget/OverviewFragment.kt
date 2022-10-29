package net.tjado.usbgadget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import net.tjado.usbgadget.databinding.FragmentOverviewBinding


class OverviewFragment : Fragment() {
    private var _binding: FragmentOverviewBinding? = null
    private val binding get() = _binding!!

    private var gadgetAdapter: GadgetAdapter? = null
    private lateinit var gadgetViewModel: GadgetViewModel
    private lateinit var gadgetData: MutableList<GadgetObject>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOverviewBinding.inflate(inflater, container, false)
        val view = binding.root

        gadgetViewModel = ViewModelProvider(requireActivity()).get(GadgetViewModel::class.java)

        gadgetData = ArrayList()
        gadgetViewModel.gadgets.observe(viewLifecycleOwner) { item: List<GadgetObject>? ->
            (gadgetData as ArrayList<GadgetObject>).clear()
            (gadgetData as ArrayList<GadgetObject>).addAll(item!!)
            gadgetAdapter?.notifyDataSetChanged()
        }

        // disable certain tabs in case of missing root
        gadgetViewModel.hasRootPermissions().observe(viewLifecycleOwner) { item: Boolean ->
            binding.flipper.displayedChild = if (item) 0 else 1
            (activity as MainActivity).setTabStatus(1, item)
        }

        gadgetAdapter = GadgetAdapter(requireActivity(), gadgetData, object : GadgetAdapter.GadgetAdapterClickInterface {
            override fun onGadgetClicked(gadget: GadgetObject) {
                val openFragment = GadgetFragment(gadget)
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace((view.parent as ViewGroup).id, openFragment, "GadgetFragment")
                    .addToBackStack(null)
                    .commit()
            }

            override fun onStatusChange(gadget: GadgetObject, newStatus: Boolean) {
                if (newStatus) {
                    gadget.activate { gadgetViewModel.updateGadgetData() }
                } else {
                    gadget.deactivate { gadgetViewModel.updateGadgetData() }
                }
            }
        })

        binding.rvGadgets.adapter = gadgetAdapter
        val gridLayoutManager = GridLayoutManager(activity, 1)
        binding.rvGadgets.layoutManager = gridLayoutManager

        return view
    }
}