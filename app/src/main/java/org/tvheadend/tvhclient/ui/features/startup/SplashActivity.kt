package org.tvheadend.tvhclient.ui.features.startup

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = Intent(this, StartupActivity::class.java)
        startActivity(intent)
        finish()
    }
}
