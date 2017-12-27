package com.company.exoplayer;

import android.content.Context;
import android.util.Log;



public class LoginHandler {
	private final String TAG = "LoginHandler";

	public void saveUser(Context context, String username, String password) {
		// Some magic code of login
		Log.d("LoginHandler", "Save user now");
		Log.d("LoginHandler", username);
		Log.d("LoginHandler", password);

		SharedPreference sharedPreference = new SharedPreference();
		sharedPreference.save(context, username);
		sharedPreference.savePassword(context, password);

	}
}
