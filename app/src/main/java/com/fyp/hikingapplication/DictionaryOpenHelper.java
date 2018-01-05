package com.fyp.hikingapplication;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class DictionaryOpenHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    DictionaryOpenHelper(Context context) {
        super(context, "project_hiking", null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE routes_version (versionID INTEGER PRIMARY KEY);");
        db.execSQL("CREATE TABLE routes (routeID INTEGER PRIMARY KEY, routeTitle TEXT NOT NULL, routeDuration DOUBLE NOT NULL);");
        db.execSQL("CREATE TABLE route_points (routeID INTEGER, pointID INTEGER, pointTitle TEXT, pointDescription TEXT, pointImage TEXT, latitude DOUBLE, longitude DOUBLE, PRIMARY KEY(routeID, pointID));");
        db.execSQL("CREATE TABLE credential (userID INTEGER PRIMARY KEY, username TEXT);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

}

