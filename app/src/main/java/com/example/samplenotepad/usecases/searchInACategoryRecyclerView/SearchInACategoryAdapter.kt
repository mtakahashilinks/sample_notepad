package com.example.samplenotepad.usecases.searchInACategoryRecyclerView

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import com.example.samplenotepad.R
import com.example.samplenotepad.entities.MemoRowInfo
import com.example.samplenotepad.entities.SetMemoContents
import com.example.samplenotepad.usecases.getMemoContentsExecuteActor
import com.example.samplenotepad.viewModels.SearchViewModel
import kotlinx.android.synthetic.main.search_in_a_category_list_row.view.*
import kotlinx.android.synthetic.main.search_in_a_category_list_row.view.titleBodyTextView
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json


class SearchInACategoryAdapter(
    private val searchViewModel: SearchViewModel,
    private val clickedAction: () -> Unit
) : RecyclerView.Adapter<SearchInACategoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val createdDate: TextView = view.createdDateTextView
        val title: TextView = view.titleBodyTextView
        val memoBody: TextView = view.memoBodyTextView
        val isSetRemainder: ImageView = view.reminderImgView
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.search_in_a_category_list_row, parent, false)
        val viewHolder = ViewHolder(view)

        viewHolder.itemView.setOnClickListener {
            val memoInfoId =
                searchViewModel.getDataSetForMemoList()[viewHolder.adapterPosition].memoInfoId
            val memoInfo = searchViewModel.loadMemoInfoAndUpdateInViewModel(memoInfoId)
            val memoContents = Json.parse(MemoRowInfo.serializer().list, memoInfo.contents)
            Log.d("場所:SearchEachMemoListAdapter", "memoId=${memoInfo.rowid} memoContents=${memoContents}")

            searchViewModel.apply {
                viewModelScope.launch {
                    getMemoContentsExecuteActor().send(SetMemoContents(memoContents))
                    updateSavePointOfMemoContents()
                }
            }

            clickedAction()
        }

        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dataSetForMemoList = searchViewModel.getDataSetForMemoList()[position]

        holder.createdDate.text = dataSetForMemoList.createdDate.replace('-', '/')
        holder.title.text = dataSetForMemoList.memoTitle
        holder.memoBody.text = dataSetForMemoList.memoText

        when (dataSetForMemoList.reminderDateTime.isEmpty()) {
            true -> holder.isSetRemainder.visibility = View.GONE
            false -> holder.isSetRemainder.visibility = View.VISIBLE
        }
    }

    override fun getItemCount(): Int = searchViewModel.getDataSetForMemoList().size
}
