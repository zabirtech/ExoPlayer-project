package com.company.exoplayer.samples;

import android.content.Context;
import android.content.Intent;

import com.company.exoplayer.PlayerActivity;

import java.util.UUID;

public class PlayListSample extends Sample {

	public final UriSample[] children;

	public PlayListSample(String name, UUID drmSchemeUuid, String drmLicenseUrl,
			String[] drmKeyRequestProperties, UriSample... children) {
		super(name, drmSchemeUuid, drmLicenseUrl, drmKeyRequestProperties);
		this.children = children;
	}

	@Override
	public Intent buildIntent(Context context) {
		String[] uris = new String[children.length];
		String[] extensions = new String[children.length];
		for (int i = 0; i < children.length; i++) {
			uris[i] = children[i].uri;
			extensions[i] = children[i].extension;
		}
		return super.buildIntent(context)
				.putExtra(PlayerActivity.URI_LIST_EXTRA, uris)
				.putExtra(PlayerActivity.EXTENSION_LIST_EXTRA, extensions)
				.setAction(PlayerActivity.ACTION_VIEW_LIST);
	}
}
