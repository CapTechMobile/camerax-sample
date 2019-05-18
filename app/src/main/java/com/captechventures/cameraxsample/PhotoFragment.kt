package com.captechventures.cameraxsample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.fragment_photo.*

class PhotoFragment : Fragment() {

    companion object {
        private const val FILE_ARG = "file_arg"

        @JvmStatic
        fun newInstance(fileLocation: String): PhotoFragment {
            return PhotoFragment().apply {
                arguments = Bundle().apply {
                    putString(FILE_ARG, fileLocation)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_photo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        backButton.setOnClickListener { requireFragmentManager().popBackStack() }

        val fileLocation = requireArguments().getString(FILE_ARG)
        Glide.with(this).load(fileLocation).fitCenter().into(image)

        if (savedInstanceState == null) {
            Toast.makeText(requireContext(), "Phrase found, image captured!", Toast.LENGTH_SHORT).show()
        }
    }
}