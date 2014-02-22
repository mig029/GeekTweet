package com.migliori.start.geektweek;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

import static android.R.layout.simple_list_item_1;

public class MainActivity extends FragmentActivity{
    private static Twitter twitter;
    Button logoutButton;
    Button updateStatus;
    Button login;
    Thread thread;
    public static boolean loginState = false;
    private static SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
            loginToTwitter();
        }

        mSharedPreferences = getApplicationContext().getSharedPreferences(
                "MyPref", 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean onPrepareOptionsMenu(Menu menu)
    {

        return true;
    }

    /**
     * A placeholder fragment containing a simple view.
     */

    public class PlaceholderFragment extends Fragment  {

        public Twitter latestTweetChecker;
        public List statuses;
        public View rootView;
        public ListView timeline;
        public  ArrayList<String> setTimeLine;
        public Button timeLineButton;
        public PlaceholderFragment() {


        }

        EditText statusText;
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            rootView = inflater.inflate(R.layout.fragment_main, container, false);
            ///login = (Button) rootView.findViewById(R.id.btnLoginToTwitter);
            timeLineButton = (Button) rootView.findViewById(R.id.refreshTimeline);
            statusText = (EditText) rootView.findViewById(R.id.statusText);
            /*
            login.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    loginToTwitter();
                }
            });
            */
            updateStatus = (Button) rootView.findViewById(R.id.updateStatus);
            updateStatus.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view)
                {
                    String status = statusText.getText().toString();
                    if(status.trim().length() > 0){
                        try {


                            twitter.updateStatus(statusText.getText().toString());

                        } catch (TwitterException e) {
                            e.printStackTrace();
                        }
                        statusText.setText("");
                        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(statusText.getWindowToken(), 0);
                        Toast.makeText(getApplicationContext(), "Status Updated", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            timeline = (ListView) rootView.findViewById(R.id.twitFeed);
           // logoutButton = (Button) rootView.findViewById(R.id.btnLogoutTwitter);
            statusText = (EditText) rootView.findViewById(R.id.statusText);
            timeLineButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //loginToTwitter();
                    Toast.makeText(getApplicationContext(), "Timeline Refreshed", Toast.LENGTH_SHORT).show();

                    Twitter twitter = TwitterFactory.getSingleton();
                    List<Status> statuses = null;
                    try {
                        statuses = twitter.getHomeTimeline();

                    } catch (TwitterException e) {
                        e.printStackTrace();
                    }
                    setTimeLine = new ArrayList<String>();
                    System.out.println("Showing home timeline.");
                    for (Status status : statuses) {
                        setTimeLine.add(status.getUser().getName() + ":" +
                                status.getText());
                    }
                    // Create The Adapter with passing ArrayList as 3rd parameter
                    ArrayAdapter<String> arrayAdapter;
                    arrayAdapter = new ArrayAdapter<String>(getActivity().getBaseContext(), simple_list_item_1, setTimeLine);
                    // Set The Adapter
                    timeline.setAdapter(arrayAdapter);


                }
            });




            return rootView;
        }
    }

    private void loginToTwitter() {
        GetRequestTokenTask getRequestTokenTask = new GetRequestTokenTask();
        getRequestTokenTask.execute();
        loginState = true;
    }

    private class GetRequestTokenTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            twitter = TwitterFactory.getSingleton();
            twitter.setOAuthConsumer(
                    getString(R.string.TWITTER_CONSUMER_KEY),
                    getString(R.string.TWITTER_CONSUMER_SECRET));
            try {
                RequestToken requestToken = twitter.getOAuthRequestToken(
                        getString(R.string.TWITTER_CALLBACK_URL));
                launchLoginWebView(requestToken);
            } catch (TwitterException e) {
                e.printStackTrace();
            }
            return null;
        }
    }


    private void launchLoginWebView(RequestToken requestToken) {
        Intent intent = new Intent(this, LoginToTwitter.class);
        intent.putExtra(Consts.AUTHENTICATION_URL_KEY, requestToken.getAuthenticationURL());
        startActivityForResult(intent, Consts.LOGIN_TO_TWITTER_REQUEST);
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Consts.LOGIN_TO_TWITTER_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                getAccessToken(data.getStringExtra(LoginToTwitter.CALLBACK_URL_KEY));
            }
        }
    }


    private void getAccessToken(String callbackUrl) {
        Uri uri = Uri.parse(callbackUrl);
        String verifier = uri.getQueryParameter("oauth_verifier");
        GetAccessTokenTask getAccessTokenTask = new GetAccessTokenTask();
        getAccessTokenTask.execute(verifier);
    }


    private class GetAccessTokenTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... strings) {
            String verifier = strings[0];
            try {
                AccessToken accessToken = twitter.getOAuthAccessToken(verifier);
                Log.d(MainActivity.class.getSimpleName(), accessToken.getToken());
            } catch (Exception e) {

            }
            return null;
        }
    }

    private void logoutFromTwitter() {
        // Clear the shared preferences
        Editor e = mSharedPreferences.edit();
        e.remove(Consts.PREF_KEY_OAUTH_TOKEN);
        e.remove(Consts.PREF_KEY_OAUTH_SECRET);
        e.remove(Consts.PREF_KEY_TWITTER_LOGIN);
        e.commit();

        // After this take the appropriate action
        // I am showing the hiding/showing buttons again
        // You might not needed this code
        logoutButton.setVisibility(View.GONE);
        updateStatus.setVisibility(View.GONE);
        login.setVisibility(View.GONE);
        //lblUpdate.setVisibility(View.GONE);
        // lblUserName.setText("");
        // lblUserName.setVisibility(View.GONE);

        //  btnLoginTwitter.setVisibility(View.VISIBLE);
    }

    public void checkLogin(){
        MenuItem loginS = (MenuItem) findViewById(R.id.action_login);
        MenuItem logoutS = (MenuItem) findViewById(R.id.action_logout);
        if(loginState)
        {
            loginS.setVisible(false);
        }
        else
        {
            logoutS.setVisible(false);
        }
    }

}

/****
 * Junk Code Below
 */

/*
public class MainActivity extends Activity {


    private static Twitter twitter;

    protected static final String AUTHENTICATION_URL_KEY = "AUTHENTICATION_URL_KEY";
    protected static final int LOGIN_TO_TWITTER_REQUEST= 0;
   // Button updateStatus;
    //TextView txtUpdate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btnLoginToTwitter)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        loginToTwitter();
                    }
                });
        //updateStatus = (Button) findViewById(R.id.updateStatus);

        // updateStatus.setOnClickListener(new View.OnClickListener(){
        //    public void onClick(View v){
        //      updateStatus();
        // }

        //});
    }

    private void loginToTwitter() {
        GetRequestTokenTask getRequestTokenTask = new GetRequestTokenTask();
        getRequestTokenTask.execute();
    }
    /*
        private void updateStatus(){
            txtUpdate = (TextView) findViewById(R.id.status);
        }
            String status = txtUpdate.getText().toString();
    /*
            // Check for blank text
            if (status.trim().length() > 0) {
                // update status
                new updateTwitterStatus().execute(status);
            } else {
                // EditText is empty
                Toast.makeText(getApplicationContext(),
                        "Please enter status message", Toast.LENGTH_SHORT)
                        .show();

        }
    */
/*
    private class GetRequestTokenTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            twitter = TwitterFactory.getSingleton();
            twitter.setOAuthConsumer(
                    getString(R.string.TWITTER_CONSUMER_KEY),
                    getString(R.string.TWITTER_CONSUMER_SECRET));

            try {
                RequestToken requestToken = twitter.getOAuthRequestToken(
                        getString(R.string.TWITTER_CALLBACK_URL));
                launchLoginWebView(requestToken);
            } catch (TwitterException e) {
                e.printStackTrace();
            }
            return null;
        }
    }


    private void launchLoginWebView(RequestToken requestToken) {
        Intent intent = new Intent(this, LoginToTwitter.class);
        intent.putExtra(AUTHENTICATION_URL_KEY, requestToken.getAuthenticationURL());
        startActivityForResult(intent, LOGIN_TO_TWITTER_REQUEST);
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == LOGIN_TO_TWITTER_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                getAccessToken(data.getStringExtra(LoginToTwitter.CALLBACK_URL_KEY));
            }
        }
    }


    private void getAccessToken(String callbackUrl) {
        Uri uri = Uri.parse(callbackUrl);
        String verifier = uri.getQueryParameter("oauth_verifier");

        GetAccessTokenTask getAccessTokenTask = new GetAccessTokenTask();
        getAccessTokenTask.execute(verifier);
    }


    private class GetAccessTokenTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... strings) {
            String verifier = strings[0];
            try {
                AccessToken accessToken = twitter.getOAuthAccessToken(verifier);
                Log.d(MainActivity.class.getSimpleName(), accessToken.getToken());
            } catch (Exception e) {

            }
            return null;
        }
    }





}
*/