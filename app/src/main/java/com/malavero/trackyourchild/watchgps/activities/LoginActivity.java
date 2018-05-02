package com.malavero.trackyourchild.watchgps.activities;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.malavero.trackyourchild.watchgps.R;
import com.malavero.trackyourchild.watchgps.helpers.FileManager;
import com.malavero.trackyourchild.watchgps.helpers.SessionManager;
import com.malavero.trackyourchild.watchgps.services.AppConfig;
import com.malavero.trackyourchild.watchgps.services.AppController;
import com.malavero.trackyourchild.watchgps.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Kiełson on 01.05.2018.
 */

public class LoginActivity extends WearableActivity{
    private static final String TAG = RegisteryActivity.class.getSimpleName();
    private Button btnLogin;
    private Button btnLinkToRegister;
    private EditText inputEmail;
    private EditText inputPassword;
    private ProgressDialog pDialog;
    private SessionManager session;
    private FileManager fm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initProperty();

        // Login button Click Event
        registerButton();

    }
    private void initProperty(){
        inputEmail = (EditText) findViewById(R.id.email);
        inputPassword = (EditText) findViewById(R.id.password);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        btnLinkToRegister = (Button) findViewById(R.id.btnLinkToRegisterScreen);

        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        // Session manager
        session = new SessionManager(getApplicationContext());
        fm = new FileManager();
        // Check if user is already logged in or not
        if (session.isLoggedIn())
        {
            // User is already logged in. Take him to main activity
            if(Utils.isOnline(this))
            {
                checkLogin(session.getEmail(), session.getPassword());
            }
            else
            {
                Log.i(TAG,"No internet connection try again later");
                Toast.makeText(this,"No internet connection try again later", Toast.LENGTH_SHORT).show();
            }


        }
    }

    private void registerButton() {
        btnLogin.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                String email = inputEmail.getText().toString().trim();
                String password = inputPassword.getText().toString().trim();

                // Check for empty data in the form
                if (!email.isEmpty() && !password.isEmpty()) {
                    // login user
                    if(Utils.isOnline(getApplicationContext()))
                    {
                        checkLogin(email, password);
                    }
                    else
                    {
                        Toast.makeText(LoginActivity.this, "No network connection", Toast.LENGTH_LONG).show();
                    }
                } else {
                    // Prompt user to enter credentials
                    Toast.makeText(LoginActivity.this, getString(R.string.app_invalid_input), Toast.LENGTH_LONG)
                            .show();
                }
            }

        });

        // Link to Register Screen
        btnLinkToRegister.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                Intent i = new Intent(LoginActivity.this,
                        RegisteryActivity.class);
                startActivity(i);
                finish();
            }
        });
    }
    private void checkLogin(final String email, final String password) {
        // Tag used to cancel the request
        try {
            String tag_string_req = "req_login";

            pDialog.setMessage("Logging in ...");
            showDialog();

            StringRequest strReq = new StringRequest(Request.Method.POST, AppConfig.URL_LOGIN, new Response.Listener<String>() {

                @Override
                public void onResponse(String response) {
                    Log.d(TAG, "Login Response: " + response.toString());
                    hideDialog();

                    try {
                        JSONObject jObj = new JSONObject(response);
                        boolean error = jObj.has("error");

                        if (!error) {
                            session.setLogin(true);
                            session.setToken(jObj.getJSONObject("data").getString("token"));
                            session.setEmail(email);
                            session.setPassword(password);
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    } catch (JSONException e) {
                        // JSON error
                        e.printStackTrace();
                        Toast.makeText(LoginActivity.this, "Json error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }

                }
            }, new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                    String body;
                    if(error.networkResponse != null)
                    {
                        String statusCode = String.valueOf(error.networkResponse.statusCode);
                        //get response body and parse with appropriate encoding
                        if (error.networkResponse.data != null) {
                            try {
                                body = new String(error.networkResponse.data, "UTF-8");
                                JSONObject jObj = new JSONObject(body);
                                Log.e(TAG, "Registration Error: " + jObj.get("error"));
                                Toast.makeText(LoginActivity.this, jObj.get("error").toString(), Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            Log.e(TAG, "Unknown Error: " + error.getMessage());
                            Toast.makeText(LoginActivity.this, "Unknown error", Toast.LENGTH_LONG).show();
                        }

                    }
                    hideDialog();
                    Toast.makeText(LoginActivity.this, "No network connection", Toast.LENGTH_LONG).show();
                }
            }) {

                @Override
                protected Map<String, String> getParams() {
                    // Posting parameters to login url
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("email", email);
                    params.put("password", password);

                    return params;
                }

            };

            // Adding request to request queue
            AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
        } catch (Exception e) {
            fm.saveFile(this,this,e.getMessage());
            hideDialog();
        }
    }
    private void showDialog() {
        if (!pDialog.isShowing())
            pDialog.show();
    }

    private void hideDialog() {
        if (pDialog.isShowing())
            pDialog.dismiss();
    }
}
