package com.example.cpvts
import android.app.Application
import com.example.cpvts.data.Network
class CpvtsApplication : Application() { val repository by lazy { Network.repository() } }
