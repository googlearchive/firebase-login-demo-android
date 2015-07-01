package com.firebase.samples.logindemo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;

/**
 * The TwitterOAuthActivity provides a simple web view for users authenticating with Twitter. To do this authentication,
 * we do the following steps:
 * <p/>
 * 1. Using twitter4j, get the request token, request token secret, and oauth verifier
 * 2. Open a web view for the user to give the application access
 * 3. Using twitter4j, get the authentication token, secret, and user id with a accepted request token
 * 4. Return to the {@link com.firebase.samples.logindemo.MainActivity} with the new access token
 */
public class TwitterOAuthActivity extends Activity {

    private static final String TAG = TwitterOAuthActivity.class.getSimpleName();

    private WebView mTwitterView;

    private Twitter mTwitter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // setup twitter client
        mTwitter = new TwitterFactory(new ConfigurationBuilder()
                .setOAuthConsumerKey(getResources().getString(R.string.twitter_consumer_key))
                .setOAuthConsumerSecret(getResources().getString(R.string.twitter_consumer_secret))
                .build()).getInstance();

        // setup twitter webview
        mTwitterView = new WebView(this);
        mTwitterView.getSettings().setJavaScriptEnabled(true);

        // initialize view
        setContentView(mTwitterView);

        // start the web view
        loginToTwitter();
    }

    private void loginToTwitter() {
        // fetch the oauth request token then prompt the user to authorize the application
        new AsyncTask<Void, Void, RequestToken>() {
            String errorMessage = null;

            @Override
            protected RequestToken doInBackground(Void... params) {
                RequestToken token = null;
                try {
                    token = mTwitter.getOAuthRequestToken("oauth://cb");
                } catch (TwitterException te) {
                    Log.e(TAG, "Error retrieving request token from Twitter: " + te.getMessage(), te);
                    errorMessage = "Error retrieving request token from Twitter: " + te.getMessage();
                }
                return token;
            }

            @Override
            protected void onPostExecute(final RequestToken token) {
                mTwitterView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(final WebView view, final String url) {
                        if (url.startsWith("oauth://cb")) {
                            if (url.contains("oauth_verifier")) {
                                try {
                                    getTwitterOAuthTokenAndLogin(token, Uri.parse(url).getQueryParameter("oauth_verifier"));
                                } catch (UnsupportedOperationException uoe) {
                                    Log.e(TAG, "Uri isn't a hierarchical URI: " + uoe.getMessage(), uoe);
                                    errorMessage = "Uri isn't a hierarchical URI: " + uoe.getMessage();
                                } catch (NullPointerException npe) {
                                    Log.e(TAG, "'url' or key to encode 'oauth_verifier' are null: " + npe.getMessage(), npe);
                                    errorMessage = "'url' or key to encode 'oauth_verifier' are null: " + npe.getMessage();
                                }
                            } else if (url.contains("denied")) {
                                Log.e(TAG, "Access token retrieval denied.");
                                errorMessage = "Access token retrieval denied.";
                                Intent resultIntent = new Intent();
                                resultIntent.putExtra("error", errorMessage);
                                setResult(RESULT_CANCELED, resultIntent);
                                finish();
                            }
                        }
                    }
                });
                if (errorMessage == null) {
                    mTwitterView.loadUrl(token.getAuthorizationURL() + "&force_login=true");
                } else {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("error", errorMessage);
                    setResult(RESULT_CANCELED, resultIntent);
                    Log.e(TAG, "Request token retrieval failed.");
                    finish();
                }
            }
        }.execute();
    }

    private void getTwitterOAuthTokenAndLogin(final RequestToken requestToken, final String oauthVerifier) {
        // once a user authorizes the application, get the auth token and return to the MainActivity
        new AsyncTask<Void, Void, AccessToken>() {
            String errorMessage = null;

            @Override
            protected AccessToken doInBackground(Void... params) {
                AccessToken accessToken = null;
                if (requestToken != null) {
                    try {
                        accessToken = mTwitter.getOAuthAccessToken(requestToken, oauthVerifier);
                    } catch (TwitterException te) {
                        Log.e(TAG, "Error authenticating with Twitter: " + te.getMessage(), te);
                        errorMessage = "Error authenticating with Twitter: " + te.getMessage();
                    }
                }
                return accessToken;
            }

            @Override
            protected void onPostExecute(AccessToken accessToken) {
                Intent resultIntent = new Intent();
                if (errorMessage == null) {
                    resultIntent.putExtra("oauth_token", accessToken.getToken());
                    resultIntent.putExtra("oauth_token_secret", accessToken.getTokenSecret());
                    resultIntent.putExtra("user_id", accessToken.getUserId() + "");
                    setResult(RESULT_OK, resultIntent);
                    Log.i(TAG, "Access token retrieved.");
                } else {
                    resultIntent.putExtra("error", errorMessage);
                    setResult(RESULT_CANCELED, resultIntent);
                    Log.e(TAG, "Access token retrieval failed.");
                }
                finish();
            }
        }.execute();
    }
}
