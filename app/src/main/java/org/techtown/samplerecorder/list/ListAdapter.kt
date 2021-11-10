package org.techtown.samplerecorder.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.techtown.samplerecorder.database.RoomItem
import org.techtown.samplerecorder.databinding.ItemListBinding

class ListAdapter(private val itemList: MutableList<RoomItem>) : RecyclerView.Adapter<ListViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        val binding = ItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ListViewHolder(binding, this)
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        val item = itemList[position]
        holder.setData(item, position)
    }

    override fun getItemCount() = itemList.size

    // 어댑터에서 업데이트되는 position 항목만 호출하여 payload를 받아 View를 업데이트
    override fun onBindViewHolder(holder: ListViewHolder, position: Int, payloads: List<Any?>) {
        super.onBindViewHolder(holder, position, payloads)
        holder.layout.visibility = View.GONE
    }

    companion object {
        private const val TAG = "ListAdapter"
    }
}