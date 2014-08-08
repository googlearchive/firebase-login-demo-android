package com.firebase.samples.simplelogindemo;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.Button;
import android.widget.TextView;
import com.facebook.Session;

import com.facebook.SessionState;
import com.facebook.widget.LoginButton;
import com.firebase.client.Firebase;
import com.firebase.simplelogin.FirebaseSimpleLoginError;
import com.firebase.simplelogin.FirebaseSimpleLoginUser;
import com.firebase.simplelogin.SimpleLogin;
import com.firebase.simplelogin.SimpleLoginAuthenticatedHandler;
import com.firebase.simplelogin.enums.Provider;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;

import java.io.IOException;


public class MainActivity extends ActionBarActivity implements
        View.OnClickListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        SimpleLoginAuthenticatedHandler {

    /***************************************
     *               GENERAL               *
     ***************************************/
    /* TextView that is used to display information about the logged in user */
    private TextView mLoggedInStatusTextView;

    /* The SimpleLogin that is used to authenticate the user with Firebase */
    private SimpleLogin mSimpleLogin;

    /* A dialog that is presented until the Firebase authentication finished. */
    private ProgressDialog mAuthProgressDialog;

    /* The Firebase user that is currently authenticated */
    private FirebaseSimpleLoginUser mAuthenticatedUser;

    /* A tag that is used for logging statements */
    private static final String TAG = "SimpleLoginDemo";

    /***************************************
     *              FACEBOOK               *
     ***************************************/
    /* The login button for Facebook */
    private LoginButton mFacebookLoginButton;

    /***************************************
     *               GOOGLE                *
     ***************************************/
    /* Request code used to invoke sign in user interactions for Google+ */
    private static final int RC_GOOGLE_LOGIN = 1;

    /* Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient;

    /* A flag indicating that a PendingIntent is in progress and prevents us from starting further intents. */
    private boolean mGoogleIntentInProgress;

    /* Track whether the sign-in button has been clicked so that we know to resolve all issues preventing sign-in
     * without waiting. */
    private boolean mGoogleLoginClicked;

    /* Store the connection result from onConnectionFailed callbacks so that we can resolve them when the user clicks
     * sign-in. */
    private ConnectionResult mGoogleConnectionResult;

    /* The login button for Google */
    private SignInButton mGoogleLoginButton;

    /***************************************
     *              ANONYMOUSLY            *
     ***************************************/
    private Button mAnonymousLoginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /* Load the view and display it */
        setContentView(R.layout.activity_main);

        /***************************************
         *              FACEBOOK               *
         ***************************************/
        /* Load the Facebook login button and set up the session callback */
        mFacebookLoginButton = (LoginButton)findViewById(R.id.login_with_facebook);
        mFacebookLoginButton.setSessionStatusCallback(new Session.StatusCallback() {
            @Override
            public void call(Session session, SessionState state, Exception exception) {
                onFacebookSessionStateChange(session, state, exception);
            }
        });

        /***************************************
         *               GOOGLE                *
         ***************************************/
        /* Load the Google login button */
        mGoogleLoginButton = (SignInButton)findViewById(R.id.login_with_google);
        mGoogleLoginButton.setOnClickListener(this);
        /* Setup the Google API object to allow Google+ logins */
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .build();

        /***************************************
         *              ANONYMOUSLY            *
         ***************************************/
        /* Load and setup the anonymous login button */
        mAnonymousLoginButton = (Button)findViewById(R.id.login_anonymously);
        mAnonymousLoginButton.setOnClickListener(this);

        /***************************************
         *               GENERAL               *
         ***************************************/
        mLoggedInStatusTextView = (TextView)findViewById(R.id.login_status);

        /* Create the SimpleLogin class that is used for all authentication with Firebase */
        String firebaseUrl = getResources().getString(R.string.firebase_url);
        mSimpleLogin = new SimpleLogin(new Firebase(firebaseUrl), getApplicationContext());

        /* Setup the progress dialog that is displayed later when authenticating with Firebase */
        mAuthProgressDialog = new ProgressDialog(this);
        mAuthProgressDialog.setTitle("Loading");
        mAuthProgressDialog.setMessage("Authenticating with Firebase...");
        mAuthProgressDialog.setCancelable(false);
        mAuthProgressDialog.show();

        /* Check if the user is authenticated with Firebase already. If this is the case we can set the authenticated
         * user and hide hide any login buttons */
        mSimpleLogin.checkAuthStatus(new SimpleLoginAuthenticatedHandler() {
            @Override
            public void authenticated(FirebaseSimpleLoginError error, FirebaseSimpleLoginUser user) {
                if (error != null) {
                    /* This indicates an error, we ignore it for now */
                    Log.e(TAG, "Error checking if user is authenticated: " + error);
                } else if (user == null) {
                    /* The user is not authenticated, nothing to do */
                    Log.i(TAG, "User is not authenticated!");
                } else {
                    /* The user is authenticated, set the currently authenticated user and hide the login buttons */
                    Log.i(TAG, "User is authenticated in: " + user);
                    setAuthenticatedUser(user);
                }
                /* In any case we have our result hide the authentication progress dialog */
                mAuthProgressDialog.hide();
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();

        /* Disconnect from Google API when Activity stops */
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        /* This was a request by the Google API */
        if (requestCode == RC_GOOGLE_LOGIN) {
            if (resultCode != RESULT_OK) {
                mGoogleLoginClicked = false;
            }

            mGoogleIntentInProgress = false;

            if (!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
        } else {
            /* Otherwise, it's probably the request by the Facebook login button, keep track of the session */
            Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /* If a user is currently authenticated, display a logout menu */
        if (this.mAuthenticatedUser != null) {
            getMenuInflater().inflate(R.menu.main, menu);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.login_with_google) {
            mGoogleLoginClicked = true;
            if (!mGoogleApiClient.isConnecting()) {
                if (mGoogleConnectionResult != null) {
                    resolveSignInError();
                } else if (mGoogleApiClient.isConnected()) {
                    getGoogleOAuthTokenAndLogin();
                } else {
                    /* connect API now */
                    Log.d(TAG, "Trying to connect to Google API");
                    mGoogleApiClient.connect();
                }
            }
        } else if (view.getId() == R.id.login_anonymously) {
            /* Login anonymously */
            loginAnonymously();
        }
    }

    private void setAuthenticatedUser(FirebaseSimpleLoginUser user) {
        if (user != null) {
            /* Hide all the login buttons */
            mFacebookLoginButton.setVisibility(View.GONE);
            mGoogleLoginButton.setVisibility(View.GONE);
            mAnonymousLoginButton.setVisibility(View.GONE);
            mLoggedInStatusTextView.setVisibility(View.VISIBLE);
            /* show a provider specific status text */
            String name;
            switch (user.getProvider()) {
                case FACEBOOK:
                    name = (String)user.getThirdPartyUserData().get("name");
                    mLoggedInStatusTextView.setText("Logged in as " + name + " (Facebook)");
                    break;
                case GOOGLE:
                    name = (String)user.getThirdPartyUserData().get("name");
                    mLoggedInStatusTextView.setText("Logged in as " + name + " (Google+)");
                    break;
                case ANONYMOUS:
                    name = user.getUserId();
                    mLoggedInStatusTextView.setText("Logged in anonymously with temporary user id " + name);
                    break;
                default:
                    mLoggedInStatusTextView.setText("Logged in with unknown provider");
                    break;
            }
        } else {
            /* No authenticated user show all the login buttons */
            mFacebookLoginButton.setVisibility(View.VISIBLE);
            mGoogleLoginButton.setVisibility(View.VISIBLE);
            mAnonymousLoginButton.setVisibility(View.VISIBLE);
            mLoggedInStatusTextView.setVisibility(View.GONE);
        }
        this.mAuthenticatedUser = user;
        /* invalidate options menu to hide/show the logout button */
        supportInvalidateOptionsMenu();
    }

    private void logout() {
        if (this.mAuthenticatedUser != null) {
            /* logout of Firebase */
            mSimpleLogin.logout();
            /* Logout of any of the Frameworks. This step is optional, but ensures the user is not logged into
             * Facebook/Google+ after logging out of Firebase. */
            if (this.mAuthenticatedUser.getProvider() == Provider.FACEBOOK) {
                /* Logout from Facebook */
                Session session = Session.getActiveSession();
                if (session != null) {
                    if (!session.isClosed()) {
                        session.closeAndClearTokenInformation();
                    }
                } else {
                    session = new Session(getApplicationContext());
                    Session.setActiveSession(session);
                    session.closeAndClearTokenInformation();
                }
            } else if (this.mAuthenticatedUser.getProvider() == Provider.GOOGLE) {
                /* Logout from Google+ */
                if (mGoogleApiClient.isConnected()) {
                    Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
                    mGoogleApiClient.disconnect();
                }
            }
            /* Update authenticated user and show login buttons */
            setAuthenticatedUser(null);
        }
    }

    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @Override
    public void authenticated(FirebaseSimpleLoginError error, FirebaseSimpleLoginUser user) {
        if (error != null) {
            // There was an error
            Log.e(TAG, "Error logging in: " + error);
            showErrorDialog("An error occurred while authenticating with Firebase: " + error.getMessage());
        } else {
            Log.i(TAG, "Logged in with user: " + user);
            setAuthenticatedUser(user);
        }
        mAuthProgressDialog.hide();
    }


    /***************************************
     *              FACEBOOK               *
     ***************************************/
    /* Handle any changes to the Facebook session */
    private void onFacebookSessionStateChange(Session session, SessionState state, Exception exception) {
        if (state.isOpened()) {
            /* We were authenticated with Facebook, use that to authenticate with Firebase */
            final String appId = getResources().getString(R.string.facebook_app_id);
            mAuthProgressDialog.show();
            mSimpleLogin.loginWithFacebook(appId, session.getAccessToken(), this);
        } else if (state.isClosed()) {
            /* Logged out of Facebook and currently authenticated with Firebase using Facebook, so do a logout */
            if (mAuthenticatedUser != null && this.mAuthenticatedUser.getProvider() == Provider.FACEBOOK) {
                mSimpleLogin.logout();
                setAuthenticatedUser(null);
            }
        }
    }


    /***************************************
     *               GOOGLE                *
     ***************************************/

    /* A helper method to resolve the current ConnectionResult error. */
    private void resolveSignInError() {
        if (mGoogleConnectionResult.hasResolution()) {
            try {
                mGoogleIntentInProgress = true;
                mGoogleConnectionResult.startResolutionForResult(this, RC_GOOGLE_LOGIN);
            } catch (IntentSender.SendIntentException e) {
                // The intent was canceled before it was sent.  Return to the default
                // state and attempt to connect to get an updated ConnectionResult.
                mGoogleIntentInProgress = false;
                mGoogleApiClient.connect();
            }
        }
    }

    private void getGoogleOAuthTokenAndLogin() {
        mAuthProgressDialog.show();
        /* Get OAuth token in Background */
        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
            String errorMessage = null;
            @Override
            protected String doInBackground(Void... params) {
                String token = null;

                try {
                    String scope = String.format("oauth2:%s", Scopes.PLUS_LOGIN);
                    token = GoogleAuthUtil.getToken(MainActivity.this, Plus.AccountApi.getAccountName(mGoogleApiClient), scope);
                } catch (IOException transientEx) {
                    /* Network or server error */
                    Log.e(TAG, "Error authenticating with Google: " + transientEx);
                    errorMessage = "Network error: " + transientEx.getMessage();
                } catch (UserRecoverableAuthException e) {
                    Log.w(TAG, "Recoverable Google OAuth error: " + e.toString());
                    /* We probably need to ask for permissions, so start the intent if there is none pending */
                    if (!mGoogleIntentInProgress) {
                        mGoogleIntentInProgress = true;
                        Intent recover = e.getIntent();
                        startActivityForResult(recover, RC_GOOGLE_LOGIN);
                    }
                } catch (GoogleAuthException authEx) {
                    /* The call is not ever expected to succeed assuming you have already verified that
                     * Google Play services is installed. */
                    Log.e(TAG, "Error authenticating with Google: " + authEx.getMessage(), authEx);
                    errorMessage = "Error authenticating with Google: " + authEx.getMessage();
                }

                return token;
            }

            @Override
            protected void onPostExecute(String token) {
                mGoogleLoginClicked = false;
                if (token != null) {
                    /* Successfully got OAuth token, now login with Google */
                    mSimpleLogin.loginWithGoogle(token, MainActivity.this);
                } else if (errorMessage != null) {
                    mAuthProgressDialog.hide();
                    showErrorDialog(errorMessage);
                }
            }
        };
        task.execute();
    }

    @Override
    public void onConnected(final Bundle bundle) {
        /* Connected with Google API, use this to authenticate with Firebase */
        getGoogleOAuthTokenAndLogin();
    }


    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (!mGoogleIntentInProgress) {
            /* Store the ConnectionResult so that we can use it later when the user clicks on the Google+ login button */
            mGoogleConnectionResult = result;

            if (mGoogleLoginClicked) {
                /* The user has already clicked login so we attempt to resolve all errors until the user is signed in,
                 * or they cancel. */
                resolveSignInError();
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        // ignore
    }

    /***************************************
     *              ANONYMOUSLY            *
     ***************************************/
    private void loginAnonymously() {
        mAuthProgressDialog.show();
        mSimpleLogin.loginAnonymously(this);
    }
}
