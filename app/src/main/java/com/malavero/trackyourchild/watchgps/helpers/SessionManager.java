package com.malavero.trackyourchild.watchgps.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Created by Kiełson on 01.05.2018.
 */

public class SessionManager {
    private static String TAG = SessionManager.class.getSimpleName();

    // Shared Preferences
    SharedPreferences pref;

    SharedPreferences.Editor editor;
    Context _context;

    // Shared pref mode
    int PRIVATE_MODE = 0;

    // Shared preferences file name
    private static final String PREF_NAME = "AndroidHiveLogin";

    private static final String KEY_IS_LOGGEDIN = "isLoggedIn";

    private static final String EMAIL = "Email";
    private static final String PASSWORD = "Password";
    private static final String TOKEN = "Autentication";

    public SessionManager(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }
    public boolean isLoggedIn(){
        return pref.getBoolean(KEY_IS_LOGGEDIN, false);
    }

    public void setLogin(boolean isLoggedIn) {

        editor.putBoolean(KEY_IS_LOGGEDIN, isLoggedIn);
        // commit changes
        editor.commit();

        Log.d(TAG, "User login session modified!");
    }
    public void setEmail(String email){
        editor.putString(EMAIL, email);
        editor.commit();
    }
    public void setPassword(String password){
        editor.putString(PASSWORD, password);
        editor.commit();
    }
    public void setToken(String token)
    {

        editor.putString(TOKEN, token);
        // commit changes
        editor.commit();

        Log.d(TAG, "Token added successfully");
    }

    public String getToken()
    {
        return pref.getString(TOKEN,"");
    }

    public String getPassword(){return pref.getString(PASSWORD,"");}

    public String getEmail(){return pref.getString(EMAIL,"");}
}
