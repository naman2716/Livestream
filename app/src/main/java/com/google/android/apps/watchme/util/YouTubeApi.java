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

package com.google.android.apps.watchme.util;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.apps.watchme.MainActivity;
import com.google.android.apps.watchme.dayWiseQuota;
import com.google.android.apps.watchme.extraQuota;
import com.google.android.apps.watchme.user;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTube.LiveBroadcasts.Transition;
import com.google.api.services.youtube.model.CdnSettings;
import com.google.api.services.youtube.model.IngestionInfo;
import com.google.api.services.youtube.model.LiveBroadcast;
import com.google.api.services.youtube.model.LiveBroadcastContentDetails;
import com.google.api.services.youtube.model.LiveBroadcastListResponse;
import com.google.api.services.youtube.model.LiveBroadcastSnippet;
import com.google.api.services.youtube.model.LiveBroadcastStatus;
import com.google.api.services.youtube.model.LiveStream;
import com.google.api.services.youtube.model.LiveStreamListResponse;
import com.google.api.services.youtube.model.LiveStreamSnippet;
import com.google.api.services.youtube.model.MonitorStreamInfo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class YouTubeApi {

    public static final String RTMP_URL_KEY = "rtmpUrl";
    public static final String BROADCAST_ID_KEY = "broadcastId";
    private static final int FUTURE_DATE_OFFSET_MILLIS = 5 * 1000;
    public static int cost=0;
    public static int dCost=0;
    public static int p=0;


    public static void fetch(final String a) {
        DatabaseReference db = MainActivity.mdbr.child("remainingQuota");
        db.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                p=0;
                Log.i("fetch remaining quota", "called");
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    extraQuota q = ds.getValue(extraQuota.class);
                    if (q.date.equals(a)) {
                        p+=q.rQuota;
                        Log.i("remain fetch", String.valueOf(q.rQuota));
                        break;
                    }
                }
                Log.i("rQuota is", String.valueOf(p));
                if(p>=30) {
                    extraQuota w = new extraQuota(MainActivity.getDateTimePT().substring(0, 10), p - 30);
                    w.addQuota(w);
                    //MainActivity.mdbr.child("remainingQuota").child(w.date).child("rQuota").setValue(p-30);
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {
                        Log.i("did not wait", "for the time");
                    }
                    MainActivity.prCost += 30;
                    MainActivity.mdbr.child("QuotaDayWise").child(MainActivity.accName)
                            .child(w.date).child("allottedQuota").setValue(MainActivity.prCost);
                }
                else
                    Log.i("there is","no enough extra quota available");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.i("fetch remaining quota", "cancelled");
            }
        });
    }

    public static void fetch1(String name, final String day){
        DatabaseReference db=MainActivity.mdbr.child("QuotaDayWise").child(name);
        dCost=0;
        db.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot ds:dataSnapshot.getChildren()){
                    dayWiseQuota d=ds.getValue(dayWiseQuota.class);
                    assert d!=null;
                    if(d.stDay.equals(day))
                    {
                        dCost+=d.quota;
                    }
                }
                Log.i("till now used quota is:", String.valueOf(dCost));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.i("fetch1","cancelled");
            }
        });
        Log.i("dcost", String.valueOf(dCost));
    }


    public static void createLiveEvent(YouTube youtube, String description,
                                       String name) {
        // We need a date that's in the proper ISO format and is in the future,
        // since the API won't
        // create events that start in the past.
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
        long futureDateMillis = System.currentTimeMillis()
                + FUTURE_DATE_OFFSET_MILLIS;
        Date futureDate = new Date();
        futureDate.setTime(futureDateMillis);
        String date = dateFormat.format(futureDate);

        Log.i("date check",MainActivity.stTime.substring(0,10));
        //fetch1(MainActivity.accName,MainActivity.stTime.substring(0,10));
        Log.i("quota used till now", String.valueOf(dCost));
        if((MainActivity.prCost<120)&&((MainActivity.prCost-dCost)<=40))
        {
            // add quota from the remaining quota and deduct it.
            // call fn in extraQuota.java file to deduct quota.
            // and update allotted quota also.
            fetch(MainActivity.getDateTimePT().substring(0,10));
            Log.i("giving","extra quota");
        }
        if(dCost+31>MainActivity.prCost)
        {
            Log.i("you have ","no enough quota try tomorrow");
            return;
        }

        Log.i(MainActivity.APP_NAME, String.format(
                "Creating event: name='%s', description='%s', date='%s'.",
                name, description, date));

        try {

            LiveBroadcastSnippet broadcastSnippet = new LiveBroadcastSnippet();
            broadcastSnippet.setTitle(name);
            broadcastSnippet.setScheduledStartTime(new DateTime(futureDate));

            LiveBroadcastContentDetails contentDetails = new LiveBroadcastContentDetails();
            MonitorStreamInfo monitorStream = new MonitorStreamInfo();
            monitorStream.setEnableMonitorStream(false);
            contentDetails.setMonitorStream(monitorStream);

            // Create LiveBroadcastStatus with privacy status.
            LiveBroadcastStatus status = new LiveBroadcastStatus();
            status.setPrivacyStatus("unlisted");

            LiveBroadcast broadcast = new LiveBroadcast();
            broadcast.setKind("youtube#liveBroadcast");
            broadcast.setSnippet(broadcastSnippet);
            broadcast.setStatus(status);
            broadcast.setContentDetails(contentDetails);

            // Create the insert request
            YouTube.LiveBroadcasts.Insert liveBroadcastInsert = youtube
                    .liveBroadcasts().insert("snippet,status,contentDetails",
                            broadcast);

            // Request is executed and inserted broadcast is returned
            LiveBroadcast returnedBroadcast = liveBroadcastInsert.execute();
            cost+=5;
            dCost+=5;
            String  endr = MainActivity.getDateTimePT();
            user u=new user(MainActivity.accName,cost,MainActivity.stTime,endr);
            u.addSession(u);

            // Create a snippet with title.
            LiveStreamSnippet streamSnippet = new LiveStreamSnippet();
            streamSnippet.setTitle(name);

            // Create content distribution network with format and ingestion
            // type.
            CdnSettings cdn = new CdnSettings();
            cdn.setFormat("240p");
            cdn.setIngestionType("rtmp");

            LiveStream stream = new LiveStream();
            stream.setKind("youtube#liveStream");
            stream.setSnippet(streamSnippet);
            stream.setCdn(cdn);

            // Create the insert request
            YouTube.LiveStreams.Insert liveStreamInsert = youtube.liveStreams()
                    .insert("snippet,cdn", stream);

            // Request is executed and inserted stream is returned
            LiveStream returnedStream = liveStreamInsert.execute();

            cost+=5;
            dCost+=5;
            endr = MainActivity.getDateTimePT();
            u=new user(MainActivity.accName,cost,MainActivity.stTime,endr);
            u.addSession(u);

            // Create the bind request
            YouTube.LiveBroadcasts.Bind liveBroadcastBind = youtube
                    .liveBroadcasts().bind(returnedBroadcast.getId(),
                            "id,contentDetails");

            // Set stream id to bind
            liveBroadcastBind.setStreamId(returnedStream.getId());

            // Request is executed and bound broadcast is returned
            liveBroadcastBind.execute();
            cost+=5;
            dCost+=5;
            endr = MainActivity.getDateTimePT();
            u=new user(MainActivity.accName,cost,MainActivity.stTime,endr);
            u.addSession(u);
            dayWiseQuota dq=new dayWiseQuota(u.st.substring(0,10),dCost);
            dq.addQuota(dq);

        } catch (GoogleJsonResponseException e) {
            System.err.println("GoogleJsonResponseException code: "
                    + e.getDetails().getCode() + " : "
                    + e.getDetails().getMessage());
            e.printStackTrace();

        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
            e.printStackTrace();
        } catch (Throwable t) {
            System.err.println("Throwable: " + t.getStackTrace());
            t.printStackTrace();
        }
    }

    // TODO: Catch those exceptions and handle them here.
    public static List<EventData> getLiveEvents(
            YouTube youtube) throws IOException {
        Log.i("date check",MainActivity.stTime.substring(0,10));
        //fetch1(MainActivity.accName,MainActivity.stTime.substring(0,10));
        Log.i("quota used till now", String.valueOf(dCost));
        if((MainActivity.prCost<120)&&((MainActivity.prCost-dCost)<=40))
        {
            // add quota from the remaining quota and deduct it.
            // call fn in extraQuota.java file to deduct quota.
            // and update allotted quota also.
            fetch(MainActivity.getDateTimePT().substring(0,10));
            Log.i("giving","extra quota");
        }
        if(dCost+8>MainActivity.prCost)
        {
            Log.i("checking", String.valueOf(MainActivity.prCost));
            if(MainActivity.prCost<=120) {
                Log.i("you will be allotted", "extra quota 30 please try again");
            }
            else
                Log.i("you have ","no enough quota try tomorrow");
            return null;
        }
        Log.i(MainActivity.APP_NAME, "Requesting live events.");

        YouTube.LiveBroadcasts.List liveBroadcastRequest = youtube
                .liveBroadcasts().list("id,snippet,contentDetails");
        // liveBroadcastRequest.setMine(true);
        liveBroadcastRequest.setBroadcastStatus("upcoming");

        // List request is executed and list of broadcasts are returned
        LiveBroadcastListResponse returnedListResponse = liveBroadcastRequest.execute();
        //MainActivity.mdbr.child("users").setValue(MainActivity.accName);
        //user w=new user();
        //w.fetch(MainActivity.accName);
        cost+=5;
        dCost+=5;
        String  endr = MainActivity.getDateTimePT();
        user u=new user(MainActivity.accName,cost,MainActivity.stTime,endr);
        u.addSession(u);
        dayWiseQuota dq=new dayWiseQuota(u.st.substring(0,10),dCost);
        dq.addQuota(dq);
        //dayWiseQuota dq=new dayWiseQuota(u.st.substring(0,10),4);
        //dq.fetch(MainActivity.accName,u.st.substring(0,10),dq);


        //MainActivity.mdbr.child("users").setValue(u);

        // Get the list of broadcasts associated with the user.
        List<LiveBroadcast> returnedList = returnedListResponse.getItems();

        List<EventData> resultList = new ArrayList<EventData>(returnedList.size());
        EventData event;

        for (LiveBroadcast broadcast : returnedList) {
            event = new EventData();
            event.setEvent(broadcast);
            String streamId = broadcast.getContentDetails().getBoundStreamId();
            if (streamId != null) {
                String ingestionAddress = getIngestionAddress(youtube, streamId);
                event.setIngestionAddress(ingestionAddress);
            }
            resultList.add(event);
        }
        return resultList;
    }

    public static void startEvent(YouTube youtube, String broadcastId)
            throws IOException {

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Log.e(MainActivity.APP_NAME, "", e);
        }
        int y=0;
        Transition transitionRequest = youtube.liveBroadcasts().transition(
                "live", broadcastId, "status");
        transitionRequest.execute();
        cost+=3;
        y+=2;dCost+=3;
        String  endr = MainActivity.getDateTimePT();
        user u=new user(MainActivity.accName,cost,MainActivity.stTime,endr);
        u.addSession(u);
        dayWiseQuota dq=new dayWiseQuota(u.st.substring(0,10),dCost);
        dq.addQuota(dq);
    }

    public static void endEvent(YouTube youtube, String broadcastId)
            throws IOException {
        int y=0;
        Transition transitionRequest = youtube.liveBroadcasts().transition(
                "complete", broadcastId, "status");
        transitionRequest.execute();
        cost+=3;
        y+=2;dCost+=3;
        String  endr = MainActivity.getDateTimePT();
        user u=new user(MainActivity.accName,cost,MainActivity.stTime,endr);
        u.addSession(u);
        dayWiseQuota dq=new dayWiseQuota(u.st.substring(0,10),dCost);
        dq.addQuota(dq);
    }

    public static String getIngestionAddress(YouTube youtube, String streamId)
            throws IOException {
        int y=0;
        YouTube.LiveStreams.List liveStreamRequest = youtube.liveStreams()
                .list("cdn");
        liveStreamRequest.setId(streamId);
        LiveStreamListResponse returnedStream = liveStreamRequest.execute();
        cost+=3;
        y+=2;dCost+=3;
        String  endr = MainActivity.getDateTimePT();
        user u=new user(MainActivity.accName,cost,MainActivity.stTime,endr);
        u.addSession(u);
        dayWiseQuota dq=new dayWiseQuota(u.st.substring(0,10),dCost);
        dq.addQuota(dq);

        List<LiveStream> streamList = returnedStream.getItems();
        if (streamList.isEmpty()) {
            return "";
        }
        IngestionInfo ingestionInfo = streamList.get(0).getCdn().getIngestionInfo();
        return ingestionInfo.getIngestionAddress() + "/"
                + ingestionInfo.getStreamName();
    }
}
