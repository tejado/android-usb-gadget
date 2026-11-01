package net.tjado.usbgadget

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.MutableLiveData
import net.tjado.usbgadget.databinding.FragmentToolsBinding

/** Keeps the test authenticator running across screen rotation. */
class ToolsViewModel : ViewModel() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var session: CtapHidSession? = null

    val webAuthnRunning = MutableLiveData(false)
    val webAuthnLog = MutableLiveData("")

    fun startWebAuthnTest(node: String) {
        if (session != null) {
            return
        }
        webAuthnLog.value = ""
        webAuthnRunning.value = true
        session = CtapHidSession(
            node,
            onEvent = { event -> mainHandler.post { appendLog(event) } },
            onStopped = {
                mainHandler.post {
                    session = null
                    webAuthnRunning.value = false
                }
            }
        ).also { it.start() }
    }

    fun stopWebAuthnTest() {
        session?.stop()
    }

    private fun appendLog(event: String) {
        Log.i("WebAuthnTest", event)
        webAuthnLog.value = webAuthnLog.value + event + "\n"
    }

    override fun onCleared() {
        stopWebAuthnTest()
    }
}

class ToolsFragment : Fragment() {
    private var _binding: FragmentToolsBinding? = null
    private val binding get() = _binding!!

    private val shellApi = GadgetShellApi()
    private lateinit var gadgetViewModel: GadgetViewModel
    private lateinit var toolsViewModel: ToolsViewModel
    private var typer: KeyboardTyper? = null
    private var typerNode: String? = null
    private var typedText = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentToolsBinding.inflate(inflater, container, false)

        gadgetViewModel = ViewModelProvider(requireActivity()).get(GadgetViewModel::class.java)
        toolsViewModel = ViewModelProvider(requireActivity()).get(ToolsViewModel::class.java)

        gadgetViewModel.gadgets.observe(viewLifecycleOwner) { showNodes() }
        toolsViewModel.webAuthnRunning.observe(viewLifecycleOwner) { showNodes() }
        toolsViewModel.webAuthnLog.observe(viewLifecycleOwner) { binding.webauthnLog.text = it }

        binding.btnKeyboardType.setOnClickListener { typeTestText() }
        binding.btnKeyboardRelease.setOnClickListener { releaseKeys() }
        binding.btnKeyboardOpen.setOnClickListener { openKeyboard() }
        binding.keyboardInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                sendTypedText(s.toString())
            }
        })
        binding.btnWebauthn.setOnClickListener { toggleWebAuthnTest() }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        gadgetViewModel.updateGadgetData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        typer?.close()
        typer = null
        typerNode = null
        _binding = null
    }

    private fun keyboardNode(): String? {
        return gadgetViewModel.gadgets.value.orEmpty().firstNotNullOfOrNull { it.activeKeyboardNode }
    }

    private fun ctapNode(): String? {
        return gadgetViewModel.gadgets.value.orEmpty().firstNotNullOfOrNull { it.activeCtapNode }
    }

    private fun showNodes() {
        val keyboard = keyboardNode()
        binding.tvKeyboardNode.text = keyboard ?: getString(R.string.tools_no_keyboard)
        binding.btnKeyboardType.isEnabled = keyboard != null
        binding.btnKeyboardRelease.isEnabled = keyboard != null
        binding.btnKeyboardOpen.isEnabled = keyboard != null
        binding.keyboardInput.isEnabled = keyboard != null

        val ctap = ctapNode()
        val running = toolsViewModel.webAuthnRunning.value == true
        binding.tvWebauthnNode.text = ctap ?: getString(R.string.tools_no_ctap)
        binding.btnWebauthn.isEnabled = ctap != null || running
        binding.btnWebauthn.setText(if (running) R.string.tools_webauthn_stop else R.string.tools_webauthn_start)
    }

    private fun openKeyboard() {
        val input = binding.keyboardInput
        input.visibility = View.VISIBLE
        input.requestFocus()
        requireContext().getSystemService(InputMethodManager::class.java)
            .showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        binding.tvKeyboardStatus.setText(R.string.tools_keyboard_typing)
    }

    private fun sendTypedText(text: String) {
        val previous = typedText
        typedText = text
        val node = keyboardNode() ?: return
        if (typerNode != node) {
            typer?.close()
            typerNode = node
            typer = KeyboardTyper(node) {
                activity?.runOnUiThread {
                    _binding?.tvKeyboardStatus?.setText(R.string.tools_keyboard_failed)
                }
            }
        }
        typer?.send(KeyboardTest.reportsForEdit(previous, text))
    }

    private fun typeTestText() {
        val node = keyboardNode() ?: return
        binding.btnKeyboardType.isEnabled = false
        binding.tvKeyboardStatus.text = getString(R.string.tools_keyboard_countdown)

        // Leaves time to click into a text field on the host
        binding.root.postDelayed({
            if (_binding != null) {
                val reports = KeyboardTest.reportsFor(KeyboardTest.TEST_TEXT)
                runKeyboardScript(
                    KeyboardTest.typeScript(node, reports),
                    getString(R.string.tools_keyboard_sent, KeyboardTest.TEST_TEXT)
                )
            }
        }, TYPE_DELAY_MS)
    }

    private fun releaseKeys() {
        val node = keyboardNode() ?: return
        runKeyboardScript(
            KeyboardTest.releaseScript(node),
            getString(R.string.tools_keyboard_released)
        )
    }

    private fun runKeyboardScript(script: String, successMessage: String) {
        shellApi.exec(script) { result ->
            val current = _binding ?: return@exec
            current.btnKeyboardType.isEnabled = keyboardNode() != null
            current.tvKeyboardStatus.text =
                if (result.output.orEmpty().contains(KeyboardTest.RESULT_OK)) {
                    successMessage
                } else {
                    getString(R.string.tools_keyboard_failed)
                }
        }
    }

    private fun toggleWebAuthnTest() {
        if (toolsViewModel.webAuthnRunning.value == true) {
            toolsViewModel.stopWebAuthnTest()
        } else {
            ctapNode()?.let { toolsViewModel.startWebAuthnTest(it) }
        }
    }

    private companion object {
        const val TYPE_DELAY_MS = 3000L
    }
}
