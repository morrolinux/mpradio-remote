package com.example.morro.telecomando.UI

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import com.example.morro.telecomando.Core.MpradioBTHelper
import com.example.morro.telecomando.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.net.URL


class DownloadUpdateFragment : Fragment(), View.OnClickListener, MpradioBTHelper.MpradioBTHelperListener{
    private var mpradioBTHelper: MpradioBTHelper? = null
    private var updateFolderPath: String? = null
    private var progressBar: ProgressBar? = null

    private val uiScope = CoroutineScope(Dispatchers.Main)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val bundle = arguments
        mpradioBTHelper = bundle!!.getParcelable<Parcelable>("BTHelper") as MpradioBTHelper
        mpradioBTHelper?.setListener(this)
        updateFolderPath = Environment.getExternalStorageDirectory().toString() + "/Download"

        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_download_updates, container, false)
        (view.findViewById<View>(R.id.btnDownloadCore) as Button).setOnClickListener(this)
        (view.findViewById<View>(R.id.btnUpdateCore) as Button).setOnClickListener(this)
        (view.findViewById<View>(R.id.btnDownloadPiFm) as Button).setOnClickListener(this)
        (view.findViewById<View>(R.id.btnUpdatePiFm) as Button).setOnClickListener(this)
        (view.findViewById<View>(R.id.btnDownloadApp) as Button).setOnClickListener(this)
        (view.findViewById<View>(R.id.btnUpdateApp) as Button).setOnClickListener(this)

        /* Progress Bar */
        progressBar = view.findViewById<View>(R.id.downloadProgress) as ProgressBar
        progressBar?.visibility = View.GONE
        // progressBar!!.visibility = View.GONE
        return view
    }

    override fun onClick(v: View) {
        // val asyncBluetoothSend = AsyncBluetoothSend(mpradioBTHelper, activity)
        when (v.id) {
            R.id.btnDownloadCore -> uiScope.launch {
                asyncDownload("https://github.com/morrolinux/mpradio/archive/master.zip", "$updateFolderPath/mpradio-master.zip")
            }
            R.id.btnUpdateCore -> uiScope.launch {
                // mpradioBTHelper?.sendMessage("system systemctl stop mpradio")
                asyncBluetoothSend("$updateFolderPath/mpradio-master.zip", "mpradio-master.zip")
            }
            R.id.btnDownloadPiFm -> uiScope.launch {
                asyncDownload("https://github.com/Miegl/PiFmAdv/archive/master.zip", "$updateFolderPath/pifmadv-master.zip")
            }
            R.id.btnUpdatePiFm -> uiScope.launch {
                // mpradioBTHelper?.sendMessage("system systemctl stop mpradio")
                asyncBluetoothSend("$updateFolderPath/pifmadv-master.zip", "pifmadv-master.zip")
            }
            R.id.btnDownloadApp -> Toast.makeText(activity, "Not implemented yet!", Toast.LENGTH_LONG).show()
            R.id.btnUpdateApp -> Toast.makeText(activity, "Not implemented yet!", Toast.LENGTH_LONG).show()
            else -> {
            }
        }
    }

    fun bluetoothSend(srcName: String) {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        val screenshotUri: Uri = Uri.parse(srcName)

        sharingIntent.type = "audio/aac"
        sharingIntent.setPackage("com.android.bluetooth")
        sharingIntent.putExtra(Intent.EXTRA_STREAM, screenshotUri)
        startActivity(Intent.createChooser(sharingIntent, "Share file"))
    }

    private suspend fun asyncBluetoothSend(srcName: String, dstName: String) {
        withContext(Dispatchers.IO) {
            val preMessage =" Sending update package to the Pi. this will take some time..."
            val postMessage = "Update package sent to the Pi. Please wait until it reboots..."
            val errMessage = "Error connecting to FTP service!"

            Log.d("MPRADIO", preMessage)

            withContext(Dispatchers.Main){
                Toast.makeText(activity, preMessage, Toast.LENGTH_LONG).show()
                progressBar?.progress = 0
                progressBar?.visibility = View.VISIBLE
            }

            try {
                mpradioBTHelper?.sendFile(srcName, dstName)
            } catch (e: java.lang.Exception) {
                Log.d("MPRADIO", errMessage + e.message)
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, errMessage, Toast.LENGTH_LONG).show()
                }
                return@withContext
            }


            Log.d("MPRADIO", postMessage)

            withContext(Dispatchers.Main){
                Toast.makeText(activity, postMessage, Toast.LENGTH_LONG).show()
                progressBar?.visibility = View.GONE
            }

        }
    }

    /**
     * So che si era detto di non fare classi Kotlin dipendenti da Android
     * (e a tale scopo ho realizzato la struttura dati Song.kt come suggerito nella traccia)
     * MA volevo anche provare una activity asincrona suspend con withContext di Kotlin
     * dato che le AsyncActivity (implementato in AsyncURLDownload.java) sono state deprecate
     * e questo è il sostituto più vicino per eseguire una attività in background
     * e aggiornare una progress bar con semplicità
     */
    private suspend fun asyncDownload(urlD: String, dest: String) {
        withContext(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                progressBar!!.progress = 0
                progressBar!!.max = 100
                progressBar!!.visibility = View.VISIBLE
            }
            var count: Int
            val bufferSize = 512
            try {
                val url = URL(urlD)
                val destination: String = dest
                Log.d("MPRADIO", "Downloading")
                val conection = url.openConnection()
                conection.connect()
                val input: InputStream = BufferedInputStream(url.openStream(), bufferSize)
                val output: OutputStream = FileOutputStream(destination)
                val fileLength = conection.contentLength
                val tmpBuffer = ByteArray(bufferSize)
                var total: Long = 0 //keep track of file downloaded/length
                while (input.read(tmpBuffer).also { count = it } != -1) {
                    total += count.toLong()
                    withContext(Dispatchers.Main) {
                        Log.d("MPRADIO", "Progress: " + (total / fileLength.toFloat() * 100).toInt())
                        progressBar!!.progress = ((total / fileLength.toFloat() * 100).toInt())
                    }
                    output.write(tmpBuffer, 0, count)
                }
                output.flush()
                output.close()
                input.close()
                Log.d("MPRADIO", "Download complete!")
                withContext(Dispatchers.Main) {
                    progressBar!!.visibility = View.GONE
                    Toast.makeText(context, "Download complete!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("MPRADIO: ", "Download ERROR! " + e.message)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Download ERROR!", Toast.LENGTH_LONG).show()
                    progressBar!!.visibility = View.GONE
                }
            }
        }
    }

    override fun onConnectionFail() {
        TODO("Not yet implemented")
    }

    override fun onBTProgressUpdate(progress: Int) {
        progressBar?.progress = progress
    }

}