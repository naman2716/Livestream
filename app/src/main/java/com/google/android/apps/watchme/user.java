package com.google.android.apps.watchme;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class user {
    public String userName;
    public int cost;
    public String st;
    public String end;

    public user(){}
    public user(String a,int b,String c,String d){
        this.end=d;
        this.st=c;
        this.cost=b;
        this.userName=a;
    }
    public user(user s){
        this.cost=s.cost;
        this.userName=s.userName;
        this.end=s.end;
        this.st=s.st;
    }
    public void addSession(user u){
        MainActivity.mdbr.child("users").child(MainActivity.accName).child(MainActivity.key).setValue(u);
    }

    public void fetch(final String name){
        Log.i("fetch","called");
        DatabaseReference db= MainActivity.mdbr.child("users").child(MainActivity.accName);
        db.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int z=0;
                for(DataSnapshot ds:dataSnapshot.getChildren()){
                    String xyz=ds.getKey();
                    assert xyz != null;
                    Log.i("keycheck",xyz);
                    user ch=ds.getValue(user.class);
                    String tDate=MainActivity.getDateTimePT();
                    assert ch != null;
                    String pDate=ch.end;
                    if(tDate.substring(0,10).equals(pDate.substring(0,10))){
                        try {
                            z+=ch.cost;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Log.i("why", String.valueOf(z));
                }
                Log.i("fetched", String.valueOf(z));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.i("fetch for user","failed");
            }
        });
    }
}
