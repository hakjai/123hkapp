package com.fyp.hikingapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;



public class RouteDB {

    private DictionaryOpenHelper dbHelper;
    private SQLiteDatabase database;
    private final static String TABLE_ROUTE = "routes";
    private final static String TABLE_ROUTE_POINT = "route_points";
    private final static String TABLE_CREDENTIAL = "credential";


    public RouteDB(Context context){
        dbHelper = new DictionaryOpenHelper(context);
        database = dbHelper.getWritableDatabase();
    }

    public void updateCredentials(int userID, String username){
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_CREDENTIAL);
        Log.e("updateCredentials: ",TABLE_CREDENTIAL );
        database.execSQL("CREATE TABLE " + TABLE_CREDENTIAL + " (userID INTEGER PRIMARY KEY, username TEXT);");
        ContentValues values = new ContentValues();
        values.put("userID", userID);
        values.put("username", username);
        database.insert(TABLE_CREDENTIAL, null, values);
    }

    public int getCredential_userID(){
        String[] cols = new String[] {"userID", "username"};
        Cursor records = database.query(TABLE_CREDENTIAL, cols, null, null, null, null, null, "1");
        if(records.moveToFirst()){
            int userid = records.getInt(0);
            records.close();
            return userid;
        }
        return 0;
    }

    public String getCredential_username(){
        String[] cols = new String[] {"userID", "username"};
        Cursor records = database.query(TABLE_CREDENTIAL, cols, null, null, null, null, null, "1");
        if(records.moveToFirst()){
            String username = records.getString(1);
            records.close();
            return username;
        }
        return "";
    }

    public Cursor getRoutes(){
        String[] cols = new String[] {"routeID", "routeTitle"};
        Cursor records = database.query(TABLE_ROUTE, cols, null, null, null, null, "routeID ASC", null);
        Log.e("getRoutes", String.valueOf(records.getCount()));
        if(records.getCount() > 0){
            records.moveToFirst();
        } else {
            records = null;
        }
        return records;
    }

    public Route getRoute(int routeID){
        String[] cols = new String[] {"routeID", "routeTitle", "routeDuration"};
        String[] args = new String[] {String.valueOf(routeID)};
        Cursor record = database.query(TABLE_ROUTE, cols, "routeID=?", args, null, null, "routeID ASC", "1");
        Route route;
        if(record.moveToFirst()){
            route = new Route(record.getString(1), null);
            route.setTotalTime(record.getDouble(2));
        } else {
            return null;
        }
        cols = new String[] {"pointTitle", "pointDescription", "pointImage", "latitude", "longitude"};
        args = new String[] {String.valueOf(routeID)};
        record = database.query(TABLE_ROUTE_POINT, cols, "routeID=?", args, null, null, "pointID ASC", null);
        if(record != null && record.moveToFirst()){
            do {
                RoutePoint point = new RoutePoint(record.getString(0), record.getString(1), record.getString(2), record.getDouble(3), record.getDouble(4));
                route.addPoint(point);
            } while(record.moveToNext());
        }
        return route;
    }

    public int getRoutesCount(){
        String[] cols = new String[] {};
        Cursor records = database.query(TABLE_ROUTE, cols, null, null, null, null, "routeID ASC", null);
        return records.getCount();
    }

    public void deleteRoute(int routeID){
        String[] args = {String.valueOf(routeID)};
        database.delete(TABLE_ROUTE, "routeID=?", args);
        database.delete(TABLE_ROUTE_POINT, "routeID=?", args);
    }

    public void resetAllRoute(){
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_ROUTE);
        database.execSQL("CREATE TABLE " + TABLE_ROUTE + " (routeID INTEGER PRIMARY KEY, routeTitle TEXT NOT NULL);");
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_ROUTE_POINT);
        database.execSQL("CREATE TABLE " + TABLE_ROUTE_POINT + " (routeID INTEGER, pointID INTEGER, pointTitle TEXT, pointDescription TEXT, pointImage TEXT, latitude DOUBLE, longitude DOUBLE, PRIMARY KEY(routeID, pointID));");
    }

    public void insertRoute(Route route){
        Log.e("Status", "hihiihihihihihihihiihihihihihi");
        int routeID = 1;
        String[] cols = new String[] {"routeID"};
        Cursor records = database.query(TABLE_ROUTE, cols, null, null, null, null, "routeID DESC", "1");
        if(records.moveToFirst()){
            routeID = records.getInt(0)+1;
        }
        ContentValues values = new ContentValues();
        values.put("routeID", routeID);
        values.put("routeTitle", route.getRouteName());
        values.put("routeDuration", route.getTotalTime());
        database.insert(TABLE_ROUTE, null, values);

        for(int i=0; i<route.getPoints().size(); i++){
            Log.e("Status", "byebyebyebyebye");
            RoutePoint point = route.getPoint(i);
            values = new ContentValues();
            values.put("routeID", routeID);
            values.put("pointID", i);
            if(point.getPointTitle()==null) {values.put("pointTitle", "");} else {values.put("pointTitle", point.getPointTitle());}
            if(point.getDescription()==null) {values.put("pointDescription", "");} else {values.put("pointDescription", point.getDescription());}
            if(point.getImage()==null) {values.put("pointImage", "");} else {values.put("pointImage", point.getImage());}
            values.put("latitude", point.getLatitude());
            values.put("longitude", point.getLongitude());
            database.insert(TABLE_ROUTE_POINT, null, values);
        }
    }

    public void updateRoute(int routeID, int pointID, RoutePoint point){
        ContentValues values = new ContentValues();
        values.put("pointTitle", point.getPointTitle());
        values.put("pointDescription", point.getDescription());
        values.put("pointImage", point.getImage());
        values.put("latitude", point.getLatitude());
        values.put("longitude", point.getLongitude());
        String[] args = {String.valueOf(routeID), String.valueOf(pointID)};
        database.update(TABLE_ROUTE_POINT, values, "routeID=? AND pointID=?", args);
    }


}

