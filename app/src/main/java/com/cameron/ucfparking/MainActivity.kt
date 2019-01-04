package com.cameron.ucfparking

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
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
    private var garages = ArrayList<Garage>()
    private var adapter = GarageViewAdapter(garages)
    private var client: OkHttpClient = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val color = Integer.toHexString(ContextCompat.getColor(this, R.color.UCFColor))

        // Need to remove the alpha from the beginning of the color
        val toolbarTitle = """
            <font color='#${color.substring(2)}'>UCF</font><font color='#000'>Parking</font>
            """.trimIndent()

        toolbar.title = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(toolbarTitle, Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(toolbarTitle)
        }

        if (savedInstanceState == null) {
            if (hasConnection()) {
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

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            addItemDecoration(DividerItemDecoration(
                    this@MainActivity,
                    DividerItemDecoration.VERTICAL)
            )
            setHasFixedSize(true)
            adapter = this@MainActivity.adapter
        }

        swipeRefreshLayout.setOnRefreshListener(this)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putParcelableArrayList(tag, garages)
    }

    override fun onRefresh() {
        if (hasConnection()) {
            loadGarageData()
            swipeRefreshLayout.isEnabled = false
        } else {
            showConnectionError()
        }
    }

    private fun showErrSnackbar(msg: String) {
        Snackbar.make(swipeRefreshLayout, msg, Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(R.string.action_retry), { loadGarageData() })
                .show()
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
                    runOnUiThread {
                        showErrSnackbar(getString(R.string.error_loading_data))
                    }
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
                        garages.add(parseResponse(text, i))
                    } catch (e: NumberFormatException) {
                        // Services might be down so the response has to be
                        // parsed differently
                        garages.clear()
                        for (j in 21..(text.size / 2) - 2 step 3) {
                            garages.add(parseResponse(text, j))
                        }
                    }
                }
                runOnUiThread { refreshView() }
            }

            override fun onFailure(call: Call?, e: IOException?) {
                Log.e(tag, getString(R.string.error_occured), e)
                runOnUiThread {
                    showErrSnackbar(getString(R.string.error_loading_data))
                }
            }
        })
    }

    private fun hasConnection(): Boolean {
        val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = manager.activeNetworkInfo
        return networkInfo.isConnectedOrConnecting
    }

    private fun showConnectionError() {
        loadingProgress.visibility = View.GONE
        recyclerView.visibility = View.GONE
        showErrSnackbar(getString(R.string.error_no_internet))
    }

    private fun parseResponse(text: List<String>, index: Int): Garage {
        val garageName = "${text[index]} ${text[index + 1]}"
        val spacesInfo = text[index + 2]
        val spacesAvailable = spacesInfo.split("/".toRegex()).toTypedArray()[0].toInt()
        val maxSpaces = spacesInfo.split("/".toRegex()).toTypedArray()[1].toInt()
        return Garage(garageName, spacesAvailable, maxSpaces)
    }
}