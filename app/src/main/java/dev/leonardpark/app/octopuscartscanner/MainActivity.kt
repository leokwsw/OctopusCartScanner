package dev.leonardpark.app.octopuscartscanner

import android.app.PendingIntent
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcF
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.google.android.material.snackbar.Snackbar
import dev.leonardpark.app.octopuscartscanner.databinding.ActivityMainBinding
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {

  private lateinit var binding: ActivityMainBinding

  private var nfcAdapter: NfcAdapter? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    super.onCreate(savedInstanceState)

    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    setSupportActionBar(binding.toolbar)

    binding.fab.setOnClickListener { view ->
      Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
        .setAnchorView(R.id.fab)
        .setAction("Action", null).show()
    }

    this.nfcAdapter = NfcAdapter.getDefaultAdapter(this)

    if (nfcAdapter == null) {
      val noNfcAlert = AlertDialog.Builder(this)
      noNfcAlert.setTitle("Oops").setMessage("NFC is not supported by this device.")
        .setPositiveButton(
          "Sorry. I'll buy a new phone."
        ) { dialog, id -> finish() }.create()
      noNfcAlert.show()
    }

    if (nfcAdapter != null && !nfcAdapter!!.isEnabled) {
      Toast.makeText(applicationContext, "Please enable NFC", Toast.LENGTH_LONG).show()
      val disabledNfcAlert = AlertDialog.Builder(this)
      disabledNfcAlert.setTitle("Oops").setMessage("NFC hasn't been enabled.")
        .setPositiveButton(
          "ENABLE"
        ) { dialog, id -> this@MainActivity.startActivity(Intent("android.settings.NFC_SETTINGS")) }
        .setNegativeButton(
          "CLOSE"
        ) { dialog, id -> finish() }.create()
      disabledNfcAlert.show()
    }
  }

  override fun onPause() {
    super.onPause()
    this.nfcAdapter?.disableForegroundDispatch(this)
  }

  override fun onResume() {
    super.onResume()

    val nfcIntentFilter = arrayOf(
      IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
      IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
      IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
    )

    val pendingFlags: Int = if (android.os.Build.VERSION.SDK_INT >= 23) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }

    val pendingIntent = PendingIntent.getActivity(
      this,
      0,
      Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
      0
    )


    this.nfcAdapter?.enableForegroundDispatch(
      this,
      pendingIntent,
      nfcIntentFilter,
      null,
    )
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    val action = intent.action
    Log.d("mooo", "RunTime : action : $action")

    if (NfcAdapter.ACTION_TAG_DISCOVERED == action
      || NfcAdapter.ACTION_TECH_DISCOVERED == action
      || NfcAdapter.ACTION_NDEF_DISCOVERED == action
    ) {
      val tagFromIntent = intent.getParcelableExtra<Parcelable>(NfcAdapter.EXTRA_TAG)
      if (tagFromIntent != null) {
        processOctopus(tagFromIntent as Tag?)
      } else {
        Log.i("mooo", "RunTime : tagFromIntent is null")
      }
    }
  }

  private fun processOctopus(tagFromIntent: Tag?) {
    Log.i("mooo", "RunTime : tag is null ? : " + (tagFromIntent == null))
    tagFromIntent?.also { tag ->
      try {
        val nfcF = NfcF.get(tag)
        try {
          if (nfcF.isConnected) {
            nfcF.close()
          }
          nfcF.connect()
          val idm = tag.id
          val command = ByteArray(16)
          command[0] = 16.toByte()
          command[1] = 6.toByte()
          System.arraycopy(idm, 0, command, 2, 8)
          command[10] = 1.toByte()
          command[11] = 23.toByte()
          command[12] = 1.toByte()
          command[13] = 1.toByte()
          command[14] = Byte.MIN_VALUE
          command[15] = 0.toByte()
          val response = nfcF.transceive(command)

          var money = 35.0

          AlertDialog.Builder(this)
            .setMessage("Money")
            .setPositiveButton("HK$35.0", object : DialogInterface.OnClickListener {
              override fun onClick(dialogInterface: DialogInterface?, position: Int) {
                money = 35.0
              }
            })
            .setNegativeButton("HK$50.0", object : DialogInterface.OnClickListener {
              override fun onClick(dialogInterface: DialogInterface?, position: Int) {
                money = 50.0
              }
            })
            .setNeutralButton("Cancel", object : DialogInterface.OnClickListener {
              override fun onClick(dialogInterface: DialogInterface?, position: Int) {

              }
            })
            .setOnDismissListener {
              val text = DecimalFormat("0.0").format(
                bytesToHexString(
                  byteArrayOf(
                    response[15],
                    response[16]
                  )
                ).toInt(16).toDouble() / 10.0 - money
                // 35.0 / 50.0
              )
              Log.i("mooo", "RunTime : text : $text")

              binding.contentMain.textviewBalance.text = text
            }
            .show()


          // alert to ask

          nfcF.close()
        } catch (e: java.lang.Exception) {
          Toast.makeText(applicationContext, "Read failed. Please try again.", Toast.LENGTH_LONG)
            .show()
          e.printStackTrace()
        }
      } catch (e2: java.lang.Exception) {
        e2.printStackTrace()
      }
    }
  }

  private fun bytesToHexString(src: ByteArray?): String {
    val stringBuilder = StringBuilder("")
    if (src == null || src.isEmpty()) {
      return ""
    }
    for (b in src) {
      val hv = "%02x".format(b)
      if (hv.length < 2) {
        stringBuilder.append(0)
      }
      stringBuilder.append(hv)
    }
    return stringBuilder.toString()
  }
}