package com.groep4.mindfulness.activities

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.groep4.mindfulness.R
import com.groep4.mindfulness.fragments.*
import com.groep4.mindfulness.interfaces.CallbackInterface
import com.groep4.mindfulness.model.Gebruiker
import com.groep4.mindfulness.model.Oefening
import com.groep4.mindfulness.model.Sessie
import com.groep4.mindfulness.utils.ExtendedDataHolder
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.*


class MainActivity : AppCompatActivity(), CallbackInterface {

    private val BACK_STACK_ROOT_TAG = "root_fragment"
    private val client = OkHttpClient()
    lateinit var mAuth: FirebaseAuth
    var gebruiker : Gebruiker = Gebruiker()
    var sessies: ArrayList<Sessie> = ArrayList()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mAuth = FirebaseAuth.getInstance()
        Logger.addLogAdapter(AndroidLogAdapter())

        // Set gebruiker
        if(this.gebruiker == null) {
            this.gebruiker = getAangemeldeGebruiker()
        }

        // Sessies
        sessies = getSessiesFromDB()
        val extras = ExtendedDataHolder.getInstance()
        extras.putExtra("sessielist", sessies)


        //Set no new fragment if there already is one
        if (savedInstanceState == null) {
            setFragment(FragmentMain(), false)
        }
    }

    /**
     * Om fragment te tonen
     */
    override fun setFragment(fragment: Fragment, addToBackstack: Boolean) {
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

    /**
     * ActionMenu aanmaken
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    /**
     * Sessies ophalen
     */
    fun getSessiesFromDB(): ArrayList<Sessie> {
        val tempSessies: ArrayList<Sessie> = ArrayList()

        // HTTP Request sessies
        val request = Request.Builder()
                .url("http://141.134.155.219:3000/sessies")
                .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ERROR", "HTTP request failed: $e")
            }

            override fun onResponse(call: Call, response: Response) {
                tempSessies.clear()
                val jsonarray = JSONArray(response.body()!!.string())
                for (i in 0 until jsonarray.length()) {
                    val jsonobject = jsonarray.getJSONObject(i)
                    val sessieId = jsonobject.getInt("sessieId")
                    val naam = jsonobject.getString("naam")
                    val beschrijving = jsonobject.getString("beschrijving")
                    val oefeningen = getOefeningen(sessieId)
                    val sessieCode = jsonobject.getString("sessieCode")
                    val sessie = Sessie(sessieId, naam, beschrijving, oefeningen, sessieCode)
                    tempSessies.add(sessie)
                }
            }
        })
        return tempSessies
    }

    /**
     * Aangemelde gebruiker ophalen
     */
    fun getAangemeldeGebruiker() : Gebruiker{
        val gebruiker : Gebruiker = Gebruiker()
        val id = mAuth.currentUser!!.uid
        val string1 = ("http://141.134.155.219:3000/users/" + id)
        val string = "http://141.134.155.219:3000/users/yXQmL8IGSCbN15fzWw60t5udU2o2"

        // HTTP Request sessies
        val request = Request.Builder()
                .url(string1)
                .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ERROR", "HTTP request failed: $e")
            }

            override fun onResponse(call: Call, response: Response) {
                val jsonobject = JSONObject(response.body()!!.string())

                gebruiker.uid = mAuth.currentUser!!.uid
                gebruiker.regio = if (jsonobject.has("regio")) jsonobject.getString("regio") else ""
                gebruiker.email = if (jsonobject.has("email")) jsonobject.getString("email") else ""
                gebruiker.name = if (jsonobject.has("name")) jsonobject.getString("name") else ""
                gebruiker.telnr = if (jsonobject.has("telnr")) jsonobject.getString("telnr") else ""
                gebruiker.groepsnr = if (jsonobject.has("groepnr")) jsonobject.getInt("groepnr") else 0
                gebruiker.sessieId = if (jsonobject.has("sessieid")) jsonobject.getInt("sessieid") else 1
            }
        })
        return gebruiker
    }

    /**
     * Oefeningen van sessie ophalen
     */
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

                    if(groepen.contains(gebruiker!!.groepsnr.toString())) {
                        val oefening: Oefening = Oefening(oefeningenId, naam, beschrijving, sessieid, fileUrl, fileMimeType, groepen)
                        oefeningen.add(oefening)
                    }
                }
            }
        })
        return oefeningen
    }

    /**
     * Terugkeerknop
     */
    override fun onBackPressed() {
        super.onBackPressed()
    }

    /**
     * gegevens van de gebruiker bewerken
     */
    fun veranderGegevensGebruiker(gebruikersnaam : String, regio : String, telnr : String) {
        gebruiker!!.name = gebruikersnaam
        gebruiker!!.regio = regio
        gebruiker!!.telnr = telnr
    }

    /**
     * gegevens van de gebruiker opslaan
     */
    fun gegevensGebruikerOpslaan(body : FormBody, url : String) : String {
        var response2 : String? = null
        val thread = Thread(Runnable {
            val mediaType: MediaType? = MediaType.parse("application/json; charset=utf-8")
            val client: OkHttpClient = OkHttpClient()
            val request: Request = Request.Builder().url(url).put(body).build()
            val response = client.newCall(request).execute()
            response2 = response.body().toString()
        })
        thread.start()
        getAangemeldeGebruiker()
        return response2.orEmpty()
    }

    /**
     * Opent menu om naar FragmentProfiel te gaan of uit te loggen
     */
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
                supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.fragment_holder_main, FragmentProfiel(), "pageContent")
                        .addToBackStack(BACK_STACK_ROOT_TAG)
                        .commit()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Slaat de feedback op
     */
    fun postFeedback(url: String, body:FormBody): String {
        var response2 : String? = null
        val thread = Thread(Runnable {
            val mediaType: MediaType? = MediaType.parse("application/json; charset=utf-8")
            val client: OkHttpClient = OkHttpClient()
            //val body: RequestBody = RequestBody.create(mediaType, json)
            val request: Request = Request.Builder().url(url).post(body).build()
            val response = client.newCall(request).execute()
            response2 = response.body().toString()
        })
        thread.start()
        return response2.orEmpty()
    }


    /**
     * Voegt de unlockte sessie toe aan de gebruiker
     */
    fun sessieUnlocked() {
        gebruiker!!.sessieId += 1
        val fromBodyBuilder = FormBody.Builder()
        fromBodyBuilder.add("name", gebruiker!!.name)
        fromBodyBuilder.add("regio", gebruiker!!.regio)
        fromBodyBuilder.add("telnr", gebruiker!!.telnr)
        fromBodyBuilder.add("uid", gebruiker!!.uid)
        fromBodyBuilder.add("email", gebruiker!!.email)
        fromBodyBuilder.add("groepnr", gebruiker!!.groepsnr.toString())
        fromBodyBuilder.add("sessieid", gebruiker!!.sessieId.toString())
        var url = "http://141.134.155.219:3000/users/" + gebruiker!!.uid
        gegevensGebruikerOpslaan(fromBodyBuilder.build(), url)
    }
}