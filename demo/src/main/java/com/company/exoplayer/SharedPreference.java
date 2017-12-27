package com.company.exoplayer;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;



public class SharedPreference {

	public static final String PREFS_NAME     = "com.company.exoplayer";
	public static final String PREFS_USER     = "PREFS_USER";
	public static final String PREFS_PASSWORD = "PREFS_PASSWORD";

	public SharedPreference() {
		super();
	}

	public void save (Context context, String text){
		SharedPreferences settings;
		Editor editor;

		settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		editor =settings.edit();

		editor.putString(PREFS_USER, text);

		editor.commit();
	}

	public void savePassword (Context context, String text){
		SharedPreferences settings;
		Editor editor;

		settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		editor =settings.edit();

		editor.putString(PREFS_PASSWORD , text);

		editor.commit();
	}

	public String getUser(Context context){
		SharedPreferences settings;
		String text;

		settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		text = settings.getString(PREFS_USER, "");
		return text;
	}

	public String getPassword(Context context) {
		SharedPreferences settings;
		String text;

		settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		text = settings.getString(PREFS_PASSWORD, "");
		return text;
	}




}
