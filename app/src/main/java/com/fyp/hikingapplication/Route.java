package com.fyp.hikingapplication;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class Route {
    private String routeName;
    private ArrayList<RoutePoint> points;
private double totalTime = 0;
    public Route(String name, JSONArray data){
        this.routeName = name;
        this.points = new ArrayList<RoutePoint>();
        if(data != null){
            for (int i = 0; i < data.length() ; i++){
                try {
                    RoutePoint pt = new RoutePoint(data.getJSONObject(i));
                    this.points.add(pt);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public Route(String name, JSONArray data, boolean dummy){
        this.routeName = name;
        this.points = new ArrayList<RoutePoint>();
        if(data != null){
            for (int i = 0; i < data.length() ; i++){
                try {
                    RoutePoint pt = new RoutePoint(data.getJSONObject(i));
                    this.points.add(pt);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public String getRouteName(){return this.routeName;}
    public ArrayList<RoutePoint> getPoints(){return this.points;}

    public String pointsToJSONString(){
        return "";
    }

    public int getPointData(String title){
        int result = -1;
        for (int i=0; i<this.points.size(); i++){
            Log.e("Finding", this.points.get(i).getPointTitle() + " vs " + title);
            if(this.points.get(i).getPointTitle().equals(title)){
                result = i;
                Log.e("RESULT?", "match");
                break;
            } else {
                Log.e("RESULT?", "no match");
            }
        }
        return result;
    }

    public RoutePoint getPoint(int i){
        return this.points.get(i);
    }
    public RoutePoint getLastPoint(){
        return this.points.get(this.points.size()-1);
    }
    public void updatePoint(int i, String description, String image){
        RoutePoint point = this.points.get(i);
        if(description!=null){
            point.setDescription(description);
        }
        if(image!=null){
            point.setImage(image);
        }
    }

    public void addPoint(RoutePoint point){
        this.points.add(point);
    }
public void setTotalTime(double time){this.totalTime = time;}
    public double getTotalTime() {return this.totalTime;}
}



