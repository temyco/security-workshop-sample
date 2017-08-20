package co.temy.securitysample;

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import co.temy.securitysample.encryption.Crypto

class EncryptionFragment : Fragment() {

    private var keyNumber = 0

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_encryption, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view?.findViewById<View>(R.id.btnAliases)?.setOnClickListener { printAliases() }
        view?.findViewById<View>(R.id.btnCreateSyncKey)?.setOnClickListener { createKey() }
    }

    private fun printAliases() {
        val crypto = Crypto(activity.applicationContext)
        crypto.getAllKeyAliases()
                .sortedBy{it.creationDate}
                .forEach { Log.i("EncryptionFragment", "${it} \n") }
    }

    private fun createKey() {
        val crypto = Crypto(activity.applicationContext)
        crypto.createSymmetricKey("Test-${++keyNumber}", false)
    }
}
