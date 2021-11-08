package org.techtown.samplerecorder

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.SystemClock
import android.provider.Settings
import android.view.*
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.techtown.samplerecorder.Audio.Queue
import org.techtown.samplerecorder.Audio.RecordService
import org.techtown.samplerecorder.Audio.RecordService.Companion.CODE_FILE_NAME
import org.techtown.samplerecorder.Audio.RecordService.Companion.record
import org.techtown.samplerecorder.Audio.RecordService.Companion.recordWave
import org.techtown.samplerecorder.Audio.TrackService
import org.techtown.samplerecorder.Audio.TrackService.Companion.playWave
import org.techtown.samplerecorder.Database.RoomHelper
import org.techtown.samplerecorder.Database.RoomItem
import org.techtown.samplerecorder.Database.RoomItemDao
import org.techtown.samplerecorder.FileNameActivity.Companion.KEY_FILE_NAME
import org.techtown.samplerecorder.HomeFragment.Companion.bufferSize
import org.techtown.samplerecorder.HomeFragment.Companion.playRate
import org.techtown.samplerecorder.HomeFragment.Companion.volumeType
import org.techtown.samplerecorder.List.ItemListActivity
import org.techtown.samplerecorder.Util.DialogService
import org.techtown.samplerecorder.Util.DialogService.Companion.dialog
import org.techtown.samplerecorder.Util.LogUtil
import org.techtown.samplerecorder.Util.VolumeObserver
import org.techtown.samplerecorder.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var fragmentId: Int = MAIN

    private val container by lazy { binding.containerMain }
    private val button by lazy { binding.ivMainStartWindow }
    private val logWindow by lazy { binding.layoutMainLogWindow }
    private var logWindowX: Float = 0f
    private var logWindowY: Float = 0f
    private var shortAnimationDuration: Int = 0
    private var currentAnimator: Animator? = null
    private var zoomState: Boolean = false

    private lateinit var helper: RoomHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        changeFragment(fragmentId)

        helper = Room.databaseBuilder(this, RoomHelper::class.java, "room_items").build()
        itemDAO = helper.roomItemDao()
        syncDatabase()

        permissionCheck()
        initUi()
        initState()
        initListener()
    }

    private fun permissionCheck() {
        val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_CODE)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initUi() {
        val toolbar = binding.toolbarMain
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun initState() {
        filePath = filesDir.absolutePath

//        volumeControlStream = volumeType

        // Log window 첫 시작 위치 설정
        with (binding.containerMain) {
            viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    val containerWidth = width
                    with (binding.layoutMainLogWindow) {
                        class OnGlobalLayoutListener : ViewTreeObserver.OnGlobalLayoutListener {
                            override fun onGlobalLayout() {
                                logWindowX = ((containerWidth - width) / 2).toFloat()
                                logWindowY = (height / 2).toFloat()
                                viewTreeObserver.removeOnGlobalLayoutListener(this)
                            }
                        }
                        viewTreeObserver.addOnGlobalLayoutListener(OnGlobalLayoutListener())
                    }
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            })
        }

        shortAnimationDuration = resources.getInteger(android.R.integer.config_shortAnimTime)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initListener() {
        var moveX = 0f
        var moveY = 0f
        binding.layoutMainLogWindow.setOnTouchListener { view: View, event: MotionEvent ->
            when(event.action) {
                MotionEvent.ACTION_DOWN -> {
                    moveX = view.x - event.rawX
                    moveY = view.y - event.rawY
                }

                MotionEvent.ACTION_MOVE -> {
                    view.animate()
                        .x(event.rawX + moveX)
                        .y(event.rawY + moveY)
                        .setDuration(0)
                        .start()
                    logWindowX = view.x
                    logWindowY = view.y
                }
            }
            true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main_toolbar, menu)
        return true
    }

    @SuppressLint("NonConstantResourceId", "UseCompatLoadingForDrawables")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_exit -> {
                dialog(this).create(getString(R.string.exit), "")
            }
            R.id.list_play -> {
                if (fragmentId == MAIN) {
                    item.icon = getDrawable(R.drawable.ic_list_toolbar_back)
                    changeFragment(LIST)
                } else {
                    item.icon = getDrawable(R.drawable.ic_main_toolbar_list)
                    changeFragment(MAIN)
                }
            }
        }
        return true
    }

    fun onClick(view: View) {
        when (view.id) {
            R.id.iv_main_start_window -> {
                zoomAnimation(view)
            }

            R.id.iv_main_minimize_popup -> {
                with (binding) {
                    layoutMainLogWindowBody.visibility = View.GONE
                    ivMainMinimizePopup.visibility = View.GONE
                    ivMainMaximizePopup.visibility = View.VISIBLE
                }
            }

            R.id.iv_main_maximize_popup -> {
                with (binding) {
                    layoutMainLogWindowBody.visibility = View.VISIBLE
                    ivMainMinimizePopup.visibility = View.VISIBLE
                    ivMainMaximizePopup.visibility = View.GONE
                }
            }

            R.id.iv_main_close_popup -> {
                zoomAnimation(button)
            }
        }
    }

    fun insertItem(item: RoomItem) {
        CoroutineScope(Dispatchers.IO).launch {
            itemDAO.insert(item)
            syncDatabase()
        }
    }

    private fun syncDatabase() {
        CoroutineScope(Dispatchers.IO).launch {
            itemList.clear()
            itemList.addAll(itemDAO.getList())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                CODE_FILE_NAME -> {
                    val name = data?.getStringExtra(KEY_FILE_NAME)
                    record(this).addItem(name!!, this)
                }
            }
        } else {
            record(this).removeFile()
        }
    }

    fun writeLogWindow(msg: String) {
        val totalMsg = "$msg\n${binding.tvMainLogWindow.text}"
        binding.tvMainLogWindow.text = totalMsg
    }

    /**
     * View Expansion and Shrink animation.
     * Start from button x, y to current window's x, y
     * Up to window's original scale
     * @param button start animation view
     */
    private fun zoomAnimation(button: View) {
        currentAnimator?.cancel()

        val startBoundsInt = Rect()
        val finalBoundsInt = Rect()
        val globalOffset = Point()

        button.getGlobalVisibleRect(startBoundsInt)
        container.getGlobalVisibleRect(finalBoundsInt, globalOffset)
        startBoundsInt.offset(-globalOffset.x, -globalOffset.y)
        finalBoundsInt.offset(-globalOffset.x, -globalOffset.y)

        val startBounds = RectF(startBoundsInt)
        val finalBounds = RectF(finalBoundsInt)
        val startScale = startBounds.height() / finalBounds.height()

        logWindow.pivotX = 0f
        logWindow.pivotY = 0f

        when (zoomState) {
            false -> {
                LogUtil.i(TAG, "Animation Open")
                with (logWindow) {
                    visibility = View.VISIBLE
                    bringToFront()
                }
                currentAnimator = AnimatorSet().apply {
                    play(ObjectAnimator.ofFloat(logWindow, View.X, button.x, logWindowX)).apply {  // X 시작 위치, 마지막 위치
                        with(ObjectAnimator.ofFloat(logWindow, View.Y, button.y, logWindowY))  // Y 시작 위치, 마지막 위치
                        with(ObjectAnimator.ofFloat(logWindow, View.SCALE_X, startScale, 1f))  // X 크기
                        with(ObjectAnimator.ofFloat(logWindow, View.SCALE_Y, startScale, 1f))  // Y 크기
                    }
                    duration = shortAnimationDuration.toLong()
                    interpolator = DecelerateInterpolator()
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            currentAnimator = null
                        }

                        override fun onAnimationCancel(animation: Animator) {
                            currentAnimator = null
                        }
                    })
                    start()
                }
                zoomState = true
            }
            true -> {
                LogUtil.i(TAG, "Animation Close")
                currentAnimator = AnimatorSet().apply {
                    play(ObjectAnimator.ofFloat(logWindow, View.X, button.x)).apply {
                        with(ObjectAnimator.ofFloat(logWindow, View.Y, button.y))
                        with(ObjectAnimator.ofFloat(logWindow, View.SCALE_X, startScale))
                        with(ObjectAnimator.ofFloat(logWindow, View.SCALE_Y, startScale))
                    }
                    duration = shortAnimationDuration.toLong()
                    interpolator = DecelerateInterpolator()
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            logWindow.visibility = View.GONE
                            currentAnimator = null
                        }

                        override fun onAnimationCancel(animation: Animator) {
                            logWindow.visibility = View.GONE
                            currentAnimator = null
                        }
                    })
                    start()
                }
                zoomState = false
            }
        }
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")
    private fun changeFragment(id: Int) {
        fragmentId = id
        val fragment = when (id) {
            MAIN -> {
                HomeFragment.instance()
            }
            LIST -> {
                ListFragment.instance()
            }
            else -> {
                HomeFragment.instance()
            }
        }
        supportFragmentManager.beginTransaction().replace(R.id.container_main, fragment).commit()
    }

    init {
        instance = this
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_CODE = 1
        const val MAIN = 0x01
        const val LIST = 0x02

        var filePath = ""  // Internal Memory

        var itemList: MutableList<RoomItem> = mutableListOf()
        lateinit var itemDAO: RoomItemDao

        private var instance: MainActivity? = null
        fun instance(): MainActivity? { return instance }
    }
}