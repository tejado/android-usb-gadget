package net.tjado.usbgadget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import net.tjado.usbgadget.Log.OnLogChangeListener
import net.tjado.usbgadget.databinding.FragmentLogBinding


class LogFragment : Fragment() {
    private var _binding: FragmentLogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.log.text = Log.getLog()
        Log.onLogChangeListener = OnLogChangeListener {
            activity?.runOnUiThread { binding.log.text = Log.getLog() }
        }
        return view
    }
}