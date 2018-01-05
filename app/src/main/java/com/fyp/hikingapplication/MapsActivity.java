package com.fyp.hikingapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private boolean mapReady = false;
    private RouteDB db;
    private Route route;
    private Boolean customRoute;
    private int routeID;
    private String routeTitle;
    private ArrayList<RoutePoint> route_points;
    private int selectedPoint = -1;
    private static final int MARKER_DETAIL = 1;
    private int userID;
    private Button convertBtn;
    private double totalDistance = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        Intent i = getIntent();
        db = new RouteDB(getApplicationContext());
        routeID = i.getIntExtra("routeID", 2);
        userID = db.getCredential_userID();
        convertBtn = (Button) findViewById(R.id.convert_btn);
        Log.e("INTENT", String.valueOf(routeID));
        if (routeID < 0) {
            new DownloadRouteData().execute("local");
            convertBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                //On click function
                public void onClick(View view) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                    builder.setTitle("Upload route data?");
                    builder.setCancelable(true);
                    builder.setPositiveButton("Upload", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                            new doRequest().execute(route.getRouteName(), String.valueOf(route.getTotalTime()));
                            for (int i = 0; i < route.getPoints().size(); i++) {
                                RoutePoint point = route.getPoint(i);
                                String encodedImage = "";
                                if (!point.getImage().isEmpty()) {

                                    Bitmap bm = BitmapFactory.decodeFile(point.getImage());

                                    ExifInterface exif = null;
                                    try {
                                        exif = new ExifInterface(point.getImage());
                                        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
                                        Matrix matrix = new Matrix();
                                        if (orientation == 6) matrix.postRotate(90);
                                        else if (orientation == 3) matrix.postRotate(180);
                                        else if (orientation == 8) matrix.postRotate(270);
                                        bm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    bm.compress(Bitmap.CompressFormat.JPEG, 10, baos);
                                    byte[] b = baos.toByteArray();
                                    encodedImage = Base64.encodeToString(b, Base64.DEFAULT);
                                    System.out.println("upload image");
                                    Log.e("image-size", String.valueOf(encodedImage.length()));
                                }
                                new doRequest().execute(point.getPointTitle(), point.getDescription(), String.valueOf(point.getLatitude()), String.valueOf(point.getLongitude()), encodedImage, String.valueOf(i));
                            }
                            db.deleteRoute(0 - routeID);
                            finish();
                        }
                    });
                    builder.setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
                    builder.create().show();
                }
            });
        } else {
            //get remote data
            new DownloadRouteData().execute("http://128.199.106.211/route.php?pathID=" + String.valueOf(routeID));
            convertBtn.setVisibility(View.GONE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case (MARKER_DETAIL): {
                if (resultCode == MapsActivity.RESULT_OK && selectedPoint >= 0) {
                    if (data.getBooleanExtra("dismiss", false)) {
                        Log.e("PASSBACK", "dismiss");
                    } else {
                        String markerDescription = data.getStringExtra("markerDescription");
                        String markerImage = data.getStringExtra("markerImage");
                        Log.e("returned description", markerDescription);
                        Log.e("returned image", markerImage);
                        Log.e("current pointid", String.valueOf(selectedPoint));
                        RoutePoint select_point = route.getPoint(selectedPoint);
                        select_point.setDescription(markerDescription);
                        select_point.setImage(markerImage);
                        if(routeID < 0){
                            db.updateRoute(0-routeID, selectedPoint, select_point);
                        } else {
                            String encodedImage = select_point.getImage();
                            if(!select_point.getImage().isEmpty() && !select_point.getImage().contains("http:")){
                                Bitmap bm = BitmapFactory.decodeFile(select_point.getImage());
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                bm.compress(Bitmap.CompressFormat.JPEG, 10, baos);
                                byte[] b = baos.toByteArray();
                                encodedImage = Base64.encodeToString(b, Base64.DEFAULT);
                            }
                            new do_updateRemote().execute(String.valueOf(routeID), String.valueOf(selectedPoint), markerDescription, encodedImage);
                        }
                        selectedPoint = -1;
                    }
                }
                break;
            }
        }
    }

    private void processPoints(){
        Log.e("process", "start processing points");
        ArrayList<RoutePoint> points = route.getPoints();
        for (int i=0;i<points.size();i++){
            RoutePoint pt = points.get(i);
            Log.e("process", "point " + i +" with title " + pt.getPointTitle());
            if(pt.getPointTitle().isEmpty()){
                if (i > 0) {
                    mMap.addPolyline(new PolylineOptions().add(points.get(i - 1).toLatLng(), pt.toLatLng()).width(5).color(Color.RED));
                    totalDistance += distance(points.get(i - 1).getLatitude(), pt.getLatitude(), points.get(i - 1).getLongitude(), pt.getLongitude(), 0, 0);
                } else {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pt.toLatLng(), 13.0f));
                }
            } else {
                System.out.println("add marker " + i);
                mMap.addMarker(new MarkerOptions().position(pt.toLatLng()).title(pt.getPointTitle()));
            }
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(points.get(0).toLatLng(), 13.0f));
        Log.e("process", "end processing points");
        if(points.size()>0){
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(points.get(0).toLatLng(), 13.0f));
            DecimalFormat df = new DecimalFormat("#.##");
            totalDistance = Double.valueOf(df.format(totalDistance));
            TextView maps_distance_tv = (TextView) findViewById(R.id.maps_distance_tv);
            maps_distance_tv.setText(String.valueOf(totalDistance) + "m");
            TextView maps_duration_tv = (TextView) findViewById(R.id.maps_duration_tv);
            maps_duration_tv.setText(String.valueOf(Double.valueOf(df.format(route.getTotalTime()))) + "h");
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            mMap.setMyLocationEnabled(true);
        }

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(true);

        mapReady = true;


        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker arg0) {
                selectedPoint = route.getPointData(arg0.getTitle());
                System.out.println(selectedPoint);
                if(selectedPoint>0){
                    RoutePoint point = route.getPoint(selectedPoint);
                    Intent intent = new Intent(MapsActivity.this, RouteMarkerActivity.class);
                    intent.putExtra("route_title", routeTitle);
                    intent.putExtra("marker_title", point.getPointTitle());
                    intent.putExtra("marker_description", point.getDescription());
                    intent.putExtra("marker_image", point.getImage());
                    startActivityForResult(intent, MARKER_DETAIL);
                }
                return false;
            }
        });
    }

    private class DownloadRouteData extends AsyncTask<String, Void, String> {
        private boolean local = true;
        @Override
        protected String doInBackground(String... urls) {
            String result = new String();
            try {
                if(urls[0] == "local"){
                    local = true;
                    JSONObject obj = new JSONObject();
                    obj.put("result", 1);
                    obj.put("local", true);
                    result = obj.toString();
                } else {
                    result = downloadUrl(urls[0]);
                }
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            } catch (org.json.JSONException e) {
                Log.e("JSON_ERROR", "Could not parse malformed JSON");
            }
            return result;
        }
        @Override
        protected void onPostExecute(String result){
            Log.e("Downloaded_data", result);
            try {
                JSONObject obj = new JSONObject(result);
                if(obj.getInt("result")==1){
                    if(!obj.isNull("local")){
                        route = db.getRoute(0-routeID);
                    } else {
                        route = new Route(obj.getString("route_name"), obj.getJSONArray("route_data"));
                        route.setTotalTime(obj.getDouble("route_duration"));
                    }
                    Log.e("Route", String.valueOf(route.getPoints().size()));
                    processPoints();
                } else {
                    Toast.makeText(MapsActivity.this, "Cannot find data from remote server, please refresh your list", Toast.LENGTH_SHORT);
                    finish();
                }
            } catch (org.json.JSONException e) {
                Log.e("JSON_ERROR", "Could not parse malformed JSON");
            }
        }
    }

    private String downloadUrl(String myurl) throws IOException {
        InputStream is = null;

        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            int response = conn.getResponseCode();
            Log.d("Response", "The response is: " + response);
            is = conn.getInputStream();
            String contentAsString = readIt(is);
            return contentAsString;
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    public String readIt(InputStream stream) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(stream));
        StringBuilder total = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            total.append(line);
        }
        return total.toString();
    }
    private class do_updateRemote extends AsyncTask<String, Void, String> {
        int update_pointID;
        String update_description;
        String update_image;
        @Override
        protected String doInBackground(String... params) {
            try {
                URL url = new URL("http://128.199.106.211/updateRoute.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                update_pointID = Integer.valueOf(params[1]);
                update_description = params[2];
                update_image = params[3];
                String param_query = "pathID="+ params[0] + "&pointID=" + params[1] + "&pointDescription=" + params[2] + "&image=" + params[3];
                writer.write(param_query);
                writer.flush();
                writer.close();
                os.close();
                conn.connect();
                int response = conn.getResponseCode();
                System.out.println(response);
                InputStream is = conn.getInputStream();
                String contentAsString = readIt(is);
                Log.e("updateRemote returned", contentAsString);
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            }
            return "";
        }
        @Override
        protected void onPostExecute(String result){
            route.getPoint(update_pointID).setDescription(update_description);
        }
    }
    private class doRequest extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                URL url = new URL("http://128.199.106.211/insertRoute.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                if(params.length == 2){
                    String param_query = "upload=path&uid="+ userID + "&pathName=" + params[0]+ "&pathDuration=" + params[1];
                    writer.write(param_query);
                } else {
                    String param_query = "upload=point&uid="+ userID + "&pointID=" + params[5] +"&pointTitle=" + params[0] + "&pointDescription=" + params[1] + "&lat=" + params[2] + "&long=" + params[3] + "&image="+params[4];
                    writer.write(param_query);
                }
                writer.flush();
                writer.close();
                os.close();
                conn.connect();
                int response = conn.getResponseCode();
                System.out.println(response);
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            }
            return "";
        }
        @Override
        protected void onPostExecute(String result){}
    }
    public static double distance(double lat1, double lat2, double lon1, double lon2, double el1, double el2) {

        final int R = 6371;
        Double latDistance = Math.toRadians(lat2 - lat1);
        Double lonDistance = Math.toRadians(lon2 - lon1);
        Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000;
        double height = el1 - el2;
        distance = Math.pow(distance, 2) + Math.pow(height, 2);
        return Math.sqrt(distance);
    }
}


