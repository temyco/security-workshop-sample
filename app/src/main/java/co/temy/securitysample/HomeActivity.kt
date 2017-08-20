package co.temy.securitysample

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.View

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnNavigationItemSelectedListener { item -> onNavigationItemSelected(item) }

        showTab(KeysFragment())
    }

    fun onAddKeyClick(view: View) {
        val intent = Intent(this, AddKeyActivity::class.java)
        startActivity(intent)
    }

    private fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menuKeys) {
            showTab(KeysFragment())
            return true
        } else if (item.itemId == R.id.menuEncryption) {
            showTab(EncryptionFragment())
            return true
        }
        return false
    }

    private fun showTab(tab: Fragment) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.tabContainer, tab)
        fragmentTransaction.commitNow()
    }
}
