package com.fyp.hikingapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class RouteActivity extends AppCompatActivity {

    private SQLiteDatabase database;
    private RouteDB db;
    private int db_version;
    ListView routes_listView;
    ArrayList<String> routeNames;
    ArrayList<Integer> routeID;
    AlertDialog alertDialog;
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getIntent().setAction("CREATED");
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(RouteActivity.this);
                    LayoutInflater inflater = RouteActivity.this.getLayoutInflater();
                    View dialogView = inflater.inflate(R.layout.new_custom_route_dialog, null);
                    dialogBuilder.setView(dialogView);

                    final EditText route_name = (EditText) dialogView.findViewById(R.id.new_route_name_input);

                    dialogBuilder.setPositiveButton(
                            "Create",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    Intent intent = new Intent(RouteActivity.this, CustomMapActivity.class);
                                    intent.putExtra("routeName", route_name.getText().toString());
                                    startActivity(intent);
                                }
                            });
                    dialogBuilder.setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                }
                            });
                    dialogBuilder.create().show();
                }
            });
        }

        routeNames = new ArrayList<>();
        routeID = new ArrayList<>();
        routes_listView = (ListView) findViewById(R.id.routeList);

        db = new RouteDB(getApplicationContext());
        int userid = db.getCredential_userID();
        Log.e("useriddddddddddddddd", String.valueOf(userid));
       new DownloadRouteData().execute();

        routes_listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, final int pos, long id) {
                Log.e("long clicked", "pos: " + pos);
                final int selected_routeID = routeID.get(pos);

                AlertDialog.Builder builder = new AlertDialog.Builder(RouteActivity.this);
                builder.setTitle("Remove the route?");
                builder.setCancelable(true);

                builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (selected_routeID < 0) {
                            Log.e("TODO", "Remove local record " + (0 - selected_routeID));
                            int real_selected_id = 0 - selected_routeID;
                            db.deleteRoute(real_selected_id);
                        } else {
                            Log.e("TODO", "Remove remote record " + selected_routeID);
                            new doRequest().execute(String.valueOf(selected_routeID));
                        }
                        routeID.remove(pos);
                        adapter.remove(adapter.getItem(pos));
                        adapter.notifyDataSetChanged();
                        db.deleteRoute(pos + 1);
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Log.e("Finish", "Cancel");

                    }
                });
                builder.create().show();
                return true;
            }
        });

        routes_listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.e("POS", String.valueOf(position));
                Log.e("POS", String.valueOf(routeID.get(position)));
                Intent intent = new Intent(RouteActivity.this, MapsActivity.class);
                intent.putExtra("routeID", routeID.get(position));
                startActivity(intent);
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_route, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
         if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class doRequest extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            try {
                URL url = new URL("http://128.199.106.211/deleteRoute.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write("pathID="+urls[0]);
                System.out.println("pathID="+urls[0]);
                writer.flush();
                writer.close();
                os.close();
                // Starts the query
                conn.connect();
                conn.getResponseCode();
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            }
            return "";
        }
        @Override
        protected void onPostExecute(String result){

        }
    }

    private class DownloadRouteData extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
//            try {
//                return downloadUrl(urls[0]);
//            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
//        }
        }
        @Override
        protected void onPostExecute(String result){
            Log.e("Downloaded_data", result);
            try {
                routeNames = new ArrayList<>();
                routeID = new ArrayList<>();
                Cursor cs = db.getRoutes();
                Log.e("no way","noway");
                if(cs!=null ){
                    do {
                        routeNames.add(String.valueOf(cs.getString(1)));
                        routeID.add(0-cs.getInt(0));
                        Log.e("Local-db", String.valueOf(0-cs.getInt(0)));
                    } while (cs.moveToNext());
                }
                if(cs!=null) cs.close();
//                JSONObject obj = new JSONObject(result);
//                JSONArray routes = obj.getJSONArray("routes");
//                for(int i = 0; i < routes.length(); i++){
//                    JSONObject route = routes.getJSONObject(i);
//                    String routeTitle = route.getString("pathName");
//                    int id = route.getInt("pathID");
//                    routeNames.add(routeTitle);
//                    routeID.add(id);
//                    Log.e("remote-db", String.valueOf(id));
//                }
                adapter = new ArrayAdapter<String>(RouteActivity.this, android.R.layout.simple_list_item_1, routeNames);
                routes_listView.setAdapter(adapter);
            } catch (Exception e) {
                Log.e("JSON_ERROR", "Could not parse malformed JSON");
                Log.e("JSON_ERROR", e.toString());
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
            Log.d("Test",contentAsString);
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

    public void onResume(){
        String action = getIntent().getAction();
        if(action == null || !action.equals("CREATED")) {
            Intent intent = new Intent(this, RouteActivity.class);
            startActivity(intent);
            finish();
        }
        else{
            getIntent().setAction(null);
        }
        super.onResume();

    }

}


