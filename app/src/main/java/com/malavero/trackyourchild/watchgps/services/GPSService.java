package com.malavero.trackyourchild.watchgps.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.malavero.trackyourchild.watchgps.activities.MainActivity;
import com.malavero.trackyourchild.watchgps.helpers.AppProperty;
import com.malavero.trackyourchild.watchgps.helpers.SessionManager;
import com.malavero.trackyourchild.watchgps.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Kiełson on 01.05.2018.
 */

public class GPSService extends Service {
    private LocationListener listener;
    private LocationManager locationManager;
    private boolean mRunning;
    private SessionManager session;
    private String token;
    private String TAG = "GPS_TAG";
    private FileManager2 fm = new FileManager2();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate()
    {
        try {
            mRunning = false;
            listener = new LocationListener()
            {
                @Override
                public void onLocationChanged(Location location)
                {
                    if(Utils.isOnline(getApplicationContext()))
                    {
                        Log.i(TAG,"Location changed");

                        sendCoordinates(location);
                        Intent i = new Intent("location_update");
                        i.putExtra("Coordinates",location.getLongitude()+" "+location.getLatitude()+" "+location.getAltitude());
                        sendBroadcast(i);
                    }
                }

                @Override
                public void onStatusChanged(String s, int i, Bundle bundle)
                {
                    Log.i(TAG,s+String.valueOf(i));
                }

                @Override
                public void onProviderEnabled(String s) {

                }

                @Override
                public void onProviderDisabled(String s) {
                    Intent settingsPanel = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    settingsPanel.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(settingsPanel);
                }
            };
            locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0, listener);
        } catch (Exception e) {
            fm.saveFile(this,e.getMessage());
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        try {
            Log.e(TAG, "onStartCommand");
            Log.e(TAG, "onCreate");
            if (!mRunning)
            {
                mRunning = true;
            }
            session = new SessionManager(getApplicationContext());
            token = session.getToken();

            super.onStartCommand(intent, flags, startId);
            return START_STICKY;
        } catch (Exception e) {
            fm.saveFile(this,e.getMessage());
            return START_NOT_STICKY;
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationManager != null)
            locationManager.removeUpdates(listener);
    }
    private void sendCoordinates(final Location location) {
        try {
            String tag_string_req = "req_login";

            StringRequest stringRequest = new StringRequest (Request.Method.PUT, AppConfig.URL_UPDATE, new Response.Listener<String>()
            {

                @Override
                public void onResponse(String response) {
                    try {
                        JSONObject jObj = new JSONObject(response);
                        boolean error = jObj.has("error");

                        if (!error) {
                            Log.i(TAG,"data sent successfully");
                            //                        Log.i(TAG,jObj.get("Authorization").toString());
                            //     token = jObj.get("Authorization").toString();
                        }
                    } catch (JSONException e) {
                        // JSON error
                        fm.saveFile(getApplicationContext(),e.getMessage());
                    }

                }
            }, new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error)
                {
                    String body;
                    String statusCode = String.valueOf(error.networkResponse.statusCode);
                    //get response body and parse with appropriate encoding
                    Log.i(TAG,"Error in sender"+ statusCode);
                    if(error.networkResponse.data!=null) {
                        try
                        {
                            body = new String(error.networkResponse.data,"UTF-8");
                            Log.i(TAG,body);
                            JSONObject jObj = new JSONObject(body);
                        }
                        catch (Exception e) {
                            fm.saveFile(getApplicationContext(),e.getMessage());
                        }
                    }
                }
            }) {

                @Override
                public Map<String, String> getHeaders() throws AuthFailureError
                {
                    Map<String, String> params = new HashMap<String, String>();
                    Log.i(TAG,""+token);
                    if(token != null)
                        params.put("Authorization", token);
                    return params;
                }

                @Override
                protected Map<String, String> getParams() {
                    // Posting parameters to login url
                    Map<String, String> params = new HashMap<String, String>();

                    params.put("longitude", String.valueOf(location.getLongitude()));
                    params.put("latitude", String.valueOf(location.getLatitude()));
                    params.put("device_name", Build.ID);
                    return params;
                }
            };

            AppController.getInstance().addToRequestQueue(stringRequest, tag_string_req);
        } catch (Exception e) {
            fm.saveFile(this,e.getMessage());
        }
    }
    public class FileManager2 {

        public void checkPermissionFile(Activity activity) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            } else {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, AppProperty.MEMORY_ACCESS);
            }
        }
        public void saveFile(Context context, String message) {
            createDir(context);
            createFile(context, message);
        }

        private void createDir(Context context) {
            File folder = new File(AppProperty.path);
            if (!folder.exists())
                try {
                    folder.mkdirs();
                } catch (Exception e) {
                    Toast.makeText(context, "The catalog has not been created: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
        }

        private void createFile(Context context, String message) {
            File file = new File(AppProperty.path + AppProperty.fileName);
            if (!file.exists())
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            FileOutputStream fout;
            OutputStreamWriter outWriter;
            try {
                fout = new FileOutputStream(file, true);
                outWriter = new OutputStreamWriter(fout);
                outWriter.append(Calendar.getInstance().getTime().toString() + " --- " + message + System.getProperty("line.separator"));
                outWriter.close();

            } catch (Exception e) {
                Toast.makeText(context, "The file has not been created: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }
}
