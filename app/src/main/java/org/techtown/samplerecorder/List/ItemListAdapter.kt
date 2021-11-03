package org.techtown.samplerecorder.List

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.techtown.samplerecorder.Database.RoomItem
import org.techtown.samplerecorder.MainActivity.Companion.itemList
import org.techtown.samplerecorder.databinding.ItemListBinding

class ItemListAdapter(private val itemList: MutableList<RoomItem>) : RecyclerView.Adapter<ItemListViewHolder>() {

    private val TAG = this.javaClass.simpleName

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemListViewHolder {
        val binding = ItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemListViewHolder(binding, this)
    }

    override fun onBindViewHolder(holder: ItemListViewHolder, position: Int) {
        val item = itemList[position]
        holder.setData(item, position)
    }

    override fun getItemCount() = itemList.size

    // 어댑터에서 업데이트되는 position 항목만 호출하여 payload를 받아 View를 업데이트
    override fun onBindViewHolder(holder: ItemListViewHolder, position: Int, payloads: List<Any?>) {
        super.onBindViewHolder(holder, position, payloads)
        holder.layout.visibility = View.GONE
    }
}