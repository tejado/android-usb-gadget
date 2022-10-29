package net.tjado.usbgadget

import android.app.Activity
import android.graphics.Color
import android.text.Html
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.tjado.usbgadget.databinding.CardviewGadgetBinding
import java.lang.ref.WeakReference


class GadgetAdapter(
    private val context: Activity,
    private var data: List<GadgetObject>,
    private val listener: GadgetAdapterClickInterface
) : RecyclerView.Adapter<GadgetAdapter.DefaultViewHolder>() {
    private val inflater: LayoutInflater? = null

    private var _binding: CardviewGadgetBinding? = null
    private val binding get() = _binding!!

    interface GadgetAdapterClickInterface {
        fun onGadgetClicked(gadget: GadgetObject)
        fun onStatusChange(gadget: GadgetObject, newStatus: Boolean)
    }

    // Inflate the layout when viewholder created
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefaultViewHolder {
        _binding = CardviewGadgetBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return DefaultViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: DefaultViewHolder, position: Int) {
        with(holder) {
            gadget = data[position]
            binding.tvGadgetManufacturer.text = gadget?.getValue("manufacturer")
            binding.tvGadgetProduct.text = gadget?.getValue("product")
            binding.tvGadgetSn.text = gadget?.getValue("serialnumber")
            binding.tvGadgetPath.text = gadget?.getValue("gadget_path")
            binding.tvGadgetFunctions.text = Html.fromHtml(holder.gadget?.formattedFunctions, Html.FROM_HTML_MODE_LEGACY)
            binding.tvGadgetStatus.isChecked = gadget?.isActivated == true

            val udc = gadget!!.getValue("udc")
            binding.tvGadgetUdc.text = udc
            if (gadget?.isActivated == true) {
                binding.cvGadget.setCardBackgroundColor(Color.WHITE)
            } else {
                binding.cvGadget.setCardBackgroundColor(Color.LTGRAY)
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    // Static inner class to initialize the views of rows
    inner class DefaultViewHolder(val binding: CardviewGadgetBinding, clickInterface: GadgetAdapterClickInterface) : RecyclerView.ViewHolder(binding.root) {
        var gadget: GadgetObject? = null
        private val listenerRef: WeakReference<GadgetAdapterClickInterface>

        init {
            listenerRef = WeakReference(clickInterface)

            binding.tvGadgetStatus.setOnClickListener {
                gadget?.let { listenerRef.get()?.onStatusChange(it, binding.tvGadgetStatus.isChecked) }
            }

            itemView.setOnClickListener {
                gadget?.let { listenerRef.get()?.onGadgetClicked(it) }
            }
        }
    }
}