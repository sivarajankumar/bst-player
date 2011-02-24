/*
 * Copyright 2009 Sikirulai Braheem
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.bramosystems.oss.player.core.client.impl;

import com.bramosystems.oss.player.core.client.MediaInfo;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Timer;
import java.util.HashMap;
import java.util.Iterator;

/**
 * This class handles the events generated by the embedded Windows Media Player.
 * It maps the WMP states into the corresponding event in the API.
 *
 * @author Sikirulai Braheem
 */
public class WMPStateManager {

    protected HashMap<String, EventProcessor> cache;

    @SuppressWarnings({"OverridableMethodCallInConstructor", "LeakingThisInConstructor"})
    WMPStateManager() {
        cache = new HashMap<String, EventProcessor>();
        initGlobalEventListeners(this);
    }

    public EventProcessor init(String playerId, WMPEventCallback handler, WMPImplCallback impl) {
        EventProcessor sm = new EventProcessor(handler, impl);
        cache.put(playerId, sm);
        return sm;
    }

    /**
     * Provided for deferred binding enhancements. Resize fix required for
     * non-IE browsers only
     *
     * @return quick
     */
    public boolean shouldRunResizeQuickFix() {
        return true;
    }

    public final boolean isPlayerStateManaged(String playerId) {
        return cache.containsKey(playerId);
    }

    public void close(String playerId) {
        cache.remove(playerId);
    }

    public void stop(String playerId) {
        // do nothing, workaround for webkit implementation...
    }

    @SuppressWarnings("unused")
    private void firePlayStateChanged() {
        Iterator<String> keys = cache.keySet().iterator();
        while (keys.hasNext()) {
            cache.get(keys.next()).checkPlayState();
        }
    }

    @SuppressWarnings("unused")
    private void fireError() {
        Iterator<String> keys = cache.keySet().iterator();
        while (keys.hasNext()) {
            String id = keys.next();
            cache.get(id).checkError();
        }
    }

    @SuppressWarnings("unused")
    private void fireCMEvents(int type, int button, int shiftState, double fX, double fY) {
        Iterator<String> keys = cache.keySet().iterator();
        while (keys.hasNext()) {
            String id = keys.next();
            cache.get(id).doClickMouseEvents(type, button, shiftState, fX, fY);
        }
    }

    @SuppressWarnings("unused")
    private void fireBuffering(boolean buffering) {
        Iterator<String> keys = cache.keySet().iterator();
        while (keys.hasNext()) {
            cache.get(keys.next()).doBuffering(buffering);
        }
    }

    protected native void initGlobalEventListeners(WMPStateManager impl) /*-{
    $wnd.OnDSPlayStateChangeEvt = function(NewState) {
    impl.@com.bramosystems.oss.player.core.client.impl.WMPStateManager::firePlayStateChanged()();
    }
    $wnd.OnDSErrorEvt = function() {
    impl.@com.bramosystems.oss.player.core.client.impl.WMPStateManager::fireError()();
    }
    $wnd.OnDSBufferingEvt = function(Start) {
    impl.@com.bramosystems.oss.player.core.client.impl.WMPStateManager::fireBuffering(Z)(Start);
    }
    $wnd.OnDSMouseDownEvt = function(nButton,nShiftState,fX,fY) {
    impl.@com.bramosystems.oss.player.core.client.impl.WMPStateManager::fireCMEvents(IIIDD)(1,nButton,nShiftState,fX,fY);
    }
    $wnd.OnDSMouseUpEvt = function(nButton,nShiftState,fX,fY) {
    impl.@com.bramosystems.oss.player.core.client.impl.WMPStateManager::fireCMEvents(IIIDD)(2,nButton,nShiftState,fX,fY);
    }
    $wnd.OnDSMouseMoveEvt = function(nButton,nShiftState,fX,fY) {
    impl.@com.bramosystems.oss.player.core.client.impl.WMPStateManager::fireCMEvents(IIIDD)(3,nButton,nShiftState,fX,fY);
    }
    $wnd.OnDSClickEvt = function(nButton,nShiftState,fX,fY) {
    impl.@com.bramosystems.oss.player.core.client.impl.WMPStateManager::fireCMEvents(IIIDD)(10,nButton,nShiftState,fX,fY);
    }
    $wnd.OnDSDoubleClickEvt = function(nButton,nShiftState,fX,fY) {
    impl.@com.bramosystems.oss.player.core.client.impl.WMPStateManager::fireCMEvents(IIIDD)(11,nButton,nShiftState,fX,fY);
    }
    $wnd.OnDSDblClickEvt = function(nButton,nShiftState,fX,fY) {
    impl.@com.bramosystems.oss.player.core.client.impl.WMPStateManager::fireCMEvents(IIIDD)(11,nButton,nShiftState,fX,fY);
    }
    $wnd.OnDSKeyDownEvt = function(nKeyCode,nShiftState) {
    impl.@com.bramosystems.oss.player.core.client.impl.WMPStateManager::fireCMEvents(IIIDD)(20,nKeyCode,nShiftState,0,0);
    }
    $wnd.OnDSKeyUpEvt = function(nKeyCode,nShiftState) {
    impl.@com.bramosystems.oss.player.core.client.impl.WMPStateManager::fireCMEvents(IIIDD)(21,nKeyCode,nShiftState,0,0);
    }
    $wnd.OnDSKeyPressEvt = function(nKeyCode,nShiftState) {
    impl.@com.bramosystems.oss.player.core.client.impl.WMPStateManager::fireCMEvents(IIIDD)(22,nKeyCode,nShiftState,0,0);
    }
    }-*/;

    public void registerMediaStateHandlers(WinMediaPlayerImpl player) {
        // do nothing, provided for DOM event registration in IE.
    }

    public class EventProcessor {

        private boolean enabled;
        private Timer downloadProgressTimer;
        private String _mURL = "-", _oURL = "";
        private WMPEventCallback _callback;
        private WMPImplCallback _impl;

        public EventProcessor(WMPEventCallback callback, WMPImplCallback impl) {
            _callback = callback;
            _impl = impl;
            enabled = false;
            downloadProgressTimer = new Timer() {

                @Override
                public void run() {
                    _callback.onLoadingProgress(_impl.getImpl().getDownloadProgress());
                }
            };
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public void checkPlayState() {
            if (!enabled) {
                return;
            }

            int state = _impl.getImpl().getPlayState();
            if (state < 0) {
                return;
            }

            processPlayState(state);
        }

        public void onError(String message) {
            _callback.onError(message);
        }

        public void debug(String msg) {
            _callback.onInfo(msg);
        }

        public void doBuffering(boolean buffering) {
            _callback.onBuffering(buffering);

            debug("Buffering " + (buffering ? " started" : " stopped"));
            if (buffering) {
                downloadProgressTimer.scheduleRepeating(250);
            } else {
                downloadProgressTimer.cancel();
                _callback.onLoadingProgress(1.0);
                debug("Media loading complete");
            }
        }

        protected void processPlayState(int state) {
            switch (state) {
                case 1:    // stopped..
                    debug("Media playback stopped");
                    _callback.onStop();
                    break;
                case 2:    // paused..
                    debug("Media playback paused");
                    _callback.onPaused();
                    break;
                case 3:    // playing..
                    _callback.onPlay();
                    _mURL = _impl.getImpl().getCurrentMediaURL();
                    if (!_oURL.equals(_mURL)) { // new media ...
                        doMetadata();        // do metadata ...
                    }
                    _oURL = _mURL;
                    break;
                case 8:    // media ended...
                    _callback.onEnded();
                    break;
                case 9:     // preparing new item ...
                    _callback.onOpening();
                    break;
                case 10:    // player ready, ...
                    _callback.onReady();
                    break;
                case 6:    // buffering ...
                case 11:    // reconnecting to stream  ...
                   break;
            }
        }

        public void checkError() {
            if (enabled) {
                onError(_impl.getImpl().getErrorDiscription());
            }
        }

        protected void doMetadata() {
            MediaInfo info = new MediaInfo();
            String err = "";
            _impl.getImpl().fillMetadata(info, err);
            if (err.length() == 0) {
                _callback.onMediaInfo(info);
            } else {
                onError(err);
            }
        }

        public void doClickMouseEvents(int type, int button, int shiftState, double fX, double fY) {
            if (!enabled) {
                return;
            }

            boolean shift = (shiftState & 1) == 1;
            boolean alt = (shiftState & 2) == 2;
            boolean ctrl = (shiftState & 4) == 4;

            Element e = Element.as(_impl.getImpl()); //.getParentElement();
            int clientX = e.getAbsoluteLeft() + (int) fX - e.getOwnerDocument().getScrollLeft();
            int clientY = e.getAbsoluteTop() + (int) fY - e.getOwnerDocument().getScrollTop();
            int screenX = -1; //e.getAbsoluteLeft() + (int) fX; // - e.getScrollLeft();
            int screenY = -1; //e.getAbsoluteTop() + (int) fY; // - e.getScrollTop();

            Document _doc = Document.get();
            NativeEvent event = null;
            switch (type) {
                case 1:    // mouse down ..
                    event = _doc.createMouseDownEvent(button, screenX, screenY, clientX,
                            clientY, ctrl, alt, shift, false, button);
                    break;
                case 2:    // mouse up ...
                    event = _doc.createMouseUpEvent(button, screenX, screenY, clientX,
                            clientY, ctrl, alt, shift, false, button);
                    break;
                case 3:    // mouse move ...
                    event = _doc.createMouseMoveEvent(button, screenX, screenY, clientX,
                            clientY, ctrl, alt, shift, false, button);
                    break;
                case 10:    // click ...
                    event = _doc.createClickEvent(button, screenX, screenY, clientX,
                            clientY, ctrl, alt, shift, false);
                    break;
                case 11:    // double click ...
                    event = _doc.createDblClickEvent(button, screenX, screenY, clientX,
                            clientY, ctrl, alt, shift, false);
                    break;
                case 20:    // key down ...
                    event = _doc.createKeyDownEvent(ctrl, alt, shift, false, button, button);
                    break;
                case 21:    // key up ...
                    event = _doc.createKeyUpEvent(ctrl, alt, shift, false, button, button);
                    break;
                case 22:    // key press ...
                    event = _doc.createKeyPressEvent(ctrl, alt, shift, false, button, button);
                    break;
            }
            _callback.onNativeEvent(event);
        }
    }

    public static interface WMPEventCallback {
        public void onLoadingProgress(double progress);
        public void onError(String message);
        public void onInfo(String message);
        public void onBuffering(boolean started);
        public void onStop();
        public void onPlay();
        public void onPaused();
        public void onEnded();
        public void onMediaInfo(MediaInfo info);
        public void onOpening();
        public void onReady();
        public void onNativeEvent(NativeEvent event);
    }

    public static interface WMPImplCallback {
        public WinMediaPlayerImpl getImpl();
    }
}
