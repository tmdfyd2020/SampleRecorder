package org.techtown.samplerecorder.activity

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.media.AudioFormat
import android.os.Bundle
import android.view.*
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.techtown.samplerecorder.activity.DialogActivity.Companion.KEY_FILE_NAME
import org.techtown.samplerecorder.R
import org.techtown.samplerecorder.audio.RecordService.Companion.CODE_FILE_NAME
import org.techtown.samplerecorder.audio.RecordService.Companion.file
import org.techtown.samplerecorder.audio.RecordService.Companion.fileCreateTime
import org.techtown.samplerecorder.audio.RecordService.Companion.record
import org.techtown.samplerecorder.database.RoomHelper
import org.techtown.samplerecorder.database.RoomItem
import org.techtown.samplerecorder.database.RoomItem.Companion.ROOM_TABLE_NAME
import org.techtown.samplerecorder.database.RoomItemDao
import org.techtown.samplerecorder.databinding.ActivityMainBinding
import org.techtown.samplerecorder.home.HomeFragment
import org.techtown.samplerecorder.home.HomeFragment.Companion.recordChannel
import org.techtown.samplerecorder.home.HomeFragment.Companion.recordRate
import org.techtown.samplerecorder.list.ListFragment
import org.techtown.samplerecorder.util.DialogService.Companion.dialogs
import org.techtown.samplerecorder.util.LogUtil

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val container by lazy { binding.container }
    private val logWindow by lazy { binding.layoutLogWindow.layoutMainLogWindow }

    private var fragmentId: Int = MAIN

    private var logWindowX: Float = 0f
    private var logWindowY: Float = 0f
    private var shortAnimationDuration: Int = 0
    private var currentAnimator: Animator? = null
    private var zoomState: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        val helper = Room.databaseBuilder(this, RoomHelper::class.java, ROOM_TABLE_NAME).build()
        itemDAO = helper.roomItemDao()
        syncDatabase()
        changeFragment(fragmentId)
        checkPermission()
        initialize()
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

    private fun checkPermission() {
        val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_CODE)
        }
    }

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    private fun initialize() {
        // Toolbar initialization
        setSupportActionBar(binding.toolbarMain)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        filePath = filesDir.absolutePath

        // Initialize start coordination of emerging log window
        with (container) {
            viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    val containerWidth = width
                    with (logWindow) {
                        viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                            override fun onGlobalLayout() {
                                logWindowX = ((containerWidth - width) / 2).toFloat()
                                logWindowY = (height / 2).toFloat()
                                viewTreeObserver.removeOnGlobalLayoutListener(this)
                            }
                        })
                    }
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            })
        }

        shortAnimationDuration = resources.getInteger(android.R.integer.config_shortAnimTime)

        // Log window touch and drag moving listener initialization
        var moveX = 0f
        var moveY = 0f
        logWindow.setOnTouchListener { view: View, event: MotionEvent ->
            when (event.action) {
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
        menuInflater.inflate(R.menu.menu_toolbar, menu)
        return true
    }

    @SuppressLint("NonConstantResourceId", "UseCompatLoadingForDrawables")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_exit -> {
                dialogs(getString(R.string.exit)).show(supportFragmentManager, getString(R.string.exit))
            }
            R.id.list_play -> {
                if (fragmentId == MAIN) {
                    item.icon = getDrawable(R.drawable.ic_toolbar_recorder)
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

            R.id.iv_main_close_popup -> {
                zoomAnimation(binding.ivMainStartWindow)
            }
        }
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
                LogUtil.i(TAG, "Log window Open")
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
                LogUtil.i(TAG, "Log window Closed")
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

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val recordService = record(this)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                CODE_FILE_NAME -> {
                    val name = data?.getStringExtra(KEY_FILE_NAME)
                    insertItem(name!!)
                    Toast.makeText(this, file.name + " ${getString(R.string.toast_save_success)}", Toast.LENGTH_LONG).apply {
                        setGravity(Gravity.TOP, 0, resources.getInteger(R.integer.toast_height))
                        show()
                    }
                }
            }
        } else {
            file.delete()
            Toast.makeText(this, file.name + " ${getString(R.string.toast_save_failure)}", Toast.LENGTH_LONG).apply {
                setGravity(Gravity.TOP, 0, resources.getInteger(R.integer.toast_height))
                show()
            }
        }
    }

    private fun insertItem(name: String) {
        val itemName : String = if (name == "") file.name else name
        val channel : String = if (recordChannel == AudioFormat.CHANNEL_IN_MONO) getString(R.string.mono) else getString(
            R.string.stereo
        )
        val item = RoomItem(itemName, file.name, fileCreateTime, channel, recordRate)
        CoroutineScope(Dispatchers.IO).launch {
            itemDAO.insert(item)
            syncDatabase()
        }
    }

    @SuppressLint("SetTextI18n")
    fun writeLogWindow(msg: String) {
        binding.layoutLogWindow.tvMainLogWindow.let { textView ->
            textView.text = "$msg\n${textView.text}"
        }
    }

    init {
        instance = this
    }

    companion object {
        private const val TAG = "MainActivity"
        private var instance: MainActivity? = null
        fun instance(): MainActivity? { return instance }

        private const val PERMISSION_CODE = 1
        private const val MAIN            = 0x01
        private const val LIST            = 0x02

        var filePath = ""  // Internal Memory

        var itemList: MutableList<RoomItem> = mutableListOf()
        lateinit var itemDAO: RoomItemDao
        fun syncDatabase() {
            CoroutineScope(Dispatchers.IO).launch {
                itemList.clear()
                itemList.addAll(itemDAO.getList())
            }
        }
    }
}