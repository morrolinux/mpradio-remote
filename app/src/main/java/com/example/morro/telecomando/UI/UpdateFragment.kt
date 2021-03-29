package com.example.morro.telecomando.UI

import android.content.Context
import android.os.Bundle
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


class UpdateFragment : Fragment(), View.OnClickListener, MpradioBTHelper.MpradioBTHelperListener {
    private var mpradioBTHelper: MpradioBTHelper? = null
    private var progressBar: ProgressBar? = null

    private val uiScope = CoroutineScope(Dispatchers.Main)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val bundle = arguments
        mpradioBTHelper = bundle!!.getParcelable<Parcelable>("BTHelper") as MpradioBTHelper

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
        return view
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btnDownloadCore -> uiScope.launch {
                asyncDownload("https://github.com/morrolinux/mpradio/archive/master.zip", "mpradio-master.zip")
            }
            R.id.btnUpdateCore -> {
                progressBar?.visibility = View.VISIBLE
                mpradioBTHelper?.sendFile("mpradio-master.zip", this)
            }
            R.id.btnDownloadPiFm -> uiScope.launch {
                asyncDownload("https://github.com/Miegl/PiFmAdv/archive/master.zip", "pifmadv-master.zip")
            }
            R.id.btnUpdatePiFm -> {
                progressBar?.visibility = View.VISIBLE
                mpradioBTHelper?.sendFile("pifmadv-master.zip", this)
            }
            R.id.btnDownloadApp -> Toast.makeText(activity, "Not implemented yet!", Toast.LENGTH_LONG).show()
            R.id.btnUpdateApp -> Toast.makeText(activity, "Not implemented yet!", Toast.LENGTH_LONG).show()
            else -> {
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
    private suspend fun asyncDownload(urlD: String, destination: String) {
        withContext(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                progressBar!!.progress = 0
                progressBar!!.max = 100
                progressBar!!.visibility = View.VISIBLE
            }
            var count: Int
            val bufferSize = 512
            try {
                Log.d("MPRADIO", "Downloading $urlD")
                val url = URL(urlD)
                var fileLength = -1

                while (fileLength <= 0) {
                    val connection = url.openConnection()
                    connection.connect()
                    fileLength = connection.contentLength
                }
                val input: InputStream = BufferedInputStream(url.openStream(), bufferSize)
                val output: FileOutputStream? = context?.openFileOutput(destination, Context.MODE_PRIVATE)

                val tmpBuffer = ByteArray(bufferSize)
                var total: Long = 0 //keep track of file downloaded/length
                while (input.read(tmpBuffer).also { count = it } != -1) {
                    total += count.toLong()
                    withContext(Dispatchers.Main) {
                        Log.d("MPRADIO", "Progress: " + (total / fileLength.toFloat() * 100).toInt())
                        progressBar!!.progress = ((total / fileLength.toFloat() * 100).toInt())
                    }
                    output?.write(tmpBuffer, 0, count)
                }
                output?.flush()
                output?.close()
                input.close()

                Log.d("MPRADIO", "Download complete!")
                withContext(Dispatchers.Main) {
                    progressBar!!.visibility = View.GONE
                    progressBar!!.progress = 0
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

    override fun onBTOperationFailed(errMessage: String) {
        progressBar?.visibility = View.GONE
        Toast.makeText(activity, errMessage, Toast.LENGTH_LONG).show()
    }

    override fun onBTOperationCompleted() {
        progressBar?.visibility = View.GONE
        Toast.makeText(activity, "Completed!", Toast.LENGTH_LONG).show()
    }

    override fun onBTProgressUpdate(progress: Int) {
        progressBar?.progress = progress
    }

}