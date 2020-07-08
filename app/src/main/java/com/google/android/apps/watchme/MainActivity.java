/*
 * Copyright (c) 2014 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.apps.watchme;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import androidx.annotation.NonNull;

import com.android.volley.toolbox.ImageLoader;
import com.google.android.apps.watchme.util.EventData;
import com.google.android.apps.watchme.util.NetworkSingleton;
import com.google.android.apps.watchme.util.Utils;
import com.google.android.apps.watchme.util.YouTubeApi;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.youtube.YouTube;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * @author Ibrahim Ulukaya <ulukaya@google.com>
 *         <p/>
 *         Main activity class which handles authorization and intents.
 */
public class MainActivity extends Activity implements
        EventsListFragment.Callbacks {
    public static final String ACCOUNT_KEY = "AIzaSyB-rQ6A4lFhIBdmcavwYGBue0LiQyH5mkc";
    public static final String APP_NAME = "WatchMe";
    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 0;
    private static final int REQUEST_GMS_ERROR_DIALOG = 1;
    private static final int REQUEST_ACCOUNT_PICKER = 2;
    private static final int REQUEST_AUTHORIZATION = 3;
    private static final int REQUEST_STREAMER = 4;
    final HttpTransport transport = AndroidHttp.newCompatibleTransport();
    final JsonFactory jsonFactory = new GsonFactory();
    GoogleAccountCredential credential;
    private String mChosenAccountName;
    private ImageLoader mImageLoader;
    private EventsListFragment mEventsListFragment;
    public static DatabaseReference mdbr;
    public static String accName="default";
    public static String key="123";
    public static String stTime;


    public static String getDateTimePT(){
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
        String date = dateFormat.format(new Date());
        return date;
    }

    public static boolean dateIsThere=false;
    public static int prCost=0;
    public static int z=0;
    public static void fetch(){
        Log.i("date fetch","called");
        final String tDate=getDateTimePT().substring(0,10);
        Log.i("print",tDate);
        DatabaseReference db=mdbr.child("QuotaDayWise").child(accName);
        db.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.i("on","called");
                for(DataSnapshot ds:dataSnapshot.getChildren()){
                    dayWiseQuota q=ds.getValue(dayWiseQuota.class);
                    assert q.stDay!=null;
                    if(q.stDay.equals(tDate)){
                        dateIsThere=true;
                        prCost=q.allottedQuota;
                        break;
                    }
                    else{
                        prCost+=q.quota;
                        z++;
                    }
                }
                Log.i("no of days", String.valueOf(z));
                Log.i("predicted cost is:", String.valueOf(prCost));
                Log.i("date is there:", String.valueOf(dateIsThere));
                if(!dateIsThere)
                {
                    //predict();
                    if (z != 0)
                        prCost = prCost / z;
                    else
                        prCost = 120;
                    if (prCost < 90)
                        prCost = 90;
                    else if (prCost > 120)
                        prCost = 120;
                    Log.i("inside","if");
                    Log.i("no of days", String.valueOf(z));
                    Log.i("predicted cost is:", String.valueOf(prCost));
                    dayWiseQuota d=new dayWiseQuota(getDateTimePT().substring(0,10),0);
                    d.addQuota(d);
            /*db = mdbr.child("QuotaDayWise").child(accName);
            db.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    z = 0;
                    prCost = 0;
                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        dayWiseQuota q = ds.getValue(dayWiseQuota.class);
                        assert q != null;
                        if (q.quota != 0 && !q.stDay.equals(getDateTimePT().substring(0, 10))) {
                            prCost += q.quota;
                            z++;
                        }
                    }
                    // update the extraQuota database.
                    //extraQuota w = new extraQuota(stTime.substring(0, 10), 120 - prCost);
                    //w.addQuota(w);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.i("predict function", "failed");
                }
            });*/
                }
                else
                {
                    // assign quota to prCost.
                    Log.i("predict not","called");
                    Log.i("allotted quota ", String.valueOf(prCost));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.i("date check","failed");
            }
        });
    }

    public static boolean remain=false;
    public static int remainQuota=0;
    public static ArrayList<String> userNames=new ArrayList<>();
    public static void fetchR(){
        final String tDate=getDateTimePT().substring(0,10);
        DatabaseReference db=mdbr.child("remainingQuota");
        db.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot ds:dataSnapshot.getChildren()){
                    extraQuota q=ds.getValue(extraQuota.class);
                    if(q.date.equals(tDate)){
                        remain=true;
                        break;
                    }
                }
                if(!remain){
                    // call all users names.
                    Log.i("remain","false");
                    fetchUsers();
                    extraQuota q=new extraQuota(tDate,1000+remainQuota);
                    q.addQuota(q);
                    Log.i("already","added");
                }
                else{
                    Log.i("remain","true");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.i("fetchR","cancelled");
            }
        });
    }

    public static void fetchUsers(){
        DatabaseReference db1=mdbr.child("QuotaDayWise");
        db1.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.i("on","call");
                for(DataSnapshot ds:dataSnapshot.getChildren()){
                    String asd=ds.getKey();
                    userNames.add(asd);
                    assert asd != null;
                    Log.i("jsd",asd);
                }
                remainQuota=0;
                Log.i("size", String.valueOf(userNames.size()));
                for(int i=0;i<userNames.size();i++){
                    Log.i("users are",userNames.get(i));
                    predict(userNames.get(i));
                }
                Log.i("remQuota", String.valueOf(remainQuota));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.i("user names","cancelled");
            }
        });
    }

    public static void predict(String aName){
        Log.i("predict","called");
        DatabaseReference db = mdbr.child("QuotaDayWise").child(aName);
        db.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int z = 0;
                int pr = 0;
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    dayWiseQuota q = ds.getValue(dayWiseQuota.class);
                    assert q != null;
                    if (q.quota != 0 && !q.stDay.equals(getDateTimePT().substring(0, 10))) {
                        pr += q.quota;
                        z++;
                    }
                }
                if (z != 0)
                    pr = pr / z;
                else
                    pr = 120;
                if (pr < 90)
                    pr = 90;
                else if (pr > 120)
                    pr = 120;
                remainQuota+=(120-pr);
                Log.i("predicted cost is:", String.valueOf(pr));
                extraQuota q=new extraQuota(getDateTimePT().substring(0, 10),1000+remainQuota);
                q.addQuota(q);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.i("predict function", "failed");
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        mdbr = FirebaseDatabase.getInstance().getReference();
        stTime=getDateTimePT();
        getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ensureLoader();

        credential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(Utils.SCOPES));
        // set exponential backoff policy
        credential.setBackOff(new ExponentialBackOff());

        if (savedInstanceState != null) {
            Log.i("save!=null","called");
            mChosenAccountName = savedInstanceState.getString(ACCOUNT_KEY);
            accName=mChosenAccountName;
            accName=accName.replace('.',',');
            key=mdbr.child("users").child(accName).push().getKey();
            Log.i("after ",accName);
        } else {
            loadAccount();
            Log.i("load1","called");
            accName=mChosenAccountName;
            if( accName!=null) {
                Log.i("after load1",accName);
                accName = accName.replace('.', ',');
                key=mdbr.child("users").child(accName).push().getKey();
                fetch();
                //if(!dateIsThere)
                    //predict();
                fetchR();
                YouTubeApi.fetch1(accName,stTime.substring(0,10));
            }
        }

        credential.setSelectedAccountName(mChosenAccountName);

        mEventsListFragment = (EventsListFragment) getFragmentManager()
                .findFragmentById(R.id.list_fragment);

    }

    public void startStreaming(EventData event) {
        String broadcastId = event.getId();
        new StartEventTask().execute(broadcastId);
        Intent intent = new Intent(getApplicationContext(),
                StreamerActivity.class);
        intent.putExtra(YouTubeApi.RTMP_URL_KEY, event.getIngestionAddress());
        intent.putExtra(YouTubeApi.BROADCAST_ID_KEY, broadcastId);

        startActivityForResult(intent, REQUEST_STREAMER);

    }

    private void getLiveEvents() {
        if (mChosenAccountName == null) {
            return;
        }
        new GetLiveEventsTask().execute();
    }

    public void createEvent(View view) {
        new CreateLiveEventTask().execute();
    }

    private void ensureLoader() {
        if (mImageLoader == null) {
            // Get the ImageLoader through your singleton class.
            mImageLoader = NetworkSingleton.getInstance(this).getImageLoader();
        }
    }

    private void loadAccount() {
        Log.i("load","called");
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this);
        mChosenAccountName = sp.getString(ACCOUNT_KEY, null);
        invalidateOptionsMenu();
    }

    private void saveAccount() {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this);
        sp.edit().putString(ACCOUNT_KEY, mChosenAccountName).apply();
    }

    private void loadData() {
        if (mChosenAccountName == null) {
            return;
        }
        getLiveEvents();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                loadData();
                break;
            case R.id.menu_accounts:
                chooseAccount();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_GMS_ERROR_DIALOG:
                break;
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode == Activity.RESULT_OK) {
                    haveGooglePlayServices();
                } else {
                    checkGooglePlayServicesAvailable();
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode != Activity.RESULT_OK) {
                    chooseAccount();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == Activity.RESULT_OK && data != null
                        && data.getExtras() != null) {
                    String accountName = data.getExtras().getString(
                            AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        mChosenAccountName = accountName;
                        accName=mChosenAccountName;
                        Log.i("accountName",mChosenAccountName);
                        accName=accName.replace('.',',');
                        Log.i("accountName",accName);
                        key=mdbr.child("users").child(accName).push().getKey();
                        //check user is allotted quota or not.if not call predict function and assign quota.
                        fetch();
                        /*if(dateIsThere==false)
                        {
                            //call predict fn;
                            predict();
                        }*/
                        fetchR();
                        YouTubeApi.fetch1(accName,stTime.substring(0,10));
                        credential.setSelectedAccountName(accountName);
                        saveAccount();
                    }
                }
                break;
            case REQUEST_STREAMER:
                if (resultCode == Activity.RESULT_OK && data != null
                        && data.getExtras() != null) {
                    String broadcastId = data.getStringExtra(YouTubeApi.BROADCAST_ID_KEY);
                    if (broadcastId != null) {
                        new EndEventTask().execute(broadcastId);
                    }
                }
                break;

        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ACCOUNT_KEY, mChosenAccountName);
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*accName=mChosenAccountName;
        if(accName!=null) {
            accName=accName.replace('.', ',');
            key = mdbr.child("users").child(accName).push().getKey();
        }*/
        Log.i("onResume","called");
    }

    @Override
    public void onConnected(String connectedAccountName) {
        // Make API requests only when the user has successfully signed in.
        //loadData();
    }

    public void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        runOnUiThread(new Runnable() {
            public void run() {
                Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
                        connectionStatusCode, MainActivity.this,
                        REQUEST_GOOGLE_PLAY_SERVICES);
                dialog.show();
            }
        });
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     */
    private boolean checkGooglePlayServicesAvailable() {
        final int connectionStatusCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
            return false;
        }
        return true;
    }

    private void haveGooglePlayServices() {
        // check if there is already an account selected
        if (credential.getSelectedAccountName() == null) {
            // ask user to choose account
            chooseAccount();
        }
    }

    private void chooseAccount() {
        startActivityForResult(credential.newChooseAccountIntent(),
                REQUEST_ACCOUNT_PICKER);
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    public ImageLoader onGetImageLoader() {
        ensureLoader();
        return mImageLoader;
    }

    @Override
    public void onEventSelected(EventData liveBroadcast) {
        startStreaming(liveBroadcast);
    }

    private class GetLiveEventsTask extends
            AsyncTask<Void, Void, List<EventData>> {
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(MainActivity.this, null,
                    getResources().getText(R.string.loadingEvents), true);
        }

        @Override
        protected List<EventData> doInBackground(
                Void... params) {
            YouTube youtube = new YouTube.Builder(transport, jsonFactory,
                    credential).setApplicationName(APP_NAME)
                    .build();
            try {
                return YouTubeApi.getLiveEvents(youtube);
            } catch (UserRecoverableAuthIOException e) {
                startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
            } catch (IOException e) {
                Log.e(MainActivity.APP_NAME, "", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(
                List<EventData> fetchedEvents) {
            if (fetchedEvents == null) {
                // add progress dialog or alert box for 10 sec to say please try again.
                ProgressDialog qwerty=ProgressDialog.show(MainActivity.this,null,"try again once");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                qwerty.dismiss();
                progressDialog.dismiss();
                return;
            }

            mEventsListFragment.setEvents(fetchedEvents);
            progressDialog.dismiss();
        }
    }

    private class CreateLiveEventTask extends
            AsyncTask<Void, Void, List<EventData>> {
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(MainActivity.this, null,
                    getResources().getText(R.string.creatingEvent), true);
        }

        @Override
        protected List<EventData> doInBackground(
                Void... params) {
            YouTube youtube = new YouTube.Builder(transport, jsonFactory,
                    credential).setApplicationName(APP_NAME)
                    .build();
            try {
                String date = new Date().toString();
                YouTubeApi.createLiveEvent(youtube, "Event - " + date,
                        "A live streaming event - " + date);
                //return YouTubeApi.getLiveEvents(youtube);

            } /*catch (UserRecoverableAuthIOException e) {
                startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
            } catch (IOException e) {
                Log.e(MainActivity.APP_NAME, "", e);
            }*/
            finally {
                Log.i("finally","call");
            }
            return null;
        }

        @Override
        protected void onPostExecute(
                List<EventData> fetchedEvents) {

            Button buttonCreateEvent = (Button) findViewById(R.id.create_button);
            buttonCreateEvent.setEnabled(true);

            progressDialog.dismiss();
        }
    }

    private class StartEventTask extends AsyncTask<String, Void, Void> {
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(MainActivity.this, null,
                    getResources().getText(R.string.startingEvent), true);
        }

        @Override
        protected Void doInBackground(String... params) {
            YouTube youtube = new YouTube.Builder(transport, jsonFactory,
                    credential).setApplicationName(APP_NAME)
                    .build();
            try {
                YouTubeApi.startEvent(youtube, params[0]);
            } catch (UserRecoverableAuthIOException e) {
                startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
            } catch (IOException e) {
                Log.e(MainActivity.APP_NAME, "", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            progressDialog.dismiss();
        }

    }

    private class EndEventTask extends AsyncTask<String, Void, Void> {
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(MainActivity.this, null,
                    getResources().getText(R.string.endingEvent), true);
        }

        @Override
        protected Void doInBackground(String... params) {
            YouTube youtube = new YouTube.Builder(transport, jsonFactory,
                    credential).setApplicationName(APP_NAME)
                    .build();
            try {
                if (params.length >= 1) {
                    YouTubeApi.endEvent(youtube, params[0]);
                }
            } catch (UserRecoverableAuthIOException e) {
                startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
            } catch (IOException e) {
                Log.e(MainActivity.APP_NAME, "", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            progressDialog.dismiss();
        }
    }
}
