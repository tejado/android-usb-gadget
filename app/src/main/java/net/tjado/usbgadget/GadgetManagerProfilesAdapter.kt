package net.tjado.usbgadget

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.tjado.usbgadget.databinding.CardviewGadgetProfileBinding


class GadgetManagerProfilesAdapter(
    private val context: Activity,
    private var profiles: Array<String>
) : RecyclerView.Adapter<GadgetManagerProfilesAdapter.DefaultViewHolder>() {

    private var _binding: CardviewGadgetProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var gmPreferences: GadgetManagerPreferences

    // Inflate the layout when viewholder created
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefaultViewHolder {
        _binding = CardviewGadgetProfileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        gmPreferences = GadgetManagerPreferences(context)

        return DefaultViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DefaultViewHolder, position: Int) {
        with(holder) {
            binding.tvGadgetProfile.text = profiles[position]

            binding.gadgetBootCreate.setOnCheckedChangeListener { _, isChecked ->
                gmPreferences.setStateDuringBoot(profiles[position], isChecked)
            }

            binding.gadgetBootCreate.isChecked = gmPreferences.isAddedDuringBoot(profiles[position])
        }
    }

    override fun getItemCount() = profiles.count()

    inner class DefaultViewHolder(val binding: CardviewGadgetProfileBinding) : RecyclerView.ViewHolder(binding.root)
}