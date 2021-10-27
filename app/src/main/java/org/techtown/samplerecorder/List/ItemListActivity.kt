package org.techtown.samplerecorder.List

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import org.techtown.samplerecorder.MainActivity.Companion.filePath
import org.techtown.samplerecorder.R
import org.techtown.samplerecorder.databinding.ActivityListBinding
import java.io.File

class ItemListActivity : AppCompatActivity() {

    private val TAG = this.javaClass.simpleName
    private lateinit var binding: ActivityListBinding

    private var fileList: MutableList<File> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_list)

        initUi()
        initFiles()
        initState()
    }

    private fun initUi() {
        val toolbar = binding.toolbarList
        setSupportActionBar(toolbar)
        supportActionBar?.setTitle("목록")
    }

    private fun initFiles() {
        val files = File(filePath).listFiles()
        for (i in files.indices) {
            if (!files[i].isHidden && files[i].isFile) {
                fileList.add(files[i])
            }
        }
        fileList.sort() // 우선 이름 순서대로 출력
    }

    private fun initState() {
        binding.listRecyclerview.layoutManager = LinearLayoutManager(this)
        binding.listRecyclerview.adapter = ItemListAdapter(this, fileList)
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
}