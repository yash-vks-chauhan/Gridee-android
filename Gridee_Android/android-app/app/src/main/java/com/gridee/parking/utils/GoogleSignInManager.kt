package com.gridee.parking.utils

import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.gridee.parking.R
import androidx.activity.result.ActivityResultLauncher

class GoogleSignInManager(private val activity: Activity) {
    
    private val googleSignInClient: GoogleSignInClient
    
    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(activity.getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()
        
        googleSignInClient = GoogleSignIn.getClient(activity, gso)
    }
    
    /**
     * Launches sign-in and forces the account chooser by revoking the cached session first.
     * revokeAccess() clears the default account selection so the picker is shown every time.
     */
    fun launchSignIn(launcher: ActivityResultLauncher<Intent>) {
        googleSignInClient.revokeAccess().addOnCompleteListener {
            // Fallback: if revoke somehow fails silently, also sign out to clear session.
            googleSignInClient.signOut().addOnCompleteListener {
                launcher.launch(googleSignInClient.signInIntent)
            }
        }
    }
    
    fun handleSignInResult(data: Intent?): GoogleSignInResult {
        return try {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            GoogleSignInResult.Success(account)
        } catch (e: ApiException) {
            GoogleSignInResult.Error("Google sign in failed: ${e.statusCode}")
        }
    }
    
    fun signOut() {
        googleSignInClient.signOut()
    }
    
    fun revokeAccess() {
        googleSignInClient.revokeAccess()
    }
    
    fun isSignedIn(): Boolean {
        return GoogleSignIn.getLastSignedInAccount(activity) != null
    }
}

sealed class GoogleSignInResult {
    data class Success(val account: GoogleSignInAccount) : GoogleSignInResult()
    data class Error(val message: String) : GoogleSignInResult()
}
