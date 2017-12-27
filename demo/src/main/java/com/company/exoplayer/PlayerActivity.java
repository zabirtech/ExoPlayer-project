/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.company.exoplayer;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Player.EventListener;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer.DecoderInitializationException;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil.DecoderQueryException;
import com.google.android.exoplayer2.source.BehindLiveWindowException;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.company.exoplayer.drm.CustomMediaDrmCallback;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.UUID;


/**
 * An activity that plays media using {@link SimpleExoPlayer}.
 */
public class PlayerActivity extends Activity implements OnClickListener, EventListener,
		PlaybackControlView.VisibilityListener {
	private static final String TAG = "PlayerActivity";

	public static final String DRM_SCHEME_UUID_EXTRA      = "drm_scheme_uuid";
	public static final String DRM_LICENSE_URL            = "drm_license_url";
	public static final String DRM_KEY_REQUEST_PROPERTIES = "drm_key_request_properties";

	public static final String ACTION_VIEW     = "com.google.android.exoplayer.demo.action.VIEW";
	public static final String EXTENSION_EXTRA = "extension";

	public static final String ACTION_VIEW_LIST     =
			"com.google.android.exoplayer.demo.action.VIEW_LIST";
	public static final String URI_LIST_EXTRA       = "uri_list";
	public static final String EXTENSION_LIST_EXTRA = "extension_list";

	private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
	private static final CookieManager DEFAULT_COOKIE_MANAGER;



	static {
		DEFAULT_COOKIE_MANAGER = new CookieManager();
		DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
	}

	private Handler             mainHandler;
	private EventLogger         eventLogger;
	private SimpleExoPlayerView simpleExoPlayerView;
	private LinearLayout        debugRootView;
	private TextView            debugTextView;
	private Button              retryButton;

	private DataSource.Factory   mediaDataSourceFactory;
	private SimpleExoPlayer      player;
	private DefaultTrackSelector trackSelector;
	private TrackSelectionHelper trackSelectionHelper;
	private DebugHelperView      debugViewHelper;
	private boolean              inErrorState;
	private TrackGroupArray      lastSeenTrackGroupArray;

	private boolean shouldAutoPlay;
	private int     resumeWindow;
	private long    resumePosition;
	private boolean streamStart;
	private long    bufferingStartTime;
	private long    startTime;

	// Activity lifecycle

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		shouldAutoPlay = true;
		clearResumePosition();
		mediaDataSourceFactory = buildDataSourceFactory(true);
		mainHandler = new Handler();
		if (CookieHandler.getDefault() != DEFAULT_COOKIE_MANAGER) {
			CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER);
		}

		setContentView(R.layout.activity_player);
		View rootView = findViewById(R.id.root);
		rootView.setOnClickListener(this);
		debugRootView = findViewById(R.id.controls_root);
		debugTextView = findViewById(R.id.debug_text_view);
		retryButton = findViewById(R.id.retry_button);
		retryButton.setOnClickListener(this);

		simpleExoPlayerView = findViewById(R.id.player_view);
		simpleExoPlayerView.setControllerVisibilityListener(this);
		simpleExoPlayerView.requestFocus();
	}

	@Override
	public void onNewIntent(Intent intent) {
		releasePlayer();
		shouldAutoPlay = true;
		clearResumePosition();
		setIntent(intent);
	}

	@Override
	public void onStart() {
		super.onStart();
		if (Util.SDK_INT > 23) {
			initializePlayer();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if ((Util.SDK_INT <= 23 || player == null)) {
			initializePlayer();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (Util.SDK_INT <= 23) {
			releasePlayer();
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		if (Util.SDK_INT > 23) {
			releasePlayer();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
			@NonNull int[] grantResults) {
		if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			initializePlayer();
		}
		else {
			showToast(R.string.storage_permission_denied);
			finish();
		}
	}

	// Activity input

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		// If the event was not handled then see if the player view can handle it.
		return super.dispatchKeyEvent(event) || simpleExoPlayerView.dispatchKeyEvent(event);
	}

	// OnClickListener methods

	@Override
	public void onClick(View view) {
		if (view == retryButton) {
			initializePlayer();
		}
		else if (view.getParent() == debugRootView) {
			MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
			if (mappedTrackInfo != null) {
				trackSelectionHelper.showSelectionDialog(this, ((Button) view).getText(),
														 trackSelector.getCurrentMappedTrackInfo(), (int) view.getTag());
			}
		}
	}

	// PlaybackControlView.VisibilityListener implementation

	@Override
	public void onVisibilityChange(int visibility) {
		debugRootView.setVisibility(visibility);
	}

	// Internal methods

	private void initializePlayer() {

		startTime = System.nanoTime();

		Intent intent = getIntent();
		boolean needNewPlayer = player == null;
		if (needNewPlayer) {
			TrackSelection.Factory adaptiveTrackSelectionFactory =
					new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);
			trackSelector = new DefaultTrackSelector(adaptiveTrackSelectionFactory);
			trackSelectionHelper = new TrackSelectionHelper(trackSelector, adaptiveTrackSelectionFactory);
			lastSeenTrackGroupArray = null;
			eventLogger = new EventLogger(trackSelector);

			UUID drmSchemeUuid = intent.hasExtra(DRM_SCHEME_UUID_EXTRA)
					? UUID.fromString(intent.getStringExtra(DRM_SCHEME_UUID_EXTRA)) : null;
			DrmSessionManager<FrameworkMediaCrypto> drmSessionManager = null;
			if (drmSchemeUuid != null) {
				String drmLicenseUrl = intent.getStringExtra(DRM_LICENSE_URL);
				String[] keyRequestPropertiesArray = intent.getStringArrayExtra(DRM_KEY_REQUEST_PROPERTIES);
				int errorStringId = R.string.error_drm_unknown;
				if (Util.SDK_INT < 18) {
					errorStringId = R.string.error_drm_not_supported;
				}
				else {
					try {
						drmSessionManager = buildDrmSessionManagerV18(drmSchemeUuid, drmLicenseUrl,
																	  keyRequestPropertiesArray);
					}
					catch (UnsupportedDrmException e) {
						errorStringId = e.reason == UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME
								? R.string.error_drm_unsupported_scheme : R.string.error_drm_unknown;
					}
				}
				if (drmSessionManager == null) {
					showToast(errorStringId);
					return;
				}
			}

			@DefaultRenderersFactory.ExtensionRendererMode int extensionRendererMode =
					DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF;
			DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(this,
																				   drmSessionManager, extensionRendererMode);

			player = ExoPlayerFactory.newSimpleInstance(renderersFactory, trackSelector);
			player.addListener(this);
			player.addListener(eventLogger);
			player.addMetadataOutput(eventLogger);
			player.setAudioDebugListener(eventLogger);
			player.setVideoDebugListener(eventLogger);

			simpleExoPlayerView.setPlayer(player);
			player.setPlayWhenReady(shouldAutoPlay);
			debugViewHelper = new DebugHelperView(player, debugTextView);
			debugViewHelper.start();
		}
		String action = intent.getAction();
		Uri[] uris;
		String[] extensions;
		if (ACTION_VIEW.equals(action)) {
			uris = new Uri[]{ intent.getData() };
			extensions = new String[]{ intent.getStringExtra(EXTENSION_EXTRA) };
		}
		else if (ACTION_VIEW_LIST.equals(action)) {
			String[] uriStrings = intent.getStringArrayExtra(URI_LIST_EXTRA);
			uris = new Uri[uriStrings.length];
			for (int i = 0; i < uriStrings.length; i++) {
				uris[i] = Uri.parse(uriStrings[i]);
			}
			extensions = intent.getStringArrayExtra(EXTENSION_LIST_EXTRA);
			if (extensions == null) {
				extensions = new String[uriStrings.length];
			}
		}
		else {
			showToast(getString(R.string.unexpected_intent_action, action));
			return;
		}
		if (Util.maybeRequestReadExternalStoragePermission(this, uris)) {
			// The player will be reinitialized if the permission is granted.
			return;
		}
		MediaSource[] mediaSources = new MediaSource[uris.length];
		for (int i = 0; i < uris.length; i++) {
			mediaSources[i] = buildMediaSource(uris[i], extensions[i]);
		}
		MediaSource mediaSource = mediaSources.length == 1 ? mediaSources[0]
				: new ConcatenatingMediaSource(mediaSources);

		boolean haveResumePosition = resumeWindow != C.INDEX_UNSET;
		if (haveResumePosition) {
			player.seekTo(resumeWindow, resumePosition);
		}

		player.prepare(mediaSource, !haveResumePosition, false);
		inErrorState = false;
		updateButtonVisibilities();
	}

	private MediaSource buildMediaSource(Uri uri, String overrideExtension) {
		int type = TextUtils.isEmpty(overrideExtension) ? Util.inferContentType(uri)
				: Util.inferContentType("." + overrideExtension);
		switch (type) {

			case C.TYPE_DASH:
				return new DashMediaSource(uri, buildDataSourceFactory(false),
										   new DefaultDashChunkSource.Factory(mediaDataSourceFactory), mainHandler, eventLogger);

			case C.TYPE_OTHER:
				return new ExtractorMediaSource(uri, mediaDataSourceFactory, new DefaultExtractorsFactory(),
												mainHandler, eventLogger);
			default: {
				throw new IllegalStateException("Unsupported type: " + type);
			}
		}
	}

	private DrmSessionManager<FrameworkMediaCrypto> buildDrmSessionManagerV18(UUID uuid,
			String licenseUrl, String[] keyRequestPropertiesArray) throws UnsupportedDrmException {
		SharedPreference sharedPreference = new SharedPreference();
		String username = sharedPreference.getUser(this);
		String password = sharedPreference.getPassword(this);
		String tokenUrl = "companylink" + username +
				"&password=" + password;

		CustomMediaDrmCallback drmCallback = new CustomMediaDrmCallback(licenseUrl, tokenUrl,
																		buildHttpDataSourceFactory(false));


		return new DefaultDrmSessionManager<>(uuid, FrameworkMediaDrm.newInstance(uuid), drmCallback,
											  null, mainHandler, eventLogger);
	}

	private void releasePlayer() {
		if (player != null) {
			debugViewHelper.stop();
			debugViewHelper = null;
			shouldAutoPlay = player.getPlayWhenReady();
			updateResumePosition();
			player.release();
			player = null;
			trackSelector = null;
			trackSelectionHelper = null;
			eventLogger = null;
		}
	}

	private void updateResumePosition() {
		resumeWindow = player.getCurrentWindowIndex();
		resumePosition = Math.max(0, player.getContentPosition());
	}

	private void clearResumePosition() {
		resumeWindow = C.INDEX_UNSET;
		resumePosition = C.TIME_UNSET;
	}

	/**
	 * Returns a new DataSource factory.
	 *
	 * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
	 *                          DataSource factory.
	 *
	 * @return A new DataSource factory.
	 */
	private DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter) {
		return ((DemoApplication) getApplication())
				.buildDataSourceFactory(useBandwidthMeter ? BANDWIDTH_METER : null);
	}

	/**
	 * Returns a new HttpDataSource factory.
	 *
	 * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
	 *                          DataSource factory.
	 *
	 * @return A new HttpDataSource factory.
	 */
	private HttpDataSource.Factory buildHttpDataSourceFactory(boolean useBandwidthMeter) {
		return ((DemoApplication) getApplication())
				.buildHttpDataSourceFactory(useBandwidthMeter ? BANDWIDTH_METER : null);

	}

	@Override
	public void onLoadingChanged(boolean isLoading) {
		// Do nothing.
	}

	@Override
	public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
		if (playbackState == Player.STATE_BUFFERING) {
			bufferingStartTime = System.nanoTime();
		}
		else if (playbackState == Player.STATE_READY) {
			if(!streamStart) {
				long playbackStartedTime = System.nanoTime();
				long bufferingTime = (playbackStartedTime - bufferingStartTime) / 1000000;
				debugViewHelper.setBufferingTime(bufferingTime);

				long diff = (playbackStartedTime - startTime) / 1000000;
				Log.i(TAG, "Exoplayer ------" + diff);
				debugViewHelper.setStartTime(diff);
			}
			streamStart = true;

			showControls();
		}
		updateButtonVisibilities();
	}

	@Override
	public void onRepeatModeChanged(int repeatMode) {
		// Do nothing.
	}

	@Override
	public void onPositionDiscontinuity() {
		if (inErrorState) {
			// This will only occur if the user has performed a seek whilst in the error state. Update the
			// resume position so that if the user then retries, playback will resume from the position to
			// which they seeked.
			updateResumePosition();
		}
	}

	@Override
	public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
		// Do nothing.
	}

	@Override
	public void onTimelineChanged(Timeline timeline, Object manifest) {
		// Do nothing.
	}

	@Override
	public void onPlayerError(ExoPlaybackException e) {
		String errorString = null;
		if (e.type == ExoPlaybackException.TYPE_RENDERER) {
			Exception cause = e.getRendererException();
			if (cause instanceof DecoderInitializationException) {
				// Special case for decoder initialization failures.
				DecoderInitializationException decoderInitializationException =
						(DecoderInitializationException) cause;
				if (decoderInitializationException.decoderName == null) {
					if (decoderInitializationException.getCause() instanceof DecoderQueryException) {
						errorString = getString(R.string.error_querying_decoders);
					}
					else if (decoderInitializationException.secureDecoderRequired) {
						errorString = getString(R.string.error_no_secure_decoder,
												decoderInitializationException.mimeType);
					}
					else {
						errorString = getString(R.string.error_no_decoder,
												decoderInitializationException.mimeType);
					}
				}
				else {
					errorString = getString(R.string.error_instantiating_decoder,
											decoderInitializationException.decoderName);
				}
			}
		}
		if (errorString != null) {
			showToast(errorString);
		}
		inErrorState = true;
		if (isBehindLiveWindow(e)) {
			clearResumePosition();
			initializePlayer();
		}
		else {
			updateResumePosition();
			updateButtonVisibilities();
			showControls();
		}
	}

	@Override
	@SuppressWarnings("ReferenceEquality")
	public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
		updateButtonVisibilities();
		if (trackGroups != lastSeenTrackGroupArray) {
			MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
			if (mappedTrackInfo != null) {
				if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_VIDEO)
						== MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
					showToast(R.string.error_unsupported_video);
				}
				if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_AUDIO)
						== MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
					showToast(R.string.error_unsupported_audio);
				}
			}
			lastSeenTrackGroupArray = trackGroups;
		}
	}

	// User controls

	private void updateButtonVisibilities() {
		debugRootView.removeAllViews();

		retryButton.setVisibility(inErrorState ? View.VISIBLE : View.GONE);
		debugRootView.addView(retryButton);

		if (player == null) {
			return;
		}

		MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
		if (mappedTrackInfo == null) {
			return;
		}

		for (int i = 0; i < mappedTrackInfo.length; i++) {
			TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(i);
			if (trackGroups.length != 0) {
				Button button = new Button(this);
				int label;
				switch (player.getRendererType(i)) {
					case C.TRACK_TYPE_AUDIO:
						label = R.string.audio;
						break;
					case C.TRACK_TYPE_VIDEO:
						label = R.string.video;
						break;
					case C.TRACK_TYPE_TEXT:
						label = R.string.text;
						break;
					default:
						continue;
				}
				button.setText(label);
				button.setTag(i);
				button.setOnClickListener(this);
				debugRootView.addView(button, debugRootView.getChildCount() - 1);
			}
		}
	}

	private void showControls() {
		debugRootView.setVisibility(View.VISIBLE);
	}

	private void showToast(int messageId) {
		showToast(getString(messageId));
	}

	private void showToast(String message) {
		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
	}

	private static boolean isBehindLiveWindow(ExoPlaybackException e) {
		if (e.type != ExoPlaybackException.TYPE_SOURCE) {
			return false;
		}
		Throwable cause = e.getSourceException();
		while (cause != null) {
			if (cause instanceof BehindLiveWindowException) {
				return true;
			}
			cause = cause.getCause();
		}
		return false;
	}
}