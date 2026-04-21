package com.smartr.mobile

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this).apply {
            text = "Smartr mobile companion (phase 2)"
        }
        setContentView(tv)
    }
}
