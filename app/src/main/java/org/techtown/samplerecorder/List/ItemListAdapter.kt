package org.techtown.samplerecorder.List

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.techtown.samplerecorder.databinding.ItemBinding
import java.io.File


class ItemListAdapter(var context: Context, private val fileList: MutableList<File>) : RecyclerView.Adapter<ItemListViewHolder>() {

    private val TAG = this.javaClass.simpleName

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemListViewHolder {
        val binding = ItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemListViewHolder(binding, this)
    }

    override fun onBindViewHolder(holder: ItemListViewHolder, position: Int) {
        val file = fileList[position]
        holder.setData(fileList, file, position)
    }

    override fun getItemCount() = fileList.size

    // 어댑터에서 업데이트되는 position 항목만 호출하여 payload를 받아 View를 업데이트
    override fun onBindViewHolder(holder: ItemListViewHolder, position: Int, payloads: List<Any?>) {
        super.onBindViewHolder(holder, position, payloads)
        holder.seekBar.visibility = View.GONE
    }
}