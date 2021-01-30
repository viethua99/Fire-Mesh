package com.ceslab.firemesh.presentation.base

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import timber.log.Timber

abstract class BaseActivity : AppCompatActivity() {

    private lateinit var fragmentManager: FragmentManager
    protected abstract fun getResLayoutId(): Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getResLayoutId())
    }

    private fun generateFragmentManager() {
        Timber.d("generateFragmentManager")
        fragmentManager = supportFragmentManager
    }

    fun showToastMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    fun addFragment(fragment: Fragment, tag: String, containerId: Int) {
        Timber.d("addFragment: name=${fragment.javaClass.name}")
        generateFragmentManager()
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(containerId, fragment, tag)
            .addToBackStack(tag)
            .commit()
    }

    fun replaceFragment(fragment: Fragment, tag: String, containerId: Int) {
        Timber.d("replaceFragment: name=${fragment.javaClass.name}")
        generateFragmentManager()
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(containerId, fragment, tag)
            .addToBackStack(tag)
            .commit()
    }

}