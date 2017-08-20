package co.temy.securitysample

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
    }

    fun onAddKeyClick(view: View) {
        val intent = Intent(this, AddKeyActivity::class.java)
        startActivity(intent)
    }
}
