package com.example.samplenotepad.usecases.searchTopListRecyclerView

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.samplenotepad.R
import com.example.samplenotepad.viewModels.SearchViewModel
import com.example.samplenotepad.views.search.SearchTopFragment
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
        val viewHolder =
            ViewHolder(
                view
            )

        viewHolder.itemView.setOnClickListener {
            fragment.moveToSearchEachMemo(
                searchViewModel.getDataSetForCategoryList()[viewHolder.adapterPosition].name
            )
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
}