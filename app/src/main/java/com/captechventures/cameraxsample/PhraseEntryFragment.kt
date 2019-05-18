package com.captechventures.cameraxsample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.transaction
import kotlinx.android.synthetic.main.fragment_phrase_entry.*

class PhraseEntryFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance() = PhraseEntryFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_phrase_entry, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        phraseEditText.doAfterTextChanged { text ->
            enterButton.isEnabled = text.toString().isNotEmpty()
        }

        phraseEditText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE && enterButton.isEnabled) {
                enterButton.callOnClick()
                return@setOnEditorActionListener true
            }
            false
        }

        enterButton.setOnClickListener {
            requireFragmentManager().transaction {
                replace(R.id.fragmentContainer, CameraFragment.newInstance(phraseEditText.text.trim().toString()))
                addToBackStack(null)
            }
        }
    }
}