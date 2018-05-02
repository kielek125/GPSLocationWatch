package com.malavero.trackyourchild.watchgps.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.Toolbar;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.malavero.trackyourchild.watchgps.R;
import com.malavero.trackyourchild.watchgps.helpers.FileManager;
import com.malavero.trackyourchild.watchgps.helpers.SessionManager;
import com.malavero.trackyourchild.watchgps.services.AppConfig;
import com.malavero.trackyourchild.watchgps.services.AppController;
import com.malavero.trackyourchild.watchgps.services.GPSService;
import com.malavero.trackyourchild.watchgps.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends WearableActivity {

    private TextView textView, tv_latitude, tv_longitude, tv_altitude, tv_status;
    private ToggleButton toggleButton;
    private BroadcastReceiver broadcastReceiver;
    private SessionManager session;
    private String token;
    private String TAG = "GPS_TAG";
    private FileManager fM = new FileManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        textView = (TextView) findViewById(R.id.coordinateTextView);

        tv_latitude = (TextView) findViewById(R.id.tv_coordinates_latitude_values);
        tv_longitude = (TextView) findViewById(R.id.tv_coordinates_longitude_values);
        tv_altitude = (TextView) findViewById(R.id.tv_coordinates_altitude_values);
        tv_status = (TextView) findViewById(R.id.tv_status_info);
        session = new SessionManager(getApplicationContext());
        toggleButton = (ToggleButton) findViewById(R.id.tb_service);
        token = session.getToken();

        if (!runtimePermission())
            enableToggleButton();
        if(Utils.isMyServiceRunning(GPSService.class, this)){
            toggleButton.performClick();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (broadcastReceiver == null)
                broadcastReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        final String[] s = intent.getExtras().get("Coordinates").toString().split(" ");
                        setCoordinatesText(intent.getExtras().get("Coordinates").toString());

                        AsyncTask.execute(new Runnable() {
                            @Override
                            public void run() {
                                sendCoordinates(s[0],s[1]);
                            }
                        });
                    }
                };
            registerReceiver(broadcastReceiver, new IntentFilter("location_update"));
        } catch (Exception e)
        {
            fM.saveFile(this,this, e.getMessage());
        }

    }

    private void setCoordinatesText(String coordinates) {
        String[] coords = coordinates.split(" ");
        tv_longitude.setText(coords[0]);
        tv_latitude.setText(coords[1]);
        tv_altitude.setText(coords[2]);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            switch (item.getItemId()) {
                case R.id.action_settings:
                    return true;

                case R.id.action_logoff:
                    // User chose the "logoff" action, mark the current item
                    // as a favorite...
                    session = new SessionManager(getApplicationContext());
                    if (session.isLoggedIn()) {
                        session.setLogin(false);
                        // User wants to logoff. Take him to login activity
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
                    return true;
                default:
                    return super.onOptionsItemSelected(item);

            }
        } catch (Exception e) {
            fM.saveFile(this,this, e.getMessage());
            return false;
        }
    }

    private void enableToggleButton() {
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Intent service = new Intent(getApplicationContext(), GPSService.class);
                    Log.d("service_enabled", "GPS LOCALIZATION HAS BEEN ENABLED");
                    startService(service);
                    tv_status.setText(getString(R.string.app_service_enable_description));

                    //Toast.makeText(MainActivity.this, getString(R.string.app_service_enable_description), Toast.LENGTH_SHORT).show();
                } else {
                    Intent service = new Intent(getApplicationContext(), GPSService.class);
                    Log.d("service_disabled", "GPS LOCALIZATION HAS BEEN DISABLED");
                    stopService(service);
                    tv_status.setText(getString(R.string.app_service_disable_description));
                    //Toast.makeText(MainActivity.this, getString(R.string.app_service_disable_description), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private boolean runtimePermission() {
        if (Build.VERSION.SDK_INT >= 23
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 100:
            {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED)
                    enableToggleButton();
                else
                    runtimePermission();
                break;
            }
            case 5:
            {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                }
                else{
                    Toast.makeText(this, "Nie udało się utworzyć katalogu: ", Toast.LENGTH_LONG).show();
                }
            }
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (broadcastReceiver != null)
            unregisterReceiver(broadcastReceiver);
    }

    private void sendCoordinates(final String longitude, final String latitude)
    {
        try {
            String tag_string_req = "req_login";

            StringRequest stringRequest = new StringRequest (Request.Method.PUT, AppConfig.URL_UPDATE, new Response.Listener<String>()
            {

                @Override
                public void onResponse(String response) {
                    try {
                        JSONObject jObj = new JSONObject(response);
                        boolean error = jObj.has("error");

                        if (!error)
                        {
                            token = jObj.get("Authorization").toString();
                        }
                    } catch (JSONException e)
                    {
                        Log.i(TAG,"File generated successfully");
                        e.printStackTrace();
                    }

                }
            }, new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error)
                {
                    String body;
                    String statusCode = String.valueOf(error.networkResponse.statusCode);
                    //get response body and parse with appropriate encoding
                    if(error.networkResponse.data!=null) {
                        try
                        {
                            body = new String(error.networkResponse.data,"UTF-8");
                            JSONObject jObj = new JSONObject(body);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }) {

                @Override
                public Map<String, String> getHeaders() throws AuthFailureError
                {
                    Map<String, String> params = new HashMap<String, String>();
                    if(token != null)
                        params.put("Authorization", token);
                    return params;
                }

                @Override
                protected Map<String, String> getParams() {
                    // Posting parameters to login url
                    Map<String, String> params = new HashMap<String, String>();

                    params.put("longitude", longitude);
                    params.put("latitude", latitude);
                    params.put("device_name", Build.ID);
                    return params;
                }
            };

            AppController.getInstance().addToRequestQueue(stringRequest, tag_string_req);
        } catch (Exception e)
        {
            fM.saveFile(this, this, e.getMessage());
        }
    }
}
