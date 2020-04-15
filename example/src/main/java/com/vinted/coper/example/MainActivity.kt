package com.vinted.coper.example

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addFragmentToActivity(ExampleSelectFragment())
    }

    fun addFragmentToActivity(fragment: Fragment) {
        supportFragmentManager.commit(allowStateLoss = true) {
            addToBackStack(null)
            replace(android.R.id.content, fragment)
        }
    }
}
