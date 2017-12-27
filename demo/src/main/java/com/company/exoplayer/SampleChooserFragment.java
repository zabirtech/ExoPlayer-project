package com.company.exoplayer;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.JsonReader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.ParserException;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSourceInputStream;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.company.exoplayer.samples.PlayListSample;
import com.company.exoplayer.samples.Sample;
import com.company.exoplayer.samples.UriSample;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class SampleChooserFragment extends Fragment {

	private static final String TAG = "SampleChooserFragment";
	private SampleListLoader loaderTask;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_sample_list, container, false);
	}

	@Override
	public void onResume() {
		super.onResume();

		Intent intent = getActivity().getIntent();
		String dataUri = "https://docs.google.com/uc?authuser=1&id=1tVR_-b53Ywt8M2eXXJI4wEgcoYM5rB-D&export=download";
		String[] uris;
		if (dataUri != null) {
			uris = new String[]{ dataUri };
		}
		else {
			ArrayList<String> uriList = new ArrayList<>();
			AssetManager assetManager = getContext().getAssets();
			try {
				for (String asset : assetManager.list("")) {
					if (asset.endsWith(".exolist.json")) {
						uriList.add("asset:///" + asset);
					}
				}
			}
			catch (IOException e) {
				Toast.makeText(getContext(), R.string.sample_list_load_error, Toast.LENGTH_LONG)
						.show();
			}
			uris = new String[uriList.size()];
			uriList.toArray(uris);
			Arrays.sort(uris);
		}
		loaderTask = new SampleListLoader();
		loaderTask.execute(uris);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (loaderTask != null) {
			loaderTask.cancel(true);
		}
	}

	private void onSampleGroups(final List<SampleGroup> groups, boolean sawError) {
		if (sawError) {
			Toast.makeText(getContext(), R.string.sample_list_load_error, Toast.LENGTH_LONG)
					.show();
		}
		ExpandableListView sampleList = getView().findViewById(R.id.sample_list);
		sampleList.setAdapter(new SampleAdapter(groups));
		sampleList.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
			@Override
			public boolean onChildClick(ExpandableListView parent, View view, int groupPosition,
					int childPosition, long id) {
				onSampleSelected(groups.get(groupPosition).samples.get(childPosition));
				return true;
			}
		});
	}

	private void onSampleSelected(Sample sample) {

		SharedPreference sharedPreference = new SharedPreference();
		String username = sharedPreference.getUser(getActivity());
		String password = sharedPreference.getPassword(getActivity());
		/*if (username.isEmpty() || password.isEmpty()) {
			Toast.makeText(getContext(), "You need to login", Toast.LENGTH_SHORT).show();
		}*/
//		else
		{
			startActivity(sample.buildIntent(getContext()));
		}
	}

	private final class SampleListLoader extends AsyncTask<String, Void, List<SampleGroup>> {

		private boolean sawError;

		@Override
		protected List<SampleGroup> doInBackground(String... uris) {
			List<SampleGroup> result = new ArrayList<>();
			Context context = getContext();
			String userAgent = Util.getUserAgent(context, "ExoPlayerDemo");
			DataSource dataSource = new DefaultDataSource(context, null, userAgent, false);
			for (String uri : uris) {
				if (uri.startsWith("asset:///")) {

					DataSpec dataSpec = new DataSpec(Uri.parse(uri));
					InputStream inputStream = new DataSourceInputStream(dataSource, dataSpec);
					try {
						readSampleGroups(new JsonReader(new InputStreamReader(inputStream, "UTF-8")), result);
					}
					catch (Exception e) {
						Log.e(TAG, "Error loading sample list: " + uri, e);
						sawError = true;
					}
					finally {
						Util.closeQuietly(dataSource);
					}
				}
				else if (uri.startsWith("http")) {

					Request request = new Request.Builder().url(uri).build();

					try {
						OkHttpClient client = new OkHttpClient();
						Response response = client.newCall(request).execute();

						readSampleGroups(new JsonReader(new InputStreamReader(response.body().byteStream())), result);
					}
					catch (IOException e) {
						Log.e(TAG, "Error loading sample list: " + uri, e);
					}

					return result;
				}
			}
			return result;
		}

		@Override
		protected void onPostExecute(List<SampleGroup> result) {
			if (!isCancelled()) {
				onSampleGroups(result, sawError);
			}
		}

		private void readSampleGroups(JsonReader reader, List<SampleGroup> groups) throws IOException {
			reader.beginArray();
			while (reader.hasNext()) {
				readSampleGroup(reader, groups);
			}
			reader.endArray();
		}

		private void readSampleGroup(JsonReader reader, List<SampleGroup> groups) throws IOException {
			String groupName = "";
			ArrayList<Sample> samples = new ArrayList<>();

			reader.beginObject();
			while (reader.hasNext()) {
				String name = reader.nextName();
				switch (name) {
					case "name":
						groupName = reader.nextString();
						break;
					case "samples":
						reader.beginArray();
						while (reader.hasNext()) {
							samples.add(readEntry(reader, false));
						}
						reader.endArray();
						break;
					case "_comment":
						reader.nextString(); // Ignore.
						break;
					default:
						throw new ParserException("Unsupported name: " + name);
				}
			}
			reader.endObject();

			SampleGroup group = getGroup(groupName, groups);
			group.samples.addAll(samples);
		}

		private Sample readEntry(JsonReader reader, boolean insidePlaylist) throws IOException {
			String sampleName = null;
			String uri = null;
			String extension = null;
			UUID drmUuid = null;
			String drmLicenseUrl = null;
			String[] drmKeyRequestProperties = null;
			boolean preferExtensionDecoders = false;
			ArrayList<UriSample> playlistSamples = null;
			String adTagUri = null;

			reader.beginObject();
			while (reader.hasNext()) {
				String name = reader.nextName();
				switch (name) {
					case "name":
						sampleName = reader.nextString();
						break;
					case "uri":
						uri = reader.nextString();
						break;
					case "extension":
						extension = reader.nextString();
						break;
					case "drm_scheme":
						Assertions.checkState(!insidePlaylist, "Invalid attribute on nested item: drm_scheme");
						drmUuid = MainActivity.getDrmUuid(reader.nextString());
						break;
					case "drm_license_url":
						Assertions.checkState(!insidePlaylist,
											  "Invalid attribute on nested item: drm_license_url");
						drmLicenseUrl = reader.nextString();
						break;
					case "drm_key_request_properties":
						Assertions.checkState(!insidePlaylist,
											  "Invalid attribute on nested item: drm_key_request_properties");
						ArrayList<String> drmKeyRequestPropertiesList = new ArrayList<>();
						reader.beginObject();
						while (reader.hasNext()) {
							drmKeyRequestPropertiesList.add(reader.nextName());
							drmKeyRequestPropertiesList.add(reader.nextString());
						}
						reader.endObject();
						drmKeyRequestProperties = drmKeyRequestPropertiesList.toArray(new String[0]);
						break;
					case "prefer_extension_decoders":
						Assertions.checkState(!insidePlaylist,
											  "Invalid attribute on nested item: prefer_extension_decoders");
						preferExtensionDecoders = reader.nextBoolean();
						break;
					case "playlist":
						Assertions.checkState(!insidePlaylist, "Invalid nesting of playlists");
						playlistSamples = new ArrayList<>();
						reader.beginArray();
						while (reader.hasNext()) {
							playlistSamples.add((UriSample) readEntry(reader, true));
						}
						reader.endArray();
						break;

					default:
						throw new ParserException("Unsupported attribute name: " + name);
				}
			}
			reader.endObject();

			if (playlistSamples != null) {
				UriSample[] playlistSamplesArray = playlistSamples.toArray(
						new UriSample[playlistSamples.size()]);
				return new PlayListSample(sampleName, drmUuid, drmLicenseUrl, drmKeyRequestProperties,
										  playlistSamplesArray);
			}
			else {
				return new UriSample(sampleName, drmUuid, drmLicenseUrl, drmKeyRequestProperties,
									 uri, extension);
			}
		}

		private SampleGroup getGroup(String groupName, List<SampleGroup> groups) {
			for (int i = 0; i < groups.size(); i++) {
				if (Util.areEqual(groupName, groups.get(i).title)) {
					return groups.get(i);
				}
			}
			SampleGroup group = new SampleGroup(groupName);
			groups.add(group);
			return group;
		}
	}

	private static final class SampleAdapter extends BaseExpandableListAdapter {

		private final List<SampleGroup> sampleGroups;

		public SampleAdapter(List<SampleGroup> sampleGroups) {
			this.sampleGroups = sampleGroups;
		}

		@Override
		public Sample getChild(int groupPosition, int childPosition) {
			return getGroup(groupPosition).samples.get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
				View convertView, ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent,
																		false);
			}
			((TextView) view).setText(getChild(groupPosition, childPosition).name);
			return view;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return getGroup(groupPosition).samples.size();
		}

		@Override
		public SampleGroup getGroup(int groupPosition) {
			return sampleGroups.get(groupPosition);
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
				ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_expandable_list_item_1,
																		parent, false);
			}
			((TextView) view).setText(getGroup(groupPosition).title);
			return view;
		}

		@Override
		public int getGroupCount() {
			return sampleGroups.size();
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}
	}

	private static final class SampleGroup {

		public final String       title;
		public final List<Sample> samples;

		public SampleGroup(String title) {
			this.title = title;
			this.samples = new ArrayList<>();
		}
	}
}
