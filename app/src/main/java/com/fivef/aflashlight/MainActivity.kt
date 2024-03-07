package com.fivef.aflashlight

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.core.view.InputDeviceCompat
import androidx.core.view.MotionEventCompat
import com.fivef.aflashlight.databinding.ActivityMainBinding
import java.util.*
import kotlin.concurrent.timerTask


class MainActivity : Activity() {

    private lateinit var binding: ActivityMainBinding
    private var isNormalFlashlightOn: Boolean = false

    private var strobeState: Boolean = false

    private var appFinished: Boolean = false

    private var strobeFrequency: Long = 10

    private var brightness: Float = 1.0F //value 0..1 1 is fully opaque
    private var crown_sensitivity: Float = 0.1F //value 0..1 1 is most sensitive

    private var timer: CountDownTimer = object: CountDownTimer(1000000, 300) {
        override fun onTick(millisUntilFinished: Long) {
            toggleStrobe()
        }
        override fun onFinish() {
            // do something
        }
    }
    private fun setBackgroundColor(color: Int) {
//        Toast.makeText(this@MainActivity, text, Toast.LENGTH_SHORT).show()
        binding.layoutMain.setBackgroundColor(color)
        binding.layoutMain.invalidate()
    }


    private fun toggleNormalFlashLight() {
        if (!isNormalFlashlightOn) {
            switchOnFlashLight()
        } else {
            switchOffFlashLight()
        }
    }

    private fun switchOnStrobe(){
        isNormalFlashlightOn = false
        //make sure brightness is 100%
        val layout = window.attributes
        getWindow().setAttributes(layout)
        layout.screenBrightness = 1.0f

        timer.start()
    }

    private fun toggleStrobe(){
        if(strobeState){
            strobeOff()
        }else{
            strobeOn()
        }
    }

    private fun strobeOn(){
        strobeState = true
        setBackgroundColor(Color.WHITE)
    }

    private fun strobeOff(){
        strobeState = false
        setBackgroundColor(Color.BLACK)
    }


    private fun switchOnFlashLight(){
        setBackgroundColor(Color.WHITE)
        isNormalFlashlightOn = true

        val layout = window.attributes
        getWindow().setAttributes(layout)

        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        val startup_brightness_default_value = resources.getFloat(R.dimen.startup_brightness_default_value)
        brightness = sharedPref.getFloat(resources.getString(R.string.startup_brightness_key), startup_brightness_default_value)
        layout.screenBrightness = brightness

    }

    private fun switchOffFlashLight(){
        setBackgroundColor(Color.BLACK)
        isNormalFlashlightOn = false

        val layout = window.attributes
        layout.screenBrightness = 0.0f
        getWindow().setAttributes(layout)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if (event?.repeatCount == 0) {
            when (keyCode) {
                KeyEvent.KEYCODE_STEM_1 -> {

                    /* Close the app when pressing to top right button. I cannot directly call
                    finish here because then the onKeyDown Event
                    will not be handled by the application
                    but by the system and directly restart the app as it is assigned to button 1
                    (top button) in settings. */
                    Timer().schedule(timerTask {

                        val sharedPref = getPreferences(Context.MODE_PRIVATE)
                        with (sharedPref.edit()) {
                            putFloat(getString(R.string.startup_brightness_key), brightness)
                            apply()
                        }
                        finish()
                    }, 300)
                    return true
                }
                KeyEvent.KEYCODE_STEM_2 -> {
                    switchOnStrobe()
                    return true
                }
                else -> {
                    super.onKeyDown(keyCode, event)
                }
            }
        } else {
            super.onKeyDown(keyCode, event)
        }
    }

    override fun onResume() {
        super.onResume()
        switchOnFlashLight()
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        super.onGenericMotionEvent(event)
        if (event?.action == MotionEvent.ACTION_SCROLL &&
            event?.isFromSource(InputDeviceCompat.SOURCE_ROTARY_ENCODER)
        ) {
            // Don't forget the negation here
            val delta = -event?.getAxisValue(MotionEventCompat.AXIS_SCROLL) * crown_sensitivity
            brightness += delta

            if (brightness >= 1.0f){
                brightness = 1.0f
            }
            if (brightness <= 0.03f){
                //at some values close to 0.0 somehow switches back on the screen so we put 0.03f
                brightness = 0.03f
            }

            if(isNormalFlashlightOn) {
                //flashlight mode
                val layout = window.attributes
                layout.screenBrightness = brightness
                getWindow().setAttributes(layout)

            }else {
                //strobe mode

                strobeFrequency = (brightness * 1000).toLong()

                println(strobeFrequency)

                timer.cancel()
                timer = object : CountDownTimer(1000000, strobeFrequency) {
                    override fun onTick(millisUntilFinished: Long) {
                        toggleStrobe()
                    }

                    override fun onFinish() {
                        // do something
                    }
                }
                timer.start()
            }

            return true
        } else {
            return false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        val startup_brightness_default_value = resources.getFloat(R.dimen.startup_brightness_default_value)
        brightness = sharedPref.getFloat(resources.getString(R.string.startup_brightness_key), startup_brightness_default_value)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        switchOnFlashLight()

        //get focust for rotary encoder input
        binding.root.requestFocus()

        binding.apply {
            layoutMain.setOnClickListener {
                toggleNormalFlashLight()
            }
        }
    }
}