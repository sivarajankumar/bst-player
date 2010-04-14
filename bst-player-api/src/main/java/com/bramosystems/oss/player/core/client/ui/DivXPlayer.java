/*
 *  Copyright 2010 Sikiru Braheem
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package com.bramosystems.oss.player.core.client.ui;

import com.bramosystems.oss.player.core.client.*;
import com.bramosystems.oss.player.core.client.MediaInfo.MediaInfoKey;
import com.bramosystems.oss.player.core.client.impl.*;
import com.bramosystems.oss.player.core.client.ui.Logger;
import com.bramosystems.oss.player.core.event.client.*;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.FlowPanel;

/**
 *
 * @author Sikiru Braheem
 */
public class DivXPlayer extends AbstractMediaPlayer {

    private DivXStateManager manager;
    private DivXPlayerImpl impl;
    private PlayerWidget playerWidget;
    private boolean resizeToVideoSize, isEmbedded;
    private String playerId, _height, _width;
    private Logger logger;
    private LoopManager loopManager;
    private double currentPosition;
    private DisplayMode displayMode;

    private DivXPlayer() throws PluginNotFoundException, PluginVersionException {
        PluginVersion req = Plugin.DivXPlayer.getVersion();
        PluginVersion v = PlayerUtil.getDivXPlayerPluginVersion();
        if (v.compareTo(req) < 0) {
            throw new PluginVersionException(Plugin.DivXPlayer, req.toString(), v.toString());
        }

        playerId = DOM.createUniqueId().replace("-", "");
        loopManager = new LoopManager(false, new LoopManager.LoopCallback() {

            public void onLoopFinished() {
                firePlayStateEvent(PlayStateEvent.State.Finished, 0);
            }

            public void loopForever(boolean loop) {
                impl.setLoop(loop);
            }

            public void playNextLoop() {
                impl.playMedia();
            }
        });
        manager = new DivXStateManager(playerId, new DivXStateManager.StateCallback() {

            public void onStatusChanged(int statusId) {
                switch (statusId) {
                    case 1: // OPEN_DONE - media info available
                        fireDebug("Media Info available");
                        fireMediaInfoAvailable(manager.getFilledMediaInfo(impl.getMediaDuration(),
                                impl.getVideoWidth(), impl.getVideoHeight()));
                        break;
                    case 2: // VIDEO_END, notify loop manager...
                        fireDebug("Playback ended");
                        loopManager.notifyPlayFinished();
                        break;
                    case 10: // STATUS_PLAYING
                        fireDebug("Playback started");
                        firePlayStateEvent(PlayStateEvent.State.Started, 0);
                        break;
                    case 11: // STATUS_PAUSED
                        fireDebug("Playback paused");
                        firePlayStateEvent(PlayStateEvent.State.Paused, 0);
                        break;
                    case 14: // STATUS_STOPPED
                        fireDebug("Playback stopped");
                        firePlayStateEvent(PlayStateEvent.State.Stopped, 0);
                        break;
                    case 15: // BUFFERING_START
                        fireDebug("Buffering started");
                        firePlayerStateEvent(PlayerStateEvent.State.BufferingStarted);
                        break;
                    case 16: // BUFFERING_STOP
                        fireDebug("Buffering stopped");
                        firePlayerStateEvent(PlayerStateEvent.State.BufferingFinished);
                        break;
                    case 17: // DOWNLOAD_START
                        fireDebug("Download started");
                        fireLoadingProgress(0);
                        break;
                    case 19: // DOWNLOAD_DONE
                        fireDebug("Download finished");
                        fireLoadingProgress(1.0);
                        break;
                    case 0: // INIT_DONE
                    case 3: // SHUT_DONE
                    case 4: // EMBEDDED_START
                    case 5: // EMBEDDED_END
                    case 6: // WINDOWED_START
                    case 7: // WINDOWED_END
                    case 8: // FULLSCREEN_START
                    case 9: // FULLSCREEN_END
                    case 12: // STATUS_FF
                    case 13: // STATUS_RW
                    case 18: // DOWNLOAD_FAILED
                    default: // TODO: please remove before release...
                        fireDebug("DEV: Status Changed : " + statusId);
                }
            }

            public void onLoadingChanged(double current, double total) {
                fireDebug("loading : curent = " + current + ", total = " + total);
                fireLoadingProgress(current / total);
            }

            public void onPositionChanged(double time) {
                currentPosition = time * 1000;
            }
        });
        displayMode = DisplayMode.MINI;
    }

    public DivXPlayer(String mediaURL, boolean autoplay, String height, String width)
            throws LoadException, PluginNotFoundException, PluginVersionException {
        this();

        _height = height;
        _width = width;

        isEmbedded = (height == null) || (width == null);
        if (isEmbedded) {
            _height = "0px";
            _width = "0px";
        }

        playerWidget = new PlayerWidget(Plugin.DivXPlayer, playerId, mediaURL,
                autoplay, new BeforeUnloadCallback() {

            public void onBeforeUnload() {
                manager.clearCallbacks(playerId);
            }
        });
        playerWidget.addParam("statusCallback", "bstplayer.handlers.divx." + playerId + ".stateChanged");
        playerWidget.addParam("downloadCallback", "bstplayer.handlers.divx." + playerId + ".downloadState");
        playerWidget.addParam("timeCallback", "bstplayer.handlers.divx." + playerId + ".timeState");

        FlowPanel panel = new FlowPanel();
        panel.add(playerWidget);

        if (!isEmbedded) {
            logger = new Logger();
            logger.setVisible(false);
            panel.add(logger);

            addDebugHandler(new DebugHandler() {

                public void onDebug(DebugEvent event) {
                    logger.log(event.getMessage(), false);
                }
            });
            addMediaInfoHandler(new MediaInfoHandler() {

                public void onMediaInfoAvailable(MediaInfoEvent event) {
                    logger.log(event.getMediaInfo().asHTMLString(), true);
                    MediaInfo info = event.getMediaInfo();
                    if (info.getAvailableItems().contains(MediaInfoKey.VideoHeight)
                            || info.getAvailableItems().contains(MediaInfoKey.VideoWidth)) {
                        checkVideoSize(Integer.parseInt(info.getItem(MediaInfoKey.VideoHeight)),
                                Integer.parseInt(info.getItem(MediaInfoKey.VideoWidth)));
                    }
                }
            });
        }

        initWidget(panel);
    }

    public DivXPlayer(String mediaURL, boolean autoplay) throws
            LoadException, PluginNotFoundException, PluginVersionException {
        this(mediaURL, autoplay, "90px", "100%");
    }

    private void checkAvailable() {
        if (!isPlayerOnPage(playerId)) {
            String message = "Player not available, create an instance";
            fireDebug(message);
            throw new IllegalStateException(message);
        }
    }

    private void checkVideoSize(int vidHeight, int vidWidth) {
        String _h = _height, _w = _width;

        if (resizeToVideoSize) {
            if ((vidHeight > 0) && (vidWidth > 0)) {
                fireDebug("Resizing Player : " + vidWidth + " x " + vidHeight);
                _h = vidHeight + "px";
                _w = vidWidth + "px";
            }
        }

        playerWidget.setSize(_w, _h);
//        impl.setSize(playerWidget.getOffsetWidth(), playerWidget.getOffsetHeight());
        setSize(_w, _h);

        if (!_height.equals(_h) && !_width.equals(_w)) {
            firePlayerStateEvent(PlayerStateEvent.State.DimensionChangedOnVideo);
        }
    }

    /**
     * Overridden to register player for plugin DOM events
     */
    @Override
    protected final void onLoad() {
        playerWidget.setSize(_width, _height);
        impl = DivXPlayerImpl.getPlayer(playerId);
        fireDebug("DivX Web Player plugin");
        fireDebug("Version : " + impl.getPluginVersion());
        setWidth(_width);
        firePlayerStateEvent(PlayerStateEvent.State.Ready);
    }

    public void loadMedia(String mediaURL) throws LoadException {
        checkAvailable();
        impl.loadMedia(mediaURL);
    }

    public void playMedia() throws PlayException {
        checkAvailable();
        impl.playMedia();
    }

    public void stopMedia() {
        checkAvailable();
        impl.stopMedia();
    }

    public void pauseMedia() {
        checkAvailable();
        impl.pauseMedia();
    }

    /**
     * @deprecated As of version 1.1, remove player from panel instead
     */
    @Override
    public void close() {
//        stateManager.close(playerId);
//        impl.close();
    }

    public long getMediaDuration() {
        checkAvailable();
        return (long) impl.getMediaDuration();
    }

    public double getPlayPosition() {
        checkAvailable();
        return currentPosition;
    }

    // TODO: check up
    public void setPlayPosition(double position) {
        checkAvailable();
        double _pos = Math.abs((position - currentPosition) * 100 / currentPosition);
        fireDebug("play position : " + position + ", _pos : " + _pos);
        impl.seek(SeekMethod.DOWN.name(), _pos);
//        impl.playMedia();
    }

    // TODO: check up
    public double getVolume() {
        checkAvailable();
//        return 0; //impl.getVolume() / (double) 100;
        return impl.getVolume();// / (double) 100;
    }

    public void setVolume(double volume) {
        checkAvailable();
        volume *= 100;
        impl.setVolume((int) volume);
        fireDebug("Volume set to " + ((int) volume) + "%");
    }

    @Override
    public void showLogger(boolean enable) {
        if (!isEmbedded) {
            logger.setVisible(enable);
        }
    }

    /**
     * Displays or hides the player controls.
     *
     * <p>If this player is not available on the panel, this method
     * call is added to the command-queue for later execution.
     */
    @Override
    public void setControllerVisible(boolean show) {
        if(show && displayMode.equals(DisplayMode.NULL))
            setDisplayMode(DisplayMode.MINI);
        else
            
        setDisplayMode(show ? displayMode : DisplayMode.NULL);
    }

    /**
     * Checks whether the player controls are visible.
     */
    @Override
    public boolean isControllerVisible() {
        checkAvailable();
        return !displayMode.equals(DisplayMode.NULL);
    }

    /**
     * Returns the number of times this player repeats playback before stopping.
     */
    @Override
    public int getLoopCount() {
        checkAvailable();
        return loopManager.getLoopCount();
    }

    /**
     * Sets the number of times the current media file should repeat playback before stopping.
     *
     * <p>If this player is not available on the panel, this method
     * call is added to the command-queue for later execution.
     */
    @Override
    public void setLoopCount(final int loop) {
        if (isPlayerOnPage(playerId)) {
            loopManager.setLoopCount(loop);
        } else {
            addToPlayerReadyCommandQueue("loopcount", new Command() {

                public void execute() {
                    loopManager.setLoopCount(loop);
                }
            });
        }
    }

    @Override
    public int getVideoHeight() {
        checkAvailable();
        return impl.getVideoHeight();
    }

    @Override
    public int getVideoWidth() {
        checkAvailable();
        return impl.getVideoWidth();
    }

    @Override
    public void setResizeToVideoSize(boolean resize) {
        resizeToVideoSize = resize;
    }

    @Override
    public boolean isResizeToVideoSize() {
        return resizeToVideoSize;
    }

    @Override
    public <T extends ConfigValue> void setConfigParameter(ConfigParameter param, T value) {
        super.setConfigParameter(param, value);
    }

    /**
     * Specifies whether the player should display the DivX advertisement banner
     * at the end of playback.
     *
     * <p>If this player is not available on the panel, this method
     * call is added to the command-queue for later execution.
     *
     * @param enable {@code true} to enable, {@code false} otherwise
     */
    public void setBannerEnabled(final boolean enable) {
        if (isPlayerOnPage(playerId)) {
            impl.setBannerEnabled(enable);
        } else {
            addToPlayerReadyCommandQueue("banner", new Command() {

                public void execute() {
                    impl.setBannerEnabled(enable);
                }
            });
        }
    }

    /**
     * Specify whether the player should display a contextual (right-click) menu
     * when the user presses the right mouse button or the menu buttons on the skin.
     *
     * <p>If this player is not available on the panel, this method
     * call is added to the command-queue for later execution.
     *
     * @param allow {@code true} to allow, {@code false} otherwise
     */
    public void setAllowContextMenu(final boolean allow) {
        if (isPlayerOnPage(playerId)) {
            impl.setAllowContextMenu(allow);
        } else {
            addToPlayerReadyCommandQueue("context", new Command() {

                public void execute() {
                    impl.setAllowContextMenu(allow);
                }
            });
        }
    }

    /**
     * Specify how the player should buffer downloaded data before attempting
     * to start playback.
     *
     * <p>If this player is not available on the panel, this method
     * call is added to the command-queue for later execution.
     *
     * @param mode the mode
     */
    public void setBufferingMode(final BufferingMode mode) {
        if (isPlayerOnPage(playerId)) {
            impl.setBufferingMode(mode.name().toLowerCase());
        } else {
            addToPlayerReadyCommandQueue("buffering", new Command() {

                public void execute() {
                    impl.setBufferingMode(mode.name().toLowerCase());
                }
            });
        }
    }

    /**
     * Specifies which skin mode the player should use to display playback controls.
     *
     * <p>If this player is not available on the panel, this method
     * call is added to the command-queue for later execution.
     *
     * @param mode the display mode
     */
    public void setDisplayMode(final DisplayMode mode) {
        if (isPlayerOnPage(playerId)) {
            impl.setMode(mode.name().toLowerCase());
            displayMode = mode;
        } else {
            playerWidget.addParam("mode", mode.name().toLowerCase());
            displayMode = mode;
            /*
            addToPlayerReadyCommandQueue("displayMode", new Command() {

                public void execute() {
                    impl.setMode(mode.name().toLowerCase());
                    displayMode = mode;
                }
            });
            */
        }
    }

    /**
     * Specifies an image, text and text size to use as a preview for the video. 
     * 
     * <p>The image file which should be in PNG, JPEG or GIF format is used as a preview
     * image and is displayed in place of the DivX "X" logo.  The <code>message</code> is
     * displayed on top of the image at the size specified by <code>messageFontSize</code>.
     *
     * <p>Note: <code>autoplay</code> should be set to <code>false</code> for this method to
     * take effect.
     *
     * <p>If this player is not available on the panel, this method call is added to the
     * command-queue for later execution.
     *
     * @param imageURL the URL of the preview image
     * @param message the text displayed on top of the image
     * @param messageFontSize the font size of the displayed text
     */
    public void setPreview(final String imageURL, final String message, final int messageFontSize) {
        if (isPlayerOnPage(playerId)) {
            impl.setPreviewImage(imageURL);
            impl.setPreviewMessage(message);
            impl.setPreviewMessageFontSize(messageFontSize);
        } else {
            addToPlayerReadyCommandQueue("preview", new Command() {

                public void execute() {
                    impl.setPreviewImage(imageURL);
                    impl.setPreviewMessage(message);
                    impl.setPreviewMessageFontSize(messageFontSize);
                }
            });
        }
    }

    public static enum SeekMethod {

        DOWN, UP, DRAG
    }

    /**
     * An enum of buffering modes.  The mode is used to specify how the DivX Web Player should
     * buffer downloaded data before attempting to start playback.
     */
    public static enum BufferingMode {

        /**
         * The player does only very minimal buffering and starts playing as soon as data is available.
         * This mode does not guarantee a very good user experience unless on a very fast internet connection.
         */
        NULL,
        /**
         * The player analyses the download speed and tries to buffer just enough so
         * that uninterrupted progressive playback can happen at the end of the buffer period.
         */
        AUTO,
        /**
         * The player will always buffer the full video file before starting playback.
         */
        FULL
    }

    /**
     * An enum of display modes.  The display mode is used to specify which skin mode
     * the plugin should use to display playback controls
     */
    public static enum DisplayMode {

        /**
         * The player shows absolutely no controls.
         */
        NULL,
        /**
         * The player only shows a small floating controls bar in the bottom left corner of
         * the allocated video area.
         */
        ZERO,
        /**
         * The player shows a more elaborate control bar at the bottom of the video area.
         */
        MINI,
        /**
         * The player shows a complete control bar at the bottom of the video area
         */
        LARGE,
        /**
         * The player displays the complete control bar at the bottom of the video area and
         * an additional control bar at the top of the video area
         */
        FULL
    }
}