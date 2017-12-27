package com.company.exoplayer;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.JsonReader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.company.exoplayer.samples.UriSample;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.UUID;

public class OpenByGuidFragment extends Fragment {

	private EditText         enterGuidTextView;
	private Button           openByGuidButton;
	private SharedPreference sharedPreference;
	private LoginLoader      loaderTask;
	private OkHttpClient     client;
	private CookieHandler    CookieHandler;

	public OpenByGuidFragment() {

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		client = new OkHttpClient();
		CookieManager cookieManager = new CookieManager();
		cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
		client.setCookieHandler(cookieManager);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_open_by_guid, container, false);

		enterGuidTextView = view.findViewById(R.id.EnterGuTextView);
		openByGuidButton = view.findViewById(R.id.Guid);

		openByGuidButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				funOpenByGuidButton();
			}
		});

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();

		loaderTask = new LoginLoader();

		loaderTask.execute();
	}

	protected final class LoginLoader extends AsyncTask<Void, Void, Boolean> {

		SharedPreference sharedPreference = new SharedPreference();
		String           username         = sharedPreference.getUser(getActivity());
		String           password         = sharedPreference.getPassword(getActivity());
		String tokenUrl;
		private ProgressDialog dialog = new ProgressDialog(getActivity());

		public LoginLoader() {
			tokenUrl = "www.google.com/" + "&username=" + username +
					"&password=" + password + "key";
		}

		@Override
		protected void onPreExecute() {
			this.dialog.setMessage("Please Wait");
			this.dialog.show();
		}

		@Override
		protected Boolean doInBackground(Void... voids) {
			try {
				Log.d("TAG", "requestlogin: ");
				Request req = new Request.Builder()
						.url(tokenUrl)
						.build();
				Log.d("TAG", "Response: ");
				Response res = client.newCall(req).execute();
				Log.d("TAG", "stringbody: ");
				String body = res.body().string();

				res.body().close();
				Log.d("TAG", "ReturnBody: " + body);
				return Boolean.valueOf(body);
			}
			catch (IOException e) {
				Log.e("tag", "error", e);
				e.printStackTrace();
				return null;
			}
		}

		@Override
		protected void onPostExecute(final Boolean success) {
			if (dialog.isShowing()) {
				dialog.dismiss();
			}
		}
	}

	public void funOpenByGuidButton() {
		String Guid = enterGuidTextView.getText().toString().trim();
		Log.d("TAG", "funOpenByGuidButton: ");
		new Getbyguidtask(Guid).execute();
	}

	protected class Getbyguidtask extends AsyncTask<String, Void, UriSample> {
		String btnGuidUrl;
		public UUID drmSchemeUuid;

		public Getbyguidtask(String guid) {

			if (guid.endsWith("") || guid.endsWith("") || guid.endsWith("") || guid.endsWith("")) {
				btnGuidUrl = "" +
						"" + guid;
			}
			else {
				btnGuidUrl = "" + ""
						+ guid;
			}
		}

		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected UriSample doInBackground(String... uris) {

			Log.d("TAG", "doInBackground: ");
			try {
				Log.d("TAG", "call: ");
				OkHttpClient btnclient = client;

				Log.d("TAG", "request: " + btnGuidUrl);
				Request btnreq = new Request.Builder()

						.url(btnGuidUrl)
						.build();
				Response btnres = btnclient.newCall(btnreq).execute();
				Log.d("TAG", "urisample: code: " + btnres.code());
				if (btnres.code() == 200) {
					UriSample guidsample;
					guidsample = parseSampleUri(new JsonReader(new InputStreamReader(btnres.body().byteStream())));
					Log.d("TAG", "Jsonreader: " + guidsample);
					Log.d("TAG", "return parseSampleUri: " + guidsample);
					btnres.body().close();
					return guidsample;
				}
				else {
					return null;
				}
			}
			catch (IOException b) {
				Log.e("tag", "click", b);
				b.printStackTrace();
				return null;
			}
		}

		public UriSample parseSampleUri(JsonReader reader) throws IOException {
			String sampleName = null;
			UriSample uri = null;

			Log.d("TAG", "parseSampleUri begin read object:" + reader);
			reader.beginObject();
			while (reader.hasNext()) {
				String name = reader.nextName();
				switch (name) {
					case "name":
						Log.d("TAG", "parseSampleUri:name reader " + sampleName);
						sampleName = reader.nextString();
						break;
					case "_links":
						Log.d("TAG", "parseSampleUri:readURI ");
						uri = readUri(reader);
						break;
					default:
						reader.skipValue();
						break;
				}
			}

			Log.d("TAG", "reader.close: ");
			reader.endObject();
			Log.d("TAG", "parseSampleUri: ");
			return uri;
		}

		public UriSample readUri(JsonReader reader) throws IOException {
			String link = null;
			String licenselink = null;

			reader.beginObject();
			while (reader.hasNext()) {
				String linksobj = reader.nextName();
				if (linksobj.equals("company:encryptedPlaylist")) {
					link = readhref(reader);
				}
				else if (linksobj.equals("company:widevineLicense")) {
					licenselink = readhref(reader);
					Log.d("TAG", "readUri: " + licenselink);
				}
				else {
					reader.skipValue();
				}
			}
			reader.endObject();
			drmSchemeUuid = MainActivity.getDrmUuid("widevine-custom");

			return new UriSample(null, drmSchemeUuid, licenselink, null, link, null
			);
		}

		private String readhref(JsonReader reader) throws IOException {
			String href = null;
			reader.beginObject();
			while (reader.hasNext()) {
				String playlistobj = reader.nextName();
				if (playlistobj.equals("href")) {
					href = reader.nextString();
				}
				else {
					reader.skipValue();
				}
			}
			reader.endObject();
			return href;
		}

		@Override
		protected void onPostExecute(UriSample guidsample) {
			if (guidsample != null)

			{
				startActivity(guidsample.buildIntent(getContext()));
			}

			else if (guidsample == null) {
				Toast.makeText(getContext(), "Wrong guid or media guid id", Toast.LENGTH_SHORT).show();
			}
		}
	}
}






