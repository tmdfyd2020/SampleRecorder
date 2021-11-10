package org.techtown.samplerecorder.list

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import org.techtown.samplerecorder.activity.MainActivity.Companion.itemList
import org.techtown.samplerecorder.R
import org.techtown.samplerecorder.databinding.FragmentListBinding
import org.techtown.samplerecorder.util.DialogService

class ListFragment : Fragment(), View.OnClickListener {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setOnClickListener()
        BUTTON_PLAY = getDrawable(requireContext(), R.drawable.ic_list_play)!!
        BUTTON_PAUSE = getDrawable(requireContext(), R.drawable.ic_list_pause)!!
        binding.listRecyclerview.layoutManager = LinearLayoutManager(activity)
        binding.listRecyclerview.adapter = ListAdapter(itemList)
    }

    private fun setOnClickListener() {
        binding.containerFragmentList.children.forEach { btn ->
            btn.setOnClickListener(this)
        }
    }

    override fun onClick(v: View?) {
//        childFragmentManager.let {
//            when (view.id) {
//            }
//        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "ListFragment"
        fun instance() = ListFragment()
        lateinit var BUTTON_PLAY: Drawable
        lateinit var BUTTON_PAUSE: Drawable
    }
}