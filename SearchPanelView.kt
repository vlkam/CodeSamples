package com.colvir.core.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.constraintlayout.widget.ConstraintLayout
import com.colvir.core.android.R
import com.colvir.core.android.databinding.ViewSearchPanelBinding
import com.colvir.core.extensions.embed
import com.colvir.core.extensions.hideKeyboard

class SearchPanelView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null) :
        ConstraintLayout(context, attrs) {

    val clearSearchButton: ImageView get() = binding.clearSearchButton
    val searchField: AppCompatEditText get() = binding.searchField
    val closeButton: ImageView get() = binding.closeSearchButton
    val foundText: TextView get() = binding.foundText

    var description
        get() = binding.foundText.text.toString()
        set(value) {
            binding.foundText.text = value
        }

    var hint: String?
        get() = searchField.hint?.toString()
        set(value) {
            searchField.hint = value
        }

    private var binding_: ViewSearchPanelBinding? = null
    protected val binding: ViewSearchPanelBinding
        get() = binding_!!

    init {
        binding_ = ViewSearchPanelBinding.inflate(LayoutInflater.from(context), this)
        //setBackgroundColor(AppColors.PRIMARY_COLOR)
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {

        val attributes = context.obtainStyledAttributes(attrs, R.styleable.SearchPanel)
        try {
            hint = attributes.getString(R.styleable.SearchPanel_search_panel_hint)
        } finally {
            attributes.recycle()
        }

        binding.searchField.setOnEditorActionListener { v, actionId, event ->
            binding.searchField.clearFocus()
            hideKeyboard()
            true
        }
    }

    override fun onViewAdded(view: View?) {
        super.onViewAdded(view)

        if (view?.tag == "content") {
            binding.content.embed(view)
        }

    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        binding_ = null
    }

}