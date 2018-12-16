package com.cameron.ucfparking

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.text.Html
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import org.jsoup.Jsoup
import org.jsoup.safety.Cleaner
import org.jsoup.safety.Whitelist
import java.io.IOException


class MainActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener {

    private val tag = MainActivity::class.java.simpleName
    private val client = OkHttpClient()
    private var garages = ArrayList<Garage>()
    private var adapter = GarageViewAdapter(garages)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

            val ucfColor ="#BDB000"
            val toolbarTitle = "<font color='$ucfColor'>UCF</font><font color='#000'>Parking</font>"

            toolbar.title = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(toolbarTitle, Html.FROM_HTML_MODE_LEGACY)
            } else {
                Html.fromHtml(toolbarTitle)
            }

        if (savedInstanceState == null) {
            if (hasConnection(this)) {
                loadingProgress.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                loadGarageData()
            } else {
                showConnectionError()
            }
        } else {
            garages = savedInstanceState.getParcelableArrayList(tag)
            adapter.updateList(garages)
            loadingProgress.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter

        swipeRefreshLayout.setOnRefreshListener(this)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putParcelableArrayList(tag, garages)
    }

    override fun onRefresh() {
        if (hasConnection(this)) {
            loadGarageData()
            swipeRefreshLayout.isEnabled = false
        } else {
            showConnectionError()
        }
    }

    private fun refreshView() {
        if (swipeRefreshLayout.isRefreshing) {
            swipeRefreshLayout.isRefreshing = false
        }

        swipeRefreshLayout.isEnabled = true

        loadingProgress.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        adapter.updateList(garages)
    }

    private fun loadGarageData() {
        val url = "http://secure.parking.ucf.edu/GarageCount/"
        val request = Request.Builder().url(url).build()
        garages.clear()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call?, response: Response?) {
                if (response?.body() == null) {
                    Log.i(tag, "Response was null :(")
                    return
                }
                // This is the raw HTML returned from the link
                val rawDoc = Jsoup.parse(response.body()?.string())
                // This is the raw HTML but cleaned up
                val cleanedDoc = Cleaner(Whitelist.basic()).clean(rawDoc)
                val element = cleanedDoc.body()
                // This is the text around the <strong> tags
                val text = element.text().split("\\s+".toRegex())

                for (i in 6..text.size - 3 step 3) {
                    try {
                        val garageName = "${text[i]} ${text[i + 1]}"
                        val spacesInfo = text[i + 2]
                        val spacesAvailable = spacesInfo.split("/".toRegex()).toTypedArray()[0].toInt()
                        val maxSpaces = spacesInfo.split("/".toRegex()).toTypedArray()[1].toInt()
                        garages.add(Garage(garageName, spacesAvailable, maxSpaces))
                    } catch (e: NumberFormatException) {
                        // Services might be down so the response has to be
                        // parsed differently

                        // A few garages from the first loop might've been added
                        // so those might need to be cleared
                        garages.clear()
                        for (j in 21..(text.size / 2) - 2 step 3) {
                            val garageName = "${text[j]} ${text[j + 1]}"
                            val spacesInfo = text[j + 2]
                            val spacesAvailable = spacesInfo.split("/".toRegex()).toTypedArray()[0].toInt()
                            val maxSpaces = spacesInfo.split("/".toRegex()).toTypedArray()[1].toInt()
                            garages.add(Garage(garageName, spacesAvailable, maxSpaces))
                        }
                    }
                }
                garages.forEach { Log.i(tag, it.toString()) }
                runOnUiThread {
                    refreshView()
                }
            }

            override fun onFailure(call: Call?, e: IOException?) {
                Log.e(tag, getString(R.string.error_occured), e)
                runOnUiThread {
                    Snackbar.make(swipeRefreshLayout, getString(R.string.error_loading_html), Snackbar.LENGTH_INDEFINITE)
                            .setAction(getString(R.string.action_retry), { loadGarageData() })
                            .show()
                }
            }
        })
    }

    private fun hasConnection(context: Context): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = manager.activeNetworkInfo
        return networkInfo.isConnectedOrConnecting
    }

    private fun showConnectionError() {
        loadingProgress.visibility = View.GONE
        recyclerView.visibility = View.GONE
        Snackbar.make(swipeRefreshLayout, getString(R.string.error_no_internet), Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(R.string.action_retry), { loadGarageData() })
                .show()
    }
}