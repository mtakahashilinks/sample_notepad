package com.example.samplenotepad.views.display

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.ViewModelProvider
import com.example.samplenotepad.R
import com.example.samplenotepad.data.AppDatabase
import com.example.samplenotepad.entities.ConstValForMemo
import com.example.samplenotepad.viewModels.MemoDisplayViewModel
import com.example.samplenotepad.viewModels.SearchViewModel
import com.example.samplenotepad.views.main.MainActivity
import com.example.samplenotepad.views.search.MemoSearchActivity
import kotlinx.android.synthetic.main.activity_memo_display.*

class MemoDisplayActivity : AppCompatActivity() {

    companion object {
        lateinit var instanceOfActivity: Activity private set
        lateinit var displayViewModel: MemoDisplayViewModel private set
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_memo_display)
        setSupportActionBar(displayToolbar)

        instanceOfActivity = this

        displayViewModel = ViewModelProvider(this).get(MemoDisplayViewModel::class.java)

        displayViewModel.createNewMemoContentsExecuteActor()

        if (intent != null) {
            displayViewModel.loadMemoInfoAndUpdate(
                intent.getLongExtra(ConstValForMemo.MEMO_Id, -1)
            )

            supportFragmentManager.beginTransaction()
                .replace(R.id.displayContainer, MemoDisplayFragment.getInstanceOrCreateNew())
                .commit()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        viewModelStore.clear()

        AppDatabase.apply {
            getDatabase(this@MemoDisplayActivity).close()
            clearDBInstanceFlag()
        }
    }


    override fun onBackPressed() {
        super.onBackPressed()
    }


    //オプションメニューを作成
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.search_and_display_appbar_menu, menu)
        return true
    }

    //オプションメニューのItemタップ時の処理
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.createNewMemo -> {
                val intent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                startActivity(intent)
                viewModelStore.clear()
                finish()

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

//val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
//    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
//    putExtra(Settings.EXTRA_CHANNEL_ID, myNotificationChannel.getId())
//}
//startActivity(intent)
