package com.example.samplenotepad

//import android.os.Bundle
//import androidx.fragment.app.Fragment
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.lifecycle.ViewModelProvider
//import com.example.samplenotepad.databinding.MainFragmentBinding
//import kotlinx.android.synthetic.main.fragment_memo_option.*
//
//
//class MainFragment : Fragment() {
//
//    private lateinit var myDataBinding: MainFragmentBinding
//    private lateinit var myViewModel: MainViewModel
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        if (activity != null) {
//            myViewModel = activity.run {
//                ViewModelProvider.NewInstanceFactory().create(MainViewModel::class.java)
//            }
//        }
//    }
//
//    override fun onCreateView(inflater: LayoutInflater,
//                              container: ViewGroup?,
//                              savedInstanceState: Bundle?): View? {
//        //DataBindingを適用
//        val myDataBinding = MainFragmentBinding.inflate(inflater, container, false)
//        myDataBinding.lifecycleOwner = this
//        myDataBinding.viewModel = myViewModel
//
//        return myDataBinding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        //メモ内容EditTextの文字数カウンターのセット
//        setCounterText(memoContentsEditTextView, memoContentsCounterTextView, 1000)
//
//        //DateTextのテキストの初期化とイベントリスナー登録
//        myViewModel.initMemoDate()
//        memoDateTextView.setOnClickListener { showDatePickerDialog(parentFragmentManager) }
//        memoDatePickerImgBtn.setOnClickListener { showDatePickerDialog(parentFragmentManager) }
//    }
//
//    override fun onActivityCreated(savedInstanceState: Bundle?) {
//        super.onActivityCreated(savedInstanceState)
//
//        //ツールバーのタイトルをセット
//        activity?.title = getString(R.string.input_new_memo)
//    }
//}
//