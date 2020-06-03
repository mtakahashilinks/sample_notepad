package com.example.samplenotepad.views

import arrow.core.internal.AtomicRefW
import com.example.samplenotepad.views.main.MemoEditFragment


object FragmentFactories {

    private val EDIT_FRAGMENT: AtomicRefW<MemoEditFragment?> = AtomicRefW(null)

    internal fun getInputFragment(): MemoEditFragment {
        val fragment = EDIT_FRAGMENT.updateAndGet { mFragment ->
            when (mFragment == null) {
                true -> MemoEditFragment()
                false -> mFragment
            }
        }

        return fragment ?: MemoEditFragment()
    }
}