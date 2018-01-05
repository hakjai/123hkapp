package com.fyp.hikingapplication;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;


public class RoutePoint {
    private String title;
    private String image;
    private String description;
    private double latitude;
    private double longitude;

    public RoutePoint(JSONObject obj){
        try {
            if(obj.has("title")) {
                this.title = obj.getString("title");
            } else {
                this.title = "";
            }
            this.latitude = obj.getDouble("latitude");
            this.longitude = obj.getDouble("longitude");
            if(obj.has("image")){
                this.image = obj.getString("image");
            } else {
                this.image = "";
            }
            if(obj.has("description")){
                this.description = obj.getString("description");
            } else {
                this.description = "";
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public RoutePoint(String title, String description, String image, double latitude, double longitude){
        this.title = title;
        this.description = description;
        this.image = image;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public RoutePoint(String title, String description, String image, String dummy){

    }

    public LatLng toLatLng(){
        return new LatLng(latitude, longitude);
    };
    public double getLatitude(){return this.latitude;}
    public double getLongitude(){return this.longitude;}
    public String getPointTitle(){return this.title;}
    public String getImage(){return this.image;};
    public String getDescription(){return this.description;}
    public void setPointTitle(String title) { this.title = title; }
    public void setImage(String image) { this.image = image; }
    public void setDescription(String description) { this.description = description; }
}



