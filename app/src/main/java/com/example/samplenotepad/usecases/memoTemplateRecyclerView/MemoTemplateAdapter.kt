package com.example.samplenotepad.usecases.memoTemplateRecyclerView

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.samplenotepad.R
import com.example.samplenotepad.entities.EditExistMemo
import com.example.samplenotepad.usecases.clearAll
import com.example.samplenotepad.usecases.initMemoContentsOperation
import com.example.samplenotepad.viewModels.MemoEditViewModel
import com.example.samplenotepad.views.main.MemoEditFragment
import com.example.samplenotepad.views.main.TemplateListDialogFragment
import com.example.samplenotepad.views.main.getFocusAndShowSoftwareKeyboard
import kotlinx.android.synthetic.main.fragment_memo_edit.*
import kotlinx.android.synthetic.main.memo_template_list_row.view.*


class MemoTemplateAdapter(
    private val editFragment: MemoEditFragment,
    private val editViewModel: MemoEditViewModel
    ) : RecyclerView.Adapter<MemoTemplateAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val templateNameView: TextView = view.memoTemplateNameTextView
            val templateNumberView: TextView = view.templateNumberTextView
        }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.memo_template_list_row, parent, false)
            val viewHolder = ViewHolder(view)

            viewHolder.itemView.setOnClickListener {
                clearAll()
                editViewModel.loadTemplateAndUpdateMemoContents(
                    editFragment, viewHolder.templateNameView.text.toString()
                )

                TemplateListDialogFragment.getInstanceOrCreateNewOne().dismiss()

                initMemoContentsOperation(
                    editFragment,
                    editViewModel,
                    editFragment.memoContentsContainerLayout,
                    EditExistMemo
                )
            }

            return viewHolder
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val templateName = editViewModel.getTemplateNameList()[position]

            holder.templateNumberView.text = String.format(
                editFragment.resources.getString(R.string.template_number_view_text), position + 1
            )
            holder.templateNameView.text = templateName
        }

        override fun getItemCount(): Int = editViewModel.getTemplateNameList().size
    }