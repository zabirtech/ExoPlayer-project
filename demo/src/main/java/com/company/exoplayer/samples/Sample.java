package com.company.exoplayer.samples;

import android.content.Context;
import android.content.Intent;

import com.company.exoplayer.PlayerActivity;

import java.util.UUID;

public abstract class Sample {

	public final String   name;
	public final UUID     drmSchemeUuid;
	public final String   drmLicenseUrl;
	public final String[] drmKeyRequestProperties;

	public Sample(String name, UUID drmSchemeUuid, String drmLicenseUrl,
			String[] drmKeyRequestProperties) {
		this.name = name;
		this.drmSchemeUuid = drmSchemeUuid;
		this.drmLicenseUrl = drmLicenseUrl;
		this.drmKeyRequestProperties = drmKeyRequestProperties;
	}

	public Intent buildIntent(Context context) {
		Intent intent = new Intent(context, PlayerActivity.class);
		if (drmSchemeUuid != null) {
			intent.putExtra(PlayerActivity.DRM_SCHEME_UUID_EXTRA, drmSchemeUuid.toString());
			intent.putExtra(PlayerActivity.DRM_LICENSE_URL, drmLicenseUrl);
			intent.putExtra(PlayerActivity.DRM_KEY_REQUEST_PROPERTIES, drmKeyRequestProperties);
		}
		return intent;
	}
}
