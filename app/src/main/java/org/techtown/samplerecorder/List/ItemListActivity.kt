package org.techtown.samplerecorder.List

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import org.techtown.samplerecorder.MainActivity.Companion.itemList
import org.techtown.samplerecorder.R
import org.techtown.samplerecorder.databinding.ActivityListBinding

class ItemListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_list)

        initUi()
        initState()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initUi() {
        val toolbar = binding.toolbarList
        setSupportActionBar(toolbar)
        supportActionBar?.title = "목록"
        BUTTON_PLAY = getDrawable(R.drawable.ic_list_play)!!
        BUTTON_PAUSE = getDrawable(R.drawable.ic_list_pause)!!
    }

    private fun initState() {
        binding.listRecyclerview.layoutManager = LinearLayoutManager(this)
        binding.listRecyclerview.adapter = ItemListAdapter(itemList)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_list_toolbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.list_back -> finish()
        }
        return true
    }

    companion object {
        private const val TAG = "ItemListActivity"
        lateinit var BUTTON_PLAY: Drawable
        lateinit var BUTTON_PAUSE: Drawable
    }
}