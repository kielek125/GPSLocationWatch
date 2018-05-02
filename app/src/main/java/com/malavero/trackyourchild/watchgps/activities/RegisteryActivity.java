package com.malavero.trackyourchild.watchgps.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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

public class RegisteryActivity extends Activity {
    private static final String TAG = RegisteryActivity.class.getSimpleName();
    private Button btnRegister;
    private Button btnLinkToLogin;
    private EditText inputFullName;
    private EditText inputEmail;
    private EditText inputPassword;
    private ProgressDialog pDialog;
    private SessionManager session;
    private FileManager fm;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initProperty();
        // Register Button Click event
        registerButton();
    }

    private void initProperty() {
        inputFullName = (EditText) findViewById(R.id.name);
        inputEmail = (EditText) findViewById(R.id.email);
        inputPassword = (EditText) findViewById(R.id.password);
        btnRegister = (Button) findViewById(R.id.btnRegister);
        btnLinkToLogin = (Button) findViewById(R.id.btnLinkToLoginScreen);

        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        // Session manager
        session = new SessionManager(getApplicationContext());
        fm = new FileManager();
    }

    private void registerButton() {
        btnRegister.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                String name = inputFullName.getText().toString().trim();
                String email = inputEmail.getText().toString().trim();
                String password = inputPassword.getText().toString().trim();

                inputFullName.onEditorAction(EditorInfo.IME_ACTION_DONE);
                inputEmail.onEditorAction(EditorInfo.IME_ACTION_DONE);
                inputPassword.onEditorAction(EditorInfo.IME_ACTION_DONE);

                if (!name.isEmpty() && !email.isEmpty() && !password.isEmpty()) {
                    registerUser(name, email, password);
                } else {
                    Toast.makeText(RegisteryActivity.this,
                            "Please enter your details!", Toast.LENGTH_LONG)
                            .show();
                }
            }
        });

        // Link to Login Screen
        btnLinkToLogin.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                Intent i = new Intent(RegisteryActivity.this,
                        LoginActivity.class);
                startActivity(i);
                finish();
            }
        });
    }

    /**
     * Function to store user in MySQL database will post params(tag, name,
     * email, password) to register url
     */
    private void registerUser(final String name, final String email,
                              final String password) {
        try {
            // Tag used to cancel the request
            String tag_string_req = "req_register";

            pDialog.setMessage("Registering ...");
            showDialog();

            StringRequest strReq = new StringRequest(Request.Method.POST,
                    AppConfig.URL_REGISTER, new Response.Listener<String>() {

                @Override
                public void onResponse(String response) {
                    Log.d(TAG, "Register Response: " + response.toString());
                    hideDialog();

                    try {
                        JSONObject jObj = new JSONObject(response);
                        boolean error = jObj.has("error");
                        if (!error) {
                            Toast.makeText(RegisteryActivity.this, "User successfully registered. Try login now!", Toast.LENGTH_LONG).show();
                            Utils.delay(3, new Utils.DelayCallback() {
                                @Override
                                public void afterDelay() {
                                    // Launch login activity
                                    Intent intent = new Intent(RegisteryActivity.this, LoginActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            });
                        } else {
                            String errorMsg = jObj.getString("error_msg");
                            Toast.makeText(RegisteryActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }, new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                    String body;
                    String statusCode = String.valueOf(error.networkResponse.statusCode);
                    //get response body and parse with appropriate encoding
                    if (error.networkResponse.data != null) {
                        try {
                            body = new String(error.networkResponse.data, "UTF-8");
                            JSONObject jObj = new JSONObject(body);
                            Log.e(TAG, "Registration Error: " + jObj.get("error"));
                            Toast.makeText(RegisteryActivity.this, jObj.get("error").toString(), Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        Log.e(TAG, "Unknown Error: " + error.getMessage());
                        Toast.makeText(RegisteryActivity.this, "Unknown error", Toast.LENGTH_LONG).show();
                    }
                    hideDialog();
                }
            }) {

                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("name", name);
                    params.put("email", email);
                    params.put("password", password);

                    return params;
                }

            };
            AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
        } catch (Exception e) {
            fm.saveFile(this,this,e.getMessage());
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
