package com.groep4.mindfulness.fragments


import android.content.pm.PackageManager
import android.support.design.widget.Snackbar

import android.content.CursorLoader
import android.content.Loader
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.TextView
import java.util.ArrayList
import android.Manifest.permission.READ_CONTACTS
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.TargetApi
import android.app.Activity


import android.content.Intent
import android.os.AsyncTask
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.app.LoaderManager
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import com.groep4.mindfulness.R
import com.groep4.mindfulness.activities.ActivityLogin

import com.groep4.mindfulness.activities.MainActivity
import com.groep4.mindfulness.utils.LoginValidation
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.fragment_login.view.*


class FragmentLogin : Fragment(){

    /**
     * Houd de inlogtaak bij om ervoor te zorgen dat we deze kunnen annuleren als hierom wordt gevraagd.
     */

    lateinit var mAuth: FirebaseAuth

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view:View=inflater!!.inflate(R.layout.fragment_login,container,false)

        // Stel het login formulier in.

        view.password.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })
        view.email
        view.email_sign_in_button.setOnClickListener { attemptLogin() }
        activity!!.tv_register.visibility = View.VISIBLE
        activity!!.tv_register.text = resources.getString(R.string.registreer)
        activity!!.tv_register.isClickable
        return view
    }


    /**
     *Validatie en aanmelden van account
     */
    private fun attemptLogin() {

        // Reset errors.
        view!!.email.error = null
        view!!.password.error = null

        // values
        val emailStr = view!!.email.text.toString()
        val passwordStr = view!!.password.text.toString()

        var cancel = false
        var focusView: View? = null


        // Controleer op een geldig email adres.
        if (!LoginValidation.isValidEmail(view!!.email)) {

            focusView =view!!.email
            cancel = true
        }

        if (!LoginValidation.isValidPassword(view!!.password)) {
            focusView =view!!.password
            cancel = true
        }

        if (cancel) {

            focusView?.requestFocus()

        } else {

            //Aanmelden
            showProgress(true)
            mAuth = FirebaseAuth.getInstance()

            mAuth.signInWithEmailAndPassword(emailStr,passwordStr)
                    .addOnCompleteListener(activity!!){
                    task ->
                    activity!!.tv_register.visibility = View.INVISIBLE


                    if (task.isSuccessful()) {
                        //mainactivity tonen
                        val intent = Intent(activity, MainActivity::class.java)
                        this.startActivity(intent)

                    } else {
                        //showProgress((false))
                        activity!!.tv_register.visibility = View.VISIBLE
                        view!!.password.error = this.getString(R.string.error_incorrect_password)
                        view!!.password.requestFocus()
                    }

            }

            showProgress(false)

        }
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    fun showProgress(show: Boolean) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

            activity!!.login_layout.visibility = if (show) View.GONE else View.VISIBLE
            activity!!.login_layout.animate()
                    .setDuration(shortAnimTime)
                    .alpha((if (show) 0 else 1).toFloat())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            activity!!.login_layout.visibility = if (show) View.GONE else View.VISIBLE
                        }
                    })

            activity!!.login_progress.visibility = if (show) View.VISIBLE else View.GONE
            activity!!.login_progress.animate()
                    .setDuration(shortAnimTime)
                    .alpha((if (show) 1 else 0).toFloat())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            activity!!.login_progress.visibility = if (show) View.VISIBLE else View.GONE
                        }
                    })
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            activity!!.login_progress.visibility = if (show) View.VISIBLE else View.GONE
            activity!!.login_layout.visibility = if (show) View.GONE else View.VISIBLE
        }
    }



}