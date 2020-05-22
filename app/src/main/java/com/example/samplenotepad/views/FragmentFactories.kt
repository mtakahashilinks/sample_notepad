package com.example.samplenotepad.views

import arrow.core.internal.AtomicRefW
import com.example.samplenotepad.views.main.MemoInputFragment


object FragmentFactories {

    private val inputFragment: AtomicRefW<MemoInputFragment?> = AtomicRefW(null)

    internal fun getInputFragment(): MemoInputFragment {
        val fragment = inputFragment.updateAndGet { mFragment ->
            when (mFragment == null) {
                true -> MemoInputFragment()
                false -> mFragment
            }
        }

        return fragment ?: MemoInputFragment()
    }
}