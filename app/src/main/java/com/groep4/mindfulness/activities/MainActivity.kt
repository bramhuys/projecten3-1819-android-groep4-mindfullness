package com.groep4.mindfulness.activities

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.groep4.mindfulness.R
import com.groep4.mindfulness.fragments.FragmentProfiel
import com.groep4.mindfulness.model.Gebruiker
import com.groep4.mindfulness.model.Oefening
import com.groep4.mindfulness.model.Sessie
import com.groep4.mindfulness.utils.ExtendedDataHolder
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URL


class MainActivity : AppCompatActivity() {

    private val BACK_STACK_ROOT_TAG = "root_fragment"
    private val client = OkHttpClient()
    lateinit var mAuth: FirebaseAuth
    private var isFragmentProfielLoaded = false
    var gebruiker : Gebruiker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mAuth = FirebaseAuth.getInstance()
        Logger.addLogAdapter(AndroidLogAdapter())

        // Toolbar
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        this.gebruiker = getAangemeldeGebruiker()

        // Sessies
        val sessies: ArrayList<Sessie> = getSessies()


        // belangrijk key_page mee te geven om juiste fragment te kunnen laden vanuit eenzelfde activity!
        ll_sessies.setOnClickListener {
            val intent = Intent(this, ActivityPage::class.java)
            intent.putExtra("key_page", "sessies")

            //save sessies in een externe klasse aangezien app crasht door grootte indien meegegeven met een intent
            var extras = ExtendedDataHolder.getInstance()
            extras.putExtra("sessielist", sessies)

            startActivity(intent)
        }

        ll_reminder.setOnClickListener {
            val intent = Intent(this, ActivityPage::class.java)
            intent.putExtra("key_page", "reminder")

            //save sessies in een externe klasse aangezien app crasht door grootte indien meegegeven met een intent
            var extras = ExtendedDataHolder.getInstance()
            extras.putExtra("sessielist", sessies)

            startActivity(intent)
        }

        ll_contact.setOnClickListener{
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("key_page", "contact")
            startActivity(intent)

        }

        ll_kalender.setOnClickListener{
            val intent = Intent(this, ActivityKalender::class.java)
            intent.putExtra("key_page", "kalender")
            startActivity(intent)

        }


    }



    // Menu icons are inflated just as they were with actionbar
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    // Sessies ophalen
    fun getSessies(): ArrayList<Sessie> {
        val sessies: ArrayList<Sessie> = ArrayList()

        // HTTP Request sessies
        val request = Request.Builder()
                /*.header("Authorization", "token abcd")*/
                .url("http://141.134.155.219:3000/sessies")
                .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ERROR", "HTTP request failed: $e")
            }

            override fun onResponse(call: Call, response: Response) {
                sessies.clear()
                val jsonarray = JSONArray(response.body()!!.string())
                for (i in 0 until jsonarray.length()) {
                    val jsonobject = jsonarray.getJSONObject(i)
                    val sessieId = jsonobject.getInt("sessieId")
                    val naam = jsonobject.getString("naam")
                    val beschrijving = jsonobject.getString("beschrijving")
                    val oefeningen = getOefeningen(sessieId)
                    //val oefeningen = ArrayList<Oefening>()
                    val sessieCode = jsonobject.getString("sessieCode")
                    val sessie: Sessie = Sessie(sessieId, naam, beschrijving, oefeningen, sessieCode,false)
                    sessies.add(sessie)
                }
            }
        })
        return sessies
    }

    fun getAangemeldeGebruiker() : Gebruiker{
        var gebruiker : Gebruiker = Gebruiker()
        val id = mAuth.currentUser!!.uid
        val string1 = ("http://141.134.155.219:3000/users/" + id)
        val string = "http://141.134.155.219:3000/users/yXQmL8IGSCbN15fzWw60t5udU2o2"
        Logger.d("MEOZOZOZ", string1)
        // HTTP Request sessies
        val request = Request.Builder()
                /*.header("Authorization", "token abcd")*/
                .url(string1/*+ mAuth.currentUser!!.uid*/)
                .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ERROR", "HTTP request failed: $e")
            }

            override fun onResponse(call: Call, response: Response) {
                //val jsonarray = JSONArray(response.body()!!.string())
                val jsonobject = JSONObject(response.body()!!.string())

                    gebruiker.uid = mAuth.currentUser!!.uid
                    gebruiker.regio = if (jsonobject.has("regio")) jsonobject.getString("regio") else ""
                    gebruiker.email = if (jsonobject.has("email")) jsonobject.getString("email") else ""
                    gebruiker.name = if (jsonobject.has("name")) jsonobject.getString("name") else ""
                    gebruiker.telnr = if (jsonobject.has("telnr")) jsonobject.getString("telnr") else ""
                    gebruiker.groepsnr = if (jsonobject.has("groepnr")) jsonobject.getInt("groepnr") else 0

            }
        })
        return gebruiker
    }

    // Oefeningen van sessie ophalen
    fun getOefeningen(sessieId: Int): ArrayList<Oefening>{
        val oefeningen: ArrayList<Oefening> = ArrayList()

        // HTTP Request oefeningen
        val request = Request.Builder()
                /*.header("Authorization", "token abcd")*/
                .url("http://141.134.155.219:3000/oefeningen/$sessieId")
                .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ERROR", "HTTP request failed: $e")
            }

            override fun onResponse(call: Call, response: Response) {
                val jsonarray = JSONArray(response.body()!!.string())
                for (i in 0 until jsonarray.length()) {
                    val jsonobject = jsonarray.getJSONObject(i)
                    val oefeningenId = jsonobject.getInt("oefeningId")
                    val naam = jsonobject.getString("naam")
                    val beschrijving = jsonobject.getString("beschrijving")
                    val sessieid = jsonobject.getInt("sessieId")
                    val fileUrl = jsonobject.getString("fileName")
                    val fileMimeType = jsonobject.getString("fileMimetype")
                    val groepen = jsonobject.getString("groepen")

                    val oefening: Oefening = Oefening(oefeningenId, naam, beschrijving, sessieid, fileUrl, fileMimeType, groepen)
                    oefeningen.add(oefening)
                }
            }
        })
        return oefeningen
    }

    override fun onBackPressed() {

        //In case there are no fragments visible (mainactivity is visible) disable the back button
        if (supportFragmentManager.backStackEntryCount > 0) {
            super.onBackPressed()
        }
    }

    fun veranderGegevensGebruiker(gebruikersnaam : String, regio : String, telnr : String) {
        gebruiker!!.name = gebruikersnaam
        gebruiker!!.regio = regio
        gebruiker!!.telnr = telnr
    }

    fun gegevensGebruikerOpslaan(body : FormBody, url : String) : String {
        var response2 : String? = null
        val thread = Thread(Runnable {
            val mediaType: MediaType? = MediaType.parse("application/json; charset=utf-8")
            val client: OkHttpClient = OkHttpClient()
            //val body: RequestBody = RequestBody.create(mediaType, json)
            val request: Request = Request.Builder().url(url).put(body).build()
            val response = client.newCall(request).execute()
            response2 = response.body().toString()
        })
        thread.start()
        getAangemeldeGebruiker()
        //supportFragmentManager.popBackStack()


        return response2.orEmpty()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle presses on the action bar menu items
        when (item.itemId) {
            R.id.action_logout -> {

                val builder = AlertDialog.Builder(this@MainActivity)
                builder.setMessage("Wil je uitloggen ?")

                builder.setPositiveButton("Ja"){dialog, which ->
                    FirebaseAuth.getInstance().signOut()
                    Toast.makeText(this,"Account uitgelogd", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, ActivityLogin::class.java)
                    this.startActivity(intent)
                    finish()
                }

                builder.setNegativeButton("Nee"){dialog,which ->
                }

                val dialog: AlertDialog = builder.create()
                dialog.show()


                return true
            }

            R.id.action_profiel -> {
                setFragment(FragmentProfiel(), true)
                isFragmentProfielLoaded = true
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun setFragment(fragment: Fragment, addToBackstack: Boolean) {
        if (addToBackstack)
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_holder_main, fragment, "pageContent")
                    .addToBackStack(BACK_STACK_ROOT_TAG)
                    .commit()
        else
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_holder_main, fragment, "pageContent")
                    .commit()
    }

    fun postFeedback(url: String, json: String): String {
        val mediaType: MediaType? = MediaType.parse("application/json; charset=utf-8")
        val client: OkHttpClient = OkHttpClient()
        val body: RequestBody = RequestBody.create(mediaType, json)
        val request: Request = Request.Builder().url(url).post(body).build()
        val response: Response = client.newCall(request).execute()
        return response.body().toString()
    }

}
