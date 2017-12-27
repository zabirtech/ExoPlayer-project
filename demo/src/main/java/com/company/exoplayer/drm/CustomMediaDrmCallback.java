package com.company.exoplayer.drm;

import android.net.Uri;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.drm.ExoMediaDrm;
import com.google.android.exoplayer2.drm.MediaDrmCallback;
import com.google.android.exoplayer2.upstream.DataSourceInputStream;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CustomMediaDrmCallback implements MediaDrmCallback {

	private final HttpDataSource.Factory dataSourceFactory;
	private final String                 defaultUrl;
	private final Map<String, String>    keyRequestProperties;

	/**
	 * @param defaultUrl        The default license URL.
	 * @param dataSourceFactory A factory from which to obtain {@link HttpDataSource} instances.
	 */
	public CustomMediaDrmCallback(String defaultUrl, String tokenUrl, HttpDataSource.Factory dataSourceFactory) {
		this(defaultUrl, tokenUrl, dataSourceFactory, new HashMap<String, String>());
	}

	/**
	 * @param defaultUrl           The default license URL.
	 * @param dataSourceFactory    A factory from which to obtain {@link HttpDataSource} instances.
	 * @param keyRequestProperties Request properties to set when making key requests, or null.
	 */
	public CustomMediaDrmCallback(String defaultUrl, String tokenUrl, HttpDataSource.Factory dataSourceFactory,
			Map<String, String> keyRequestProperties) {
		this.dataSourceFactory = dataSourceFactory;
		this.defaultUrl = defaultUrl;
		this.keyRequestProperties = keyRequestProperties;

	}

	@Override
	public byte[] executeProvisionRequest(UUID uuid, ExoMediaDrm.ProvisionRequest provisionRequest) throws IOException {
		String url = provisionRequest.getDefaultUrl() + "&signedRequest=" + new String(provisionRequest.getData());
		return executePost(dataSourceFactory, url, new byte[0], null);
	}

	@Override
	public byte[] executeKeyRequest(UUID uuid, ExoMediaDrm.KeyRequest keyRequest) throws Exception {

		Map<String, String> requestProperties = new HashMap<>();
		requestProperties.put("Content-Type", "text/xml");

		// Add additional request properties.
		synchronized (keyRequestProperties) {
			requestProperties.putAll(keyRequestProperties);
		}
		return executePost(dataSourceFactory, defaultUrl, keyRequest.getData(), requestProperties);
	}

	private static byte[] executePost(HttpDataSource.Factory dataSourceFactory, String url,
			byte[] data, Map<String, String> requestProperties) throws IOException {
		HttpDataSource dataSource = dataSourceFactory.createDataSource();
		if (requestProperties != null) {
			for (Map.Entry<String, String> requestProperty : requestProperties.entrySet()) {
				dataSource.setRequestProperty(requestProperty.getKey(), requestProperty.getValue());
			}
		}
		DataSpec dataSpec = new DataSpec(Uri.parse(url), data, 0, 0, C.LENGTH_UNSET, null,
										 DataSpec.FLAG_ALLOW_GZIP);
		DataSourceInputStream inputStream = new DataSourceInputStream(dataSource, dataSpec);
		try {
			return Util.toByteArray(inputStream);
		}
		finally {
			Util.closeQuietly(inputStream);
		}
	}
}