package com.company.exoplayer;

import android.annotation.SuppressLint;
import android.widget.TextView;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerLibraryInfo;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;

import java.util.Locale;

public final class DebugHelperView  implements Runnable, Player.EventListener{

	private static final int REFRESH_INTERVAL_MS = 1000;

	private final SimpleExoPlayer player;
	private final TextView        textView;

	private boolean started;
	private long    startTime;
	private long    bufferingTime;

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public void setBufferingTime(long bufferingTime) {
		this.bufferingTime = bufferingTime;
	}

	/**
	 * @param player The {@link SimpleExoPlayer} from which debug information should be obtained.
	 * @param textView The {@link TextView} that should be updated to display the information.
	 */
	public DebugHelperView(SimpleExoPlayer player, TextView textView) {
		this.player = player;
		this.textView = textView;
	}

	/**
	 * Starts periodic updates of the {@link TextView}. Must be called from the application's main
	 * thread.
	 */
	public void start() {
		if (started) {
			return;
		}
		started = true;
		player.addListener(this);
		updateAndPost();
	}

	/**
	 * Stops periodic updates of the {@link TextView}. Must be called from the application's main
	 * thread.
	 */
	public void stop() {
		if (!started) {
			return;
		}
		started = false;
		player.removeListener(this);
		textView.removeCallbacks(this);
	}

	// Player.EventListener implementation.

	@Override
	public void onLoadingChanged(boolean isLoading) {
		// Do nothing.
	}

	@Override
	public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
		updateAndPost();
	}

	@Override
	public void onRepeatModeChanged(int repeatMode) {
		// Do nothing.
	}

	@Override
	public void onPositionDiscontinuity() {
		updateAndPost();
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
	public void onPlayerError(ExoPlaybackException error) {
		// Do nothing.
	}

	@Override
	public void onTracksChanged(TrackGroupArray tracks, TrackSelectionArray selections) {
		// Do nothing.
	}

	// Runnable implementation.

	@Override
	public void run() {
		updateAndPost();
	}

	// Private methods.

	@SuppressLint("SetTextI18n")
	private void updateAndPost() {
		textView.setText(getPlayerStateString() + getPlayerWindowIndexString() + getVideoString()
								 + getAudioString() + getStreamType() + getOpeningTime() + getPlayerVersion());
		textView.removeCallbacks(this);
		textView.postDelayed(this, REFRESH_INTERVAL_MS);
	}

	private String getPlayerStateString() {
		String text = "playWhenReady:" + player.getPlayWhenReady() + " playbackState:";
		switch (player.getPlaybackState()) {
			case Player.STATE_BUFFERING:
				text += "buffering";
				break;
			case Player.STATE_ENDED:
				text += "ended";
				break;
			case Player.STATE_IDLE:
				text += "idle";
				break;
			case Player.STATE_READY:
				text += "ready";
				break;
			default:
				text += "unknown";
				break;
		}
		return text;
	}

	private String getPlayerWindowIndexString() {
		return " window:" + player.getCurrentWindowIndex();
	}

	private String getVideoString() {
		Format format = player.getVideoFormat();
		if (format == null) {
			return "";
		}
		return "\n" + format.sampleMimeType + "(id:" + format.id + " r:" + format.width + "x"
				+ format.height + getPixelAspectRatioString(format.pixelWidthHeightRatio)
				+ getDecoderCountersBufferCountString(player.getVideoDecoderCounters()) + ")";
	}

	private String getAudioString() {
		Format format = player.getAudioFormat();
		if (format == null) {
			return "";
		}
		return "\n" + format.sampleMimeType + "(id:" + format.id + " hz:" + format.sampleRate + " ch:"
				+ format.channelCount
				+ getDecoderCountersBufferCountString(player.getAudioDecoderCounters()) + ")";
	}


	private static String getDecoderCountersBufferCountString(DecoderCounters counters) {
		if (counters == null) {
			return "";
		}
		counters.ensureUpdated();
		return " rb:" + counters.renderedOutputBufferCount
				+ " sb:" + counters.skippedOutputBufferCount
				+ " db:" + counters.droppedOutputBufferCount
				+ " mcdb:" + counters.maxConsecutiveDroppedOutputBufferCount;

	}

	private static String getPixelAspectRatioString(float pixelAspectRatio) {
		return pixelAspectRatio == Format.NO_VALUE || pixelAspectRatio == 1f ? ""
				: (" par:" + String.format(Locale.US, "%.02f", pixelAspectRatio));
	}

	public String getStreamType() {

		String type = "\nType: ";

		switch (player.getPlaybackState()) {
				case Player.STATE_READY:

					if (player.isCurrentWindowDynamic()) {
						type = type + "Dynamic|LIVE";
					} else {
						type = type + "Static|VOD" ;
					}
					break;
				default:
					type = type + "-";
			}

			return type;

	}

	public String getOpeningTime(){

		return " Opening Time:" + startTime + " Buffering Time:" + bufferingTime;
	}

	public String getPlayerVersion (){

		return " Version: " + ExoPlayerLibraryInfo.VERSION;
	}



}


