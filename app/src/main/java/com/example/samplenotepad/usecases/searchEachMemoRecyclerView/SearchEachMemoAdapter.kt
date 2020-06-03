package com.example.samplenotepad.usecases.searchEachMemoRecyclerView

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.samplenotepad.R
import com.example.samplenotepad.data.deserializeMemoContents
import com.example.samplenotepad.data.loadMemoInfoFromDatabase
import com.example.samplenotepad.viewModels.SearchViewModel
import com.example.samplenotepad.views.search.SearchEachMemoFragment
import kotlinx.android.synthetic.main.search_each_memo_list_row.view.*
import kotlinx.android.synthetic.main.search_each_memo_list_row.view.titleBodyTextView
import java.text.SimpleDateFormat
import java.util.*


class SearchEachMemoListAdapter(
    private val fragment: SearchEachMemoFragment,
    private val searchViewModel: SearchViewModel
) : RecyclerView.Adapter<SearchEachMemoListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val createdDate: TextView = view.createdDateTextView
        val title: TextView = view.titleBodyTextView
        val memoBody: TextView = view.memoBodyTextView
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.search_each_memo_list_row, parent, false)
        val viewHolder = ViewHolder(view)

        viewHolder.itemView.setOnClickListener {
            val memoInfoId =
                searchViewModel.getDataSetForEachMemoList()[viewHolder.adapterPosition].memoInfoId
            val memoInfo = loadMemoInfoFromDatabase(fragment, memoInfoId)
            val memoContents = deserializeMemoContents(memoInfo.contents)

            searchViewModel.apply {
                updateMemoInfo { memoInfo }
                updateMemoContents { memoContents }
                updateMemoContentsAtSavePoint()
            }
            fragment.moveToDisplayMemo()
        }

        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val createdDateFormatter = SimpleDateFormat("yyyy/MM/dd", Locale("ja","JP","JP")).apply {
            timeZone = TimeZone.getTimeZone("Asia/Tokyo")
        }
        val memoBodyFormatter = "%s"
        val eachMemoDataSet = searchViewModel.getDataSetForEachMemoList()[position]

        holder.createdDate.text = createdDateFormatter.format(eachMemoDataSet.createdDate)
        holder.title.text = eachMemoDataSet.memoTitle
        holder.memoBody.text = memoBodyFormatter.format(eachMemoDataSet.memoText)

    }

    override fun getItemCount(): Int = searchViewModel.getDataSetForEachMemoList().size
}