package com.colvir.mbul.ui.mainScreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.constraintlayout.motion.widget.MotionLayout
import com.colvir.core.extensions.enableStateSaving
import com.colvir.mbul.AppColors
import com.colvir.mbul.constructor.Constructor
import com.colvir.mbul.databinding.FragmentMainScreenBinding
import com.colvir.mbul.ui.AbstractFragment
import com.colvir.shared.ui.mainScreen.MainScreenViewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel

class MainScreenFragment : AbstractFragment<MainScreenViewModel, FragmentMainScreenBinding>(FragmentMainScreenBinding::class.java) {

    override val viewModel: MainScreenViewModel get() = getViewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val mainView = super.onCreateView(inflater, container, savedInstanceState)!!

        viewModel.appBarControl.titleLive.observe(viewLifecycleOwner) {
            binding.mainTitle.text = it
        }
        viewModel.appBarControl.secondTitleLive.observe(viewLifecycleOwner) {
            binding.secondTitle.text = it
        }

        // Header
        val adsView = Constructor.buildControl(this, viewModel.ads)
        binding.headerBody.addView(adsView, MATCH_PARENT, WRAP_CONTENT)

        // Body
        val mainMenuView = Constructor.buildControl(this, viewModel.mainMenuTree)
        binding.body.addView(mainMenuView, MATCH_PARENT, MATCH_PARENT)

        val motionLayout = mainView as MotionLayout
        motionLayout.enableStateSaving(this)

        return mainView
    }
}

