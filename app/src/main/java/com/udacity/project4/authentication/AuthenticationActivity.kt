package com.udacity.project4.authentication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */

class AuthenticationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthenticationBinding
    private val viewModel: AuthenticationViewModel by viewModels()


    companion object {
        const val TAG = "MainFragment"
        const val SIGN_IN_RESULT_CODE = 1001
        const val SIGN_IN_REQUEST_CODE = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthenticationBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

//    Implement the create account and sign in using FirebaseUI, use sign in using email and sign in using Google
        binding.authButton.setOnClickListener {
            //  call launchSignInFlow when authButton is clicked
            launchSignInFlow()
        }

        observeAuthenticationState()


//          TODO: a bonus is to customize the sign in flow to look nice using :
        //https://github.com/firebase/FirebaseUI-Android/blob/master/auth/README.md#custom-layout

    }


    private fun observeAuthenticationState() {
        //  If the user was authenticated, send him to RemindersActivity

        viewModel.authenticationState.observe(this, Observer { authenticationState ->
            when (authenticationState) {
                //If user is authenticated
                AuthenticationViewModel.AuthenticationState.AUTHENTICATED -> {
                    val intent = Intent(this, RemindersActivity::class.java)
                    startActivity(intent)

                    binding.authButton.setOnClickListener {
                        goToRemindersActivity()
                    }
                    Log.i("onCreate", "User is Authenticated")
                }
                else -> {
                    Log.i("onCreate", "User is not Authenticated")
                    binding.authButton.setOnClickListener {
                        launchSignInFlow()
                    }
                }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // TODO Listen to the result of the sign in process by filter for when
        //  SIGN_IN_REQUEST_CODE is passed back. Start by having log statements to know
        //  whether the user has signed in successfully
        if (requestCode == SIGN_IN_REQUEST_CODE) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                // User successfully signed in
                Log.i(
                    TAG,
                    "Successfully signed in user from authenti" +
                            "${FirebaseAuth.getInstance().currentUser?.displayName}!"
                )
                goToRemindersActivity()
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                Toast.makeText(this, getText(R.string.coudnt_login), Toast.LENGTH_SHORT).show()
                Log.i(
                    TAG,
                    "Sign in unsuccessful ${response?.error?.errorCode}"
                )
             }
        }
    }

    private fun goToRemindersActivity() {
        val intent = Intent(this, RemindersActivity::class.java)
        startActivity(intent)
    }
    private fun launchSignInFlow() {
        // TODO Complete this function by allowing users to register and sign in with
        //  either their email address or Google account.
        // Give users the option to sign in / register with their email or Google account.
        // If users choose to register with their email,
        // they will need to create a password as well.
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(), AuthUI.IdpConfig.GoogleBuilder().build()

            // This is where you can provide more ways for users to register and
            // sign in.
        )

        // Create and launch sign-in intent.
        // We listen to the response of this activity with the
        // SIGN_IN_REQUEST_CODE
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
            AuthenticationActivity.SIGN_IN_REQUEST_CODE
        )

    }
}
