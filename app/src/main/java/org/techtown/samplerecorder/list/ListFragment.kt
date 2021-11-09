package org.techtown.samplerecorder.list

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import org.techtown.samplerecorder.MainActivity.Companion.itemList
import org.techtown.samplerecorder.R
import org.techtown.samplerecorder.databinding.FragmentListBinding

class ListFragment : Fragment() {

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUi()
        initState()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initUi() {
        BUTTON_PLAY = getDrawable(requireContext(), R.drawable.ic_list_play)!!
        BUTTON_PAUSE = getDrawable(requireContext(), R.drawable.ic_list_pause)!!
    }

    private fun initState() {
        binding.listRecyclerview.layoutManager = LinearLayoutManager(activity)
        binding.listRecyclerview.adapter = ListAdapter(itemList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        lateinit var BUTTON_PLAY: Drawable
        lateinit var BUTTON_PAUSE: Drawable

        fun instance() = ListFragment()
    }
}