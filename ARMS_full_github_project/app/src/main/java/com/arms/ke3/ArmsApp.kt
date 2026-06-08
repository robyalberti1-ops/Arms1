package com.arms.ke3

import android.app.Application

class ArmsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ArmsBleManager.init(this)
    }
}
