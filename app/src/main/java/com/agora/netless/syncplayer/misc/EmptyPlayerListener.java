package com.agora.netless.syncplayer.misc;

import com.herewhite.sdk.PlayerListener;
import com.herewhite.sdk.domain.PlayerPhase;
import com.herewhite.sdk.domain.PlayerState;
import com.herewhite.sdk.domain.SDKError;

/**
 * whiteboard player listener adapter.
 * <p>
 * reduce unimportant code that interferes with Samples
 */
public class EmptyPlayerListener implements PlayerListener {
    @Override
    public void onPhaseChanged(PlayerPhase phase) {

    }

    @Override
    public void onLoadFirstFrame() {

    }

    @Override
    public void onSliceChanged(String slice) {

    }

    @Override
    public void onPlayerStateChanged(PlayerState modifyState) {

    }

    @Override
    public void onStoppedWithError(SDKError error) {

    }

    @Override
    public void onScheduleTimeChanged(long time) {

    }

    @Override
    public void onCatchErrorWhenAppendFrame(SDKError error) {

    }

    @Override
    public void onCatchErrorWhenRender(SDKError error) {

    }
}
