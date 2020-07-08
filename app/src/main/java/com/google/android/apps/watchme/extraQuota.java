package com.google.android.apps.watchme;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class extraQuota {
    public int rQuota;
    public String date;

    public extraQuota(){}
    public extraQuota(String a,int b){
        this.rQuota=b;
        this.date=a;
    }

    public void addQuota(extraQuota q){
        MainActivity.mdbr.child("remainingQuota").child(q.date).setValue(q);
    }

    public void fetch(final String a){
        DatabaseReference db=MainActivity.mdbr.child("remainingQuota");
        db.addValueEventListener(new ValueEventListener() {
            int z =0;
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.i("fetch remaining quota","called");
                for(DataSnapshot ds:dataSnapshot.getChildren()){
                    extraQuota q=ds.getValue(extraQuota.class);
                    if(q.date.equals(a)){
                        z +=  q.rQuota;
                        Log.i("remain fetch", String.valueOf(q.rQuota));
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.i("fetch remaining quota","cancelled");
            }
        });
    }
}
