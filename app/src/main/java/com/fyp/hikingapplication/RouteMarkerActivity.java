package com.fyp.hikingapplication;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;


public class RouteMarkerActivity extends AppCompatActivity {

    final Context context = this;
    private TextView marker_title_tv;
    private EditText marker_description_tv;
    private ImageView marker_iv;
    private Button route_marker_save;
    private String markerImage_URIString = "";

    private static final int SELECT_PICTURE = 2;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private Bitmap markerImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_marker);

        Intent i = getIntent();

        marker_title_tv = (TextView) findViewById(R.id.marker_title_tv);
        marker_description_tv = (EditText) findViewById(R.id.marker_description_tv);
        marker_iv = (ImageView) findViewById(R.id.marker_iv);
        route_marker_save = (Button) findViewById(R.id.route_marker_save);

        setTitle(i.getStringExtra("route_title"));
        marker_title_tv.setHint("title");
        marker_description_tv.setHint("description");

        if(i.getStringExtra("marker_title") != null && marker_title_tv != null){
            marker_title_tv.setText(i.getStringExtra("marker_title"));
        }

        if(i.getStringExtra("marker_description") != null && marker_description_tv != null){
            marker_description_tv.setText(i.getStringExtra("marker_description"));
        }

        String image_path = i.getStringExtra("marker_image");
        Log.e("load image", image_path);
        if(image_path != null && marker_iv != null){
            markerImage_URIString = image_path;
            if(image_path.contains("http:")){
                new getImage().execute("");
            } else {
                marker_iv.setImageBitmap(decodeSampledBitmapFromFile(image_path, 1280, 960));
            }
        }

        marker_iv.setOnClickListener(new View.OnClickListener() {
            @Override
            //On click function
            public void onClick(View view) {
                final CharSequence[] items = {"Take photos", "Select images from gallery"};

                AlertDialog.Builder builder = new AlertDialog.Builder(RouteMarkerActivity.this);
                builder.setTitle("Select your action");
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        if (item == 0) {
                            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                            }
                        } else if (item == 1) {
                            Intent intent = new Intent();
                            intent.setType("image/*");
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE);
                        }

                    }
                });
                AlertDialog image_option_dialog = builder.create();
                image_option_dialog.show();
            }
        });

        route_marker_save.setOnClickListener(new View.OnClickListener() {
            @Override
            //On click function
            public void onClick(View view) {
                finishActivity();
            }
        });


    }


    public static Bitmap decodeSampledBitmapFromFile(String path, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        final int height = options.outHeight;
        final int width = options.outWidth;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        int inSampleSize = 1;
        if (height > reqHeight) {
            inSampleSize = Math.round((float)height / (float)reqHeight);
        }
        int expectedWidth = width / inSampleSize;
        if (expectedWidth > reqWidth) {
            inSampleSize = Math.round((float)width / (float)reqWidth);
        }
        options.inSampleSize = inSampleSize;
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                Log.e("PROCESS", "TAKE PICTURE");
                Uri imageUri = data.getData();
                marker_iv.setImageURI(imageUri);
                markerImage_URIString = "";
                Cursor cursor = null;
                try {
                    String[] proj = { MediaStore.Images.Media.DATA };
                    cursor = context.getContentResolver().query(imageUri, proj, null, null, null);
                    if(cursor!=null && cursor.moveToFirst()){
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
                marker_iv.setImageURI(selectedImageUri);
                markerImage_URIString = "";
                markerImage_URIString = getRealPathFromURI(selectedImageUri);
                Log.e("IMAGE-2", markerImage_URIString);
            }
        }
    }

    private String getRealPathFromURI(Uri contentURI) {
        String filePath = "";
        String wholeID = null;
        wholeID = DocumentsContract.getDocumentId(contentURI);
        String id = wholeID.split(":")[1];
        String[] column = { MediaStore.Images.Media.DATA };
        String sel = MediaStore.Images.Media._ID + "=?";

        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                column, sel, new String[]{ id }, null);
        int columnIndex = cursor.getColumnIndex(column[0]);
        if (cursor.moveToFirst()) {
            filePath = cursor.getString(columnIndex);
        }
        cursor.close();
        return filePath;
    }

    private void finishActivity(){
        AlertDialog.Builder builder = new AlertDialog.Builder(RouteMarkerActivity.this);
        builder.setTitle("Save marker data?");
        builder.setCancelable(true);

        builder.setPositiveButton("Saveqwe", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("markerDescription", marker_description_tv.getText().toString());
                resultIntent.putExtra("markerImage", markerImage_URIString);
                setResult(MapsActivity.RESULT_OK, resultIntent);
                finish();
            }
        });

        builder.setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Log.e("Finish", "Drop");
                Intent resultIntent = new Intent();
                resultIntent.putExtra("dismiss", true);
                setResult(MapsActivity.RESULT_OK, resultIntent);
                finish();
            }
        });
        builder.create().show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finishActivity();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    private class getImage extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            try {
                Log.e("imageFromRemote", markerImage_URIString);
                InputStream image_result = (InputStream)new URL(markerImage_URIString).getContent();
                markerImage = BitmapFactory.decodeStream(image_result);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "";
        }
        @Override
        protected void onPostExecute(String result){
            marker_iv.setImageBitmap(markerImage);
        }
    }
}


