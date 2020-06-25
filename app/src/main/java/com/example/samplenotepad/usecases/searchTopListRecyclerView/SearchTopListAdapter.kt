package com.example.samplenotepad.usecases.searchTopListRecyclerView

import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.samplenotepad.R
import com.example.samplenotepad.viewModels.SearchViewModel
import com.example.samplenotepad.views.search.SearchTopFragment
import kotlinx.android.synthetic.main.fragment_rename_dialog.view.*
import kotlinx.android.synthetic.main.search_top_container_row.view.*


class SearchTopListAdapter(
    private val fragment: SearchTopFragment,
    private val searchViewModel: SearchViewModel
) : RecyclerView.Adapter<SearchTopListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val categoryName: TextView = view.categoryNameTextView
        val categorySize: TextView = view.contentsCountTextView
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.search_top_container_row, parent, false)
        val viewHolder = ViewHolder(view)

        viewHolder.itemView.setOnClickListener {
            fragment.moveToSearchEachMemo(
                searchViewModel.getDataSetForCategoryList()[viewHolder.adapterPosition].name
            )
        }

        viewHolder.itemView.setOnLongClickListener { view ->
            when (viewHolder.adapterPosition) {
                0 -> false
                else -> {
                    val layoutView = fragment.requireActivity().layoutInflater.inflate(
                        R.layout.fragment_rename_dialog, null, false
                    ).apply {
                        newNameEitText.requestFocus()
                    }
                    val targetCategoryName =
                        searchViewModel.getDataSetForCategoryList()[viewHolder.adapterPosition].name

                    layoutView.nameBeforeChangeTextView.text = String.format(
                        fragment.resources.getString(
                            R.string.dialog_rename_name_before_change
                        ), targetCategoryName
                    )

                    AlertDialog.Builder(fragment.requireContext()).apply {
                        setTitle(R.string.dialog_rename_category_title)
                        setView(layoutView)
                        setPositiveButton(R.string.dialog_rename_positive_button, null)
                        setNegativeButton(R.string.dialog_rename_negative_button) { dialog, id ->
                            dialog.cancel()
                        }
                    }.show()
                        .setPositiveButtonAction(layoutView, targetCategoryName)

                    true
                }
            }
        }

        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sizeFormatter = "(%d件)"
        val categoryName = searchViewModel.getDataSetForCategoryList()[position].name
        val categorySize = searchViewModel.getDataSetForCategoryList()[position].listSize

        holder.categoryName.text = categoryName
        holder.categorySize.text = sizeFormatter.format(categorySize)
    }

    override fun getItemCount(): Int = searchViewModel.getDataSetForCategoryList().size


    private fun AlertDialog.setPositiveButtonAction(
        layoutView: View,
        targetCategoryName: String
    ) =
        this.getButton(DialogInterface.BUTTON_POSITIVE).apply {
            setOnClickListener {
                val newCategoryName = layoutView.newNameEitText.text.toString()
                val errorTextView = layoutView.errorMassageTextView

                when {
                    newCategoryName.isEmpty() -> {
                        errorTextView.showErrorText(R.string.error_not_input_new_name)
                    }
                    searchViewModel.getDataSetForCategoryList().any { it.name == newCategoryName } -> {
                        errorTextView.showErrorText(R.string.error_already_has_same_name)
                    }
                    else -> {
                        searchViewModel.renameItemInDataSetForCategoryListAndUpdateDatabase(
                            targetCategoryName, newCategoryName
                        )

                        this@SearchTopListAdapter.notifyDataSetChanged()
                        this@setPositiveButtonAction.cancel()
                    }
                }
            }
        }

    private fun TextView.showErrorText(massageId: Int) =
        this.apply {
            visibility = View.VISIBLE
            setText(massageId)
        }
}
