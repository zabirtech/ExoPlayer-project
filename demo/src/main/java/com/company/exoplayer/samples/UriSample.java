package com.company.exoplayer.samples;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.company.exoplayer.PlayerActivity;

import java.util.UUID;

public class UriSample extends Sample {

	public final String uri;
	public final String extension;

	public UriSample(String name, UUID drmSchemeUuid, String drmLicenseUrl,
			String[] drmKeyRequestProperties, String uri,
			String extension) {
		super(name, drmSchemeUuid, drmLicenseUrl, drmKeyRequestProperties);
		this.uri = uri;
		this.extension = extension;
	}

	@Override
	public Intent buildIntent(Context context) {
		return super.buildIntent(context)
				.setData(Uri.parse(uri))
				.putExtra(PlayerActivity.EXTENSION_EXTRA, extension)
				.setAction(PlayerActivity.ACTION_VIEW);
	}
}
