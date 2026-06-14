package com.voicerider.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.voicerider.app.R
import com.voicerider.app.service.VoiceRoutingService
import com.voicerider.app.viewmodel.HomeViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: HomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        // Start voice routing service and wire to ViewModel
        val voiceServiceIntent = Intent(this, VoiceRoutingService::class.java)
        startService(voiceServiceIntent)
    }

    fun getHomeViewModel(): HomeViewModel = viewModel
}
