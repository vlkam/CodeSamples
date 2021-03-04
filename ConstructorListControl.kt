package com.colvir.mbul.constructor.views

import SharedConstants.Companion.NO_COLOR
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import constructor.uiModels.ListControl
import com.colvir.core.extensions.getPrivateFieldValue
import com.colvir.core.extensions.inflateLayout
import com.colvir.core.extensions.toDP
import com.colvir.core.ui.ExpandableAdapter
import com.colvir.core.ui.ExpandableViewHolder
import com.colvir.core.ui.LinearLayoutManagerExt
import com.colvir.core.ui.layouts.RepeaterView
import com.colvir.core.views.CoreFragment
import com.colvir.mbul.R
import com.colvir.mbul.constructor.swipe.RecyclerViewSwiper
import com.colvir.mbul.constructor.swipe.SwipeButton
import com.colvir.mbul.databinding.ControlListBinding
import com.colvir.mbul.databinding.ControlRowBalanceBinding
import com.colvir.mbul.ui.info.currencies.CurrencyExchangeModel
import com.colvir.shared.extensions.loadImage
import constructor.CommonExpandableSection
import constructor.CommonRowModel
import constructor.SwipeGesture
import extensions.currentDateTime
import infrastructure.Disposable
import ui.CoreViewModel
import ui.auxiliary.ProgressMode
import ui.views.ExpandableSectionItem

object ConstructorListControl {

    fun build(fragment: CoreFragment<out CoreViewModel, out ViewBinding>, layoutInflater: LayoutInflater, controlSet: ListControl): View {

        val binding = ControlListBinding.inflate(layoutInflater)

        val adapter = CommonListAdapter(binding.recyclerView, controlSet)
        adapter.onTapAction = controlSet.onTapAction
        val layoutManager = LinearLayoutManagerExt(controlSet.name, fragment, adapter)

        // Paging library support
        adapter.pagination = controlSet.pagination

        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = adapter

        // Divider
        if (controlSet.addDivider) {
            val dividerItemDecoration = ItemDivider(fragment.requireContext(), layoutManager.orientation)
            binding.recyclerView.addItemDecoration(dividerItemDecoration)
        }

        fun setupNoItemsMessage() {
            val isThereALoading = fragment.viewModel.showProgressLive.value != ProgressMode.NONE || controlSet.showProgressLive.value != ProgressMode.NONE
            if (isThereALoading) {
                binding.recyclerViewIsEmptyText.isVisible = false
                return
            }
            val itemCollection = controlSet.hierarchyTreeLive.value
            binding.recyclerViewIsEmptyText.isVisible = itemCollection != null && itemCollection.children.isEmpty()
        }

        controlSet.hierarchyTreeLive.observe(fragment.viewLifecycleOwner) {
            setupNoItemsMessage()
            adapter.submitList(it?.children)
        }

        fragment.viewModel.showProgressLive.observe(fragment.viewLifecycleOwner) {
            setupNoItemsMessage()
        }
        controlSet.showProgressLive.observe(fragment.viewLifecycleOwner) {
            setupNoItemsMessage()
        }

        if (!controlSet.noItemsMessage.isNullOrEmpty()) {
            binding.recyclerViewIsEmptyText.text = controlSet.noItemsMessage
        }

        // Swipe to refresh
        if (controlSet.addSwipeToRefresh) {
            binding.swipeContainer.setOnRefreshListener {

                // It cancels first swipe to refresh call
                val currentMillis = currentDateTime().toEpochMilliseconds()
                val delta = currentMillis - binding.swipeContainer.lastSwipeTime
                if (delta < binding.swipeContainer.delayBetweenTwoRefreshForTwoSwipesInMillis) {
                    //Log.i("@@@", "onNestedScroll started curr $currentMillis delta $delta")
                    binding.swipeContainer.lastSwipeTime = 0
                    fragment.viewModel.updateData(pullToRefresh = true)
                } else {
                    //Log.i("@@@", "onNestedScroll skipped curr $currentMillis delta $delta")
                    binding.swipeContainer.lastSwipeTime = currentMillis
                    binding.swipeContainer.isRefreshing = false
                }
            }
            controlSet.isLoadingLive.observe(fragment.viewLifecycleOwner) {
                if (!it) {
                    binding.swipeContainer.isRefreshing = false
                }
            }
        }

        // Swipe gestures
        addSwiper(adapter, fragment, binding.recyclerView)

        // Scroll to item
        controlSet.scrollToItemLive.observe(fragment.viewLifecycleOwner) {
            it ?: return@observe

            val poz = adapter.currentflatList?.indexOf(it) ?: -1
            if (poz > -1) {
                val smoothScroller = object : LinearSmoothScroller(fragment.context) {
                    override fun getVerticalSnapPreference(): Int {
                        return LinearSmoothScroller.SNAP_TO_START
                    }
                }
                smoothScroller.targetPosition = poz
                layoutManager.startSmoothScroll(smoothScroller)
            }
            controlSet.scrollToItemLive.value = null
        }

        // the result view
        val resultView = if (controlSet.addSwipeToRefresh) {
            binding.root
        } else {
            val frame = binding.frame
            binding.root.removeView(frame)
            frame
        }

        // Progress
        val progressBar = binding.progressBarBinding.progressBar
        val layoutParams = progressBar.layoutParams as ViewGroup.LayoutParams
        layoutParams.height = layoutParams.height / 3 * 2
        layoutParams.width = layoutParams.width / 3 * 2
        controlSet.showProgressLive.observe(fragment.viewLifecycleOwner) {
            when (it) {
                ProgressMode.SHOW ->
                    progressBar.isVisible = true
                else ->
                    progressBar.isVisible = false
            }
        }

        // Clear resources
        fragment.compositeDisposable += object : Disposable {
            override fun dispose() {
                controlSet.onBindToModelPlatform = null
                controlSet.onCreateViewHolderPlatform = null
                controlSet.adapterPlatform = null
                controlSet.onSubmitListPlatform = null
            }
        }

        controlSet.adapterPlatform = adapter
        return resultView
    }

    private fun addSwiper(adapter: CommonListAdapter, fragment: CoreFragment<out CoreViewModel, out ViewBinding>, recyclerView: RecyclerView) {

        val swiper: RecyclerViewSwiper = object : RecyclerViewSwiper(fragment.context, recyclerView) {

            private fun addButton(swipeGesture: SwipeGesture, swipeButtons: MutableList<SwipeButton>, viewHolder: RecyclerView.ViewHolder, model: CommonRowModel) {
                swipeButtons.add(SwipeButton(
                        context = fragment.requireContext(),
                        swipeGesture = swipeGesture,
                        clickListener = {
                            swipeGesture.action.invoke(model)
                            adapter.notifyDataSetChanged()
                        }))
            }

            override fun initSwipeButtonRight(viewHolder: RecyclerView.ViewHolder, swipeButtons: MutableList<SwipeButton>) {

                val commonViewHolder = viewHolder as CommonViewHolder
                val model = commonViewHolder.model as CommonRowModel

                model.swipeGestures
                        ?.filter { it.side == SwipeGesture.Side.RIGHT }
                        ?.forEach {
                            addButton(it, swipeButtons, viewHolder, model)
                        }
            }

            override fun initSwipeButtonLeft(viewHolder: RecyclerView.ViewHolder, swipeButtons: MutableList<SwipeButton>) {

                val commonViewHolder = viewHolder as CommonViewHolder
                val model = commonViewHolder.model as CommonRowModel

                model.swipeGestures
                        ?.filter { it.side == SwipeGesture.Side.LEFT }
                        ?.forEach {
                            addButton(it, swipeButtons, viewHolder, model)
                        }
            }

        }
    }

}
