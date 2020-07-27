package com.example.samplenotepad.usecases.searchMemoListRecyclerView

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.samplenotepad.R
import com.example.samplenotepad.viewModels.SearchViewModel
import kotlinx.android.synthetic.main.search_memo_list_row.view.*
import kotlinx.android.synthetic.main.search_memo_list_row.view.titleBodyTextView


class SearchMemoListAdapter(
    private val searchViewModel: SearchViewModel,
    private val clickedAction: (Long) -> Unit
) : RecyclerView.Adapter<SearchMemoListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val createdDate: TextView = view.createdDateTextView
        val title: TextView = view.titleBodyTextView
        val memoBody: TextView = view.memoBodyTextView
        val isSetRemainder: ImageView = view.reminderImgView
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.search_memo_list_row, parent, false)
        val viewHolder = ViewHolder(view)

        viewHolder.itemView.setOnClickListener {
            val selectedMemoInfoId =
                searchViewModel.getDataSetForMemoList()[viewHolder.adapterPosition].rowid

            clickedAction(selectedMemoInfoId)
        }

        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dataSetForMemoList = searchViewModel.getDataSetForMemoList()[position]

        holder.createdDate.text = dataSetForMemoList.createdDateTime.replace('-', '/')
        holder.title.text = dataSetForMemoList.title
        holder.memoBody.text = dataSetForMemoList.contentsForSearchByWord

        when (dataSetForMemoList.reminderDateTime.isEmpty()) {
            true -> holder.isSetRemainder.visibility = View.GONE
            false -> holder.isSetRemainder.visibility = View.VISIBLE
        }
    }

    override fun getItemCount(): Int = searchViewModel.getDataSetForMemoList().size
}
