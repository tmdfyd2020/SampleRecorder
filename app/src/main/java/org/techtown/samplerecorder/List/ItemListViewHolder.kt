package org.techtown.samplerecorder.List

import android.R
import android.app.AlertDialog
import android.text.Html
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import org.techtown.samplerecorder.databinding.ItemBinding

class ItemListViewHolder (private val binding: ItemBinding) : RecyclerView.ViewHolder(binding.root) {

    val playButton = binding.btnItemPlay
    val fileName = binding.itemText
    val seekBar = binding.seekbarPlayState

//    init {
//        // if click itemView on LongClick, show delete dialog
//        itemView.setOnLongClickListener { v ->
//            val file = fileList[adapterPosition]
//            val builder = AlertDialog.Builder(
//                v.context,
//                R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar
//            )
//            builder.setTitle("Delete")
//                .setMessage("Are you sure you want to delete?")
//                .setIcon(listContext.getDrawable(R.drawable.png_delete))
//                .setPositiveButton(
//                    Html.fromHtml("<font color='#3399FF'>Yes</font>")
//                ) { dialog, which ->
//                    file.delete()
//                    fileList.removeAt(adapterPosition)
//                    notifyItemRemoved(adapterPosition)
//                    notifyItemRangeChanged(adapterPosition, fileList.size)
//                    Toast.makeText(listContext, "파일이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
//                }
//                .setNegativeButton(
//                    Html.fromHtml("<font color='#F06292'>No</font>")
//                ) { dialog, which -> dialog.cancel() }
//            val dialog = builder.create()
//            dialog.window!!.setGravity(Gravity.CENTER)
//            dialog.show()
//            false
//        }
//    }
}