package io.horizontalsystems.bankwallet.modules.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.fragment_slide_intro.*

class IntroSlideFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_slide_intro, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titleResId = requireArguments().getInt("title")
        val descriptionResId = requireArguments().getInt("description")

        title.text = getString(titleResId)
        description.text = getString(descriptionResId)
    }

    companion object {
        @JvmStatic
        fun newInstance(titleResId: Int, descriptionResId: Int) =
                IntroSlideFragment().apply {
                    arguments = Bundle(2).apply {
                        putInt("title", titleResId)
                        putInt("description", descriptionResId)
                    }
                }
    }
}
