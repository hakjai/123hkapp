package com.fyp.hikingapplication;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

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
import java.util.Timer;
import java.util.TimerTask;




public class CustomMapActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    //unknown use
    final Context context = this;
    public static final String TAG = CustomMapActivity.class.getSimpleName();

    RouteDB db;
    int userID = 0;

    //Google Map component
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private float currentZoom = -1;

    //boolean control
    private boolean startRequest = false;
    private boolean record_started = false;
    private boolean on_work = false;

    //timer for recording
    private Timer timer;
    private Timer timer_forTime;
    private TimerTask record_task;
    private TimerTask record_task_time;

    //activity component
    private Button recording_btn;
    private Button action_btn;
    private TextView time;
    private TextView distance;
    private Route custom_route;
    private Location last_known_location;
    private long total_time = 0;
    private long total_distance = 0;
    private long last_record_time = 0;

    //dialog component
    private AlertDialog alertDialog;
    private boolean new_marker = true;
    private int marker_ptr = -1;
    private EditText markerTitle;
    private EditText markerDescription;
    private ImageView markerImage;
    private String markerImage_URIString;
    private Button marker_save;

    private static final int SELECT_PICTURE = 2;
    private static final int REQUEST_IMAGE_CAPTURE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_map);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.customMap);
        mapFragment.getMapAsync(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();

        String route_name = getIntent().getStringExtra("routeName");
        //Log.e("input", route_name);
        if (route_name.isEmpty()) {
            route_name = "Custom Route";
        }
        setTitle(route_name);
        custom_route = new Route(route_name, null);
        RouteDB db = new RouteDB(getApplicationContext());
        userID = db.getCredential_userID();

        prepareRecording();
        prepareDialog();
        startTimer();
        startTimerForTime();

    }

    private void prepareRecording() {
        recording_btn = (Button) findViewById(R.id.recording_btn);
        action_btn = (Button) findViewById(R.id.custom_route_action);
        time = (TextView) findViewById(R.id.custom_route_time);
        distance = (TextView) findViewById(R.id.custom_route_distance);

        recording_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            //On click function
            public void onClick(View view) {
                if (!record_started) {

                    record_started = true;
                    recording_btn.setText("Stop recording");
                    recording_btn.setTextColor(Color.RED);
                    action_btn.setText("Create Marker");

                    requestNewLocation();
                    handleNewRoutePoint();

                } else {
                    record_started = false;
                    recording_btn.setText("Start recording");
                    recording_btn.setTextColor(Color.BLACK);
                    action_btn.setText("Save Route");
                }
            }
        });

        action_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            //On click function
            public void onClick(View view) {
                if (record_started) {
                    new_marker = true;
                    alertDialog.show();
                } else {
                    finishActivity(true);
                }
            }
        });
    }

    private void prepareDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.marker_dialog, null);
        dialogBuilder.setView(dialogView);
        alertDialog = dialogBuilder.create();

        markerTitle = (EditText) dialogView.findViewById(R.id.markerTitle);
        markerDescription = (EditText) dialogView.findViewById(R.id.markerDescription);
        markerImage = (ImageView) dialogView.findViewById(R.id.markerImage);
        marker_save = (Button) dialogView.findViewById(R.id.marker_save);
        markerImage_URIString = "";

        markerImage.setOnClickListener(new View.OnClickListener() {
            @Override
            //On click function
            public void onClick(View view) {
                final CharSequence[] items = {"Take photos", "Select images from gallery"};
                AlertDialog.Builder builder = new AlertDialog.Builder(CustomMapActivity.this);
                builder.setTitle("Select your action");
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        if (item == 0) {
                            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                                on_work = true;
                                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                            }
                        } else if (item == 1) {
                            Intent intent = new Intent();
                            intent.setType("image/*");
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            on_work = true;
                            startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE);
                        }

                    }
                });
                AlertDialog image_option_dialog = builder.create();
                image_option_dialog.show();

            }
        });

        marker_save.setOnClickListener(new View.OnClickListener() {
            @Override
            //On click function
            public void onClick(View view) {

                String title = markerTitle.getText().toString();
                String description = markerDescription.getText().toString();

                if (new_marker) {
                    RoutePoint point = new RoutePoint(title, description, markerImage_URIString, last_known_location.getLatitude(), last_known_location.getLongitude());
                    custom_route.addPoint(point);
                    LatLng latPoint = point.toLatLng();
                    mMap.addMarker(new MarkerOptions().position(latPoint).title(title));

                    Log.e("Routepointlength", String.valueOf(custom_route.getPoints().size()));
                } else {
                    Log.e("SavePOINT", "save point at position " + String.valueOf(marker_ptr));
                    custom_route.getPoint(marker_ptr).setImage(markerImage_URIString);
                    //TODO: update marker title on Google Map
                }


                alertDialog.dismiss();
                markerTitle.setText("");
                markerDescription.setText("");
                markerImage.setImageResource(R.mipmap.ic_image);
                markerImage_URIString = "";
            }
        });

        dialogBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Log.e("Dialog", "dismissed");
            }
    });
    }

    public void do_update(){    }

    private void startTimerForTime(){
        record_task_time = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        long current_time = System.currentTimeMillis();
                        if (record_started && startRequest) {
                            if(last_record_time != 0){
                                total_time += current_time - last_record_time;
                            }
                            double total_time_seconds = new Long(total_time).doubleValue()/1000;
                            time.setText("Time: " + String.valueOf((int)total_time_seconds));
                        }
                        last_record_time = current_time;
                    }
                });
            }
        };
        timer_forTime = new Timer();
        timer_forTime.schedule(record_task_time, 1000, 1000);
    }

    public void startTimer() {
        record_task = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (record_started && startRequest) {
                            requestNewLocation();
                            handleNewRoutePoint();
                        }
                    }
                });
            }
        };
        timer = new Timer();
        timer.schedule(record_task, 1000, 5000);
    }

    private void handleNewRoutePoint(){
        System.out.println("handleNewRoutePoint");
        System.out.println(record_started);
        System.out.println(startRequest);
        if (last_known_location != null) {
            LatLng pointLatLng = new LatLng(last_known_location.getLatitude(), last_known_location.getLongitude());
            if (currentZoom < 10) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pointLatLng, 16.0f));
            } else {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pointLatLng, currentZoom));
            }


            RoutePoint point = new RoutePoint("", "", "", last_known_location.getLatitude(), last_known_location.getLongitude());
            if (custom_route.getPoints().size() > 1) {
                RoutePoint previous_point = custom_route.getLastPoint();
                mMap.addPolyline(new PolylineOptions().add(previous_point.toLatLng(), point.toLatLng()).width(5).color(Color.RED));
                float distance_diff[] = new float[1];
                Location.distanceBetween(previous_point.getLatitude(), previous_point.getLongitude(), point.getLatitude(), point.getLongitude(), distance_diff );
                total_distance += (long)distance_diff[0];
                System.out.println(total_distance);
                distance.setText("Distance: " + String.valueOf(total_distance));
            }
            custom_route.addPoint(point);
            do_update();
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
                InputStream is = conn.getInputStream();
                String contentAsString = readIt(is);
                Log.e("returned", contentAsString);
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            }
            return "";
        }
        @Override
        protected void onPostExecute(String result){

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

    private void finishActivity(boolean forceToLeave) {
        AlertDialog.Builder builder = new AlertDialog.Builder(CustomMapActivity.this);
        builder.setTitle("Save route data?");
        builder.setCancelable(true);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                AlertDialog.Builder sub_builder = new AlertDialog.Builder(CustomMapActivity.this);
                sub_builder.setTitle("Save to remote server?");
                sub_builder.setCancelable(true);

                sub_builder.setPositiveButton("Save to remote server", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Log.e("Status", "Custom-map saving to remote");
                        custom_route.setTotalTime(new Long(total_time).doubleValue() / 1000 / 3600);
                        new doRequest().execute(custom_route.getRouteName(), String.valueOf(custom_route.getTotalTime()));
                        for(int i=0; i<custom_route.getPoints().size();i++){
                            RoutePoint point= custom_route.getPoint(i);
                            String encodedImage = "";
                            if(!point.getImage().isEmpty()){
                                Bitmap bm = BitmapFactory.decodeFile(point.getImage());
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                bm.compress(Bitmap.CompressFormat.JPEG, 10, baos);
                                byte[] b = baos.toByteArray();
                                encodedImage = Base64.encodeToString(b, Base64.DEFAULT);
                                System.out.println("upload image");
                                Log.e("image-size", String.valueOf(encodedImage.length()));
                            }
                            new doRequest().execute(point.getPointTitle(), point.getDescription(), String.valueOf(point.getLatitude()), String.valueOf(point.getLongitude()),encodedImage,String.valueOf(i));
                        }

                        if(timer!=null) timer.cancel();
                        if(timer_forTime!=null) timer_forTime.cancel();
                        finish();
                    }
                });
                sub_builder.setNegativeButton("Save to local only", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        RouteDB db = new RouteDB(getApplicationContext());
                        Log.e("Status", "Custom-map saving to local");
                        custom_route.setTotalTime(new Long(total_time).doubleValue()/1000/3600);
                        db.insertRoute(custom_route);
                        if(timer!=null) timer.cancel();
                        if(timer_forTime!=null) timer_forTime.cancel();
                        finish();
                    }
                });

                sub_builder.create().show();
            }
        });
        builder.setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Log.e("Finish", "Drop");
                if (mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.disconnect();
                }
                if(timer!=null) timer.cancel();
                if(timer_forTime!=null) timer_forTime.cancel();
                finish();
            }
        });
        builder.create().show();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                Log.e("PROCESS", "TAKE PICTURE");
                Uri imageUri = data.getData();
                markerImage.setImageURI(imageUri);
                markerImage_URIString = "";
                Cursor cursor = null;
                try {
                    String[] proj = {MediaStore.Images.Media.DATA};
                    cursor = context.getContentResolver().query(imageUri, proj, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        markerImage_URIString = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                Log.e("IMAGE-1", markerImage_URIString);
            } else if (requestCode == SELECT_PICTURE) {
                Log.e("PROCESS", "SELECT PICTURE");
                Uri selectedImageUri = data.getData();
                markerImage.setImageURI(selectedImageUri);
                markerImage_URIString = "";
                markerImage_URIString = getRealPathFromURI(selectedImageUri);
                Log.e("IMAGE-2", markerImage_URIString);
            }
            on_work = false;
        }
    }

    private String getRealPathFromURI(Uri contentURI) {
        String filePath = "";
        String wholeID = null;

        wholeID = DocumentsContract.getDocumentId(contentURI);
        String id = wholeID.split(":")[1];
        String[] column = {MediaStore.Images.Media.DATA};
        String sel = MediaStore.Images.Media._ID + "=?";
        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                column, sel, new String[]{id}, null);
        int columnIndex = cursor.getColumnIndex(column[0]);
        if (cursor.moveToFirst()) {
            filePath = cursor.getString(columnIndex);
        }
        cursor.close();
        return filePath;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(true);

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                new_marker = false;
                marker_ptr = -1;
                marker_ptr = custom_route.getPointData(marker.getTitle());
                Log.e("DEBUG-CLICKMARKER", String.valueOf(marker_ptr));
                if (marker_ptr != -1) {
                    RoutePoint point = custom_route.getPoint(marker_ptr);
                    Log.e("DEBUG-image", point.getImage());
                    markerTitle.setText(point.getPointTitle());
                    markerDescription.setText(point.getDescription());
                    markerImage.setImageURI(Uri.parse(point.getImage()));
                    markerImage_URIString = point.getImage();
                    alertDialog.show();
                }

                return false;
            }
        });
        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {

            @Override
            public void onCameraChange(CameraPosition position) {
                if (position.zoom != currentZoom) {
                    currentZoom = position.zoom;
                }
            }
        });
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Location services connected.");
        startRequest = true;
    }

    private void requestNewLocation() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (location == null) {
                Log.d("New Location", "No location");
            } else {
                handleNewLocation(location);
            }
        }
    }

    private void handleNewLocation(Location location) {
        last_known_location = location;
        Log.e("New Location", location.toString());
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Location services suspended. Please reconnect.");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
        if (startRequest && record_started) {
            startTimer();
            startTimerForTime();
        }
    }

    @Override
    protected void onPause() {
        Log.e("ACTION", "onPause");
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
        }
    }

    public void onStop() {
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        finishActivity(true);
        return;
    }

}



