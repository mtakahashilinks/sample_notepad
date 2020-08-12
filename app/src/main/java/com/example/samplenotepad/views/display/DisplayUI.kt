package com.example.samplenotepad.views.display

import com.example.samplenotepad.R
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_display_memo.*


internal fun MemoDisplayFragment.showSnackbarForSavedMassageAtDisplayMemo() {
    this.displaySaveImgBtn?.let { Snackbar.make(it, R.string.save_snackbar, Snackbar.LENGTH_SHORT).apply {
        view.alpha = 0.5f
        show()
    } }
}
