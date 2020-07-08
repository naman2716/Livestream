package com.google.android.apps.watchme;


import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;


public class dayWiseQuota {
    public String stDay;
    public int quota;
    public int allottedQuota;

    public dayWiseQuota(){}

    public dayWiseQuota(String  a,int b){
        this.quota=b;
        this.stDay=a;
        this.allottedQuota=MainActivity.prCost;// or use with predict function.
    }

    public void addQuota(dayWiseQuota d){
        Log.i("addQuota called", String.valueOf(d.quota));
        Log.i("allot called",String.valueOf(d.allottedQuota));
        MainActivity.mdbr.child("QuotaDayWise").child(MainActivity.accName).child(d.stDay).setValue(d);
    }

    public void fetch(String name, final String day,final dayWiseQuota c){
        DatabaseReference db=MainActivity.mdbr.child("QuotaDayWise").child(name);
        db.addListenerForSingleValueEvent(new ValueEventListener() {
            int z=0;
            dayWiseQuota cx;
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot ds:dataSnapshot.getChildren()){
                    dayWiseQuota q=ds.getValue(dayWiseQuota.class);
                    Log.i("date",q.stDay);
                    Log.i("quota", String.valueOf(q.quota));
                    if(q.stDay.equals(day))
                    z+=q.quota;
                }
                cx = new dayWiseQuota(day,c.quota+z);
                //if(!MainActivity.dateIsThere)
                //cx.addQuota(cx);
                Log.i("what is quota", String.valueOf(c.quota));
                MainActivity.mdbr.child("QuotaDayWise").child(MainActivity.accName).child(day).child("quota").setValue(c.quota+z);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.i("fetch for dayWiseQuota","failed");
            }
        });
    }
}
