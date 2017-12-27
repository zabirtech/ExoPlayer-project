package com.company.exoplayer.drm;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class LicenseProxyRequest {

	public static String getSessionToken(String tokenUrl) {

		String sessionToken = null;

		String jsonstring = GetHTTPData(tokenUrl);

		try {
			JSONObject jo = new JSONObject(jsonstring);
			jo = jo.getJSONObject("signInResponse");
			sessionToken = jo.getString("token");
		}
		catch (JSONException e) {
			e.printStackTrace();
		}

		return sessionToken;
	}

	private static String GetHTTPData(String url) {
		URL accessurl = null;
		try {
			accessurl = new URL(url);
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}

		HttpURLConnection connection = null;
		InputStream responseStream = null;
		try {

			connection = (HttpURLConnection) accessurl.openConnection();
			connection.setRequestMethod("GET");
			connection.connect();

			responseStream = connection.getInputStream();
			ByteArrayOutputStream outStream = new ByteArrayOutputStream();
			byte[] data = new byte[1024];
			int count = -1;
			try {
				while ((count = responseStream.read(data, 0, 1024)) != -1) {
					outStream.write(data, 0, count);
				}
			}
			catch (IOException e) {
				return null;
			}
			finally {
				responseStream.close();
			}

			String response = null;
			try {
				response = new String(outStream.toByteArray(), "ISO-8859-1");
			}
			catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

			return response;
		}
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		finally {
			if (connection != null) {
				try {

					int httpStatusCode = connection.getResponseCode();
					if (httpStatusCode >= 400) {
						// no error handling
						if (httpStatusCode >= 500) {
							// no error handling
						}
					}
				}
				catch (IOException e1) {
				}

				connection.disconnect();
			}
		}
	}
}