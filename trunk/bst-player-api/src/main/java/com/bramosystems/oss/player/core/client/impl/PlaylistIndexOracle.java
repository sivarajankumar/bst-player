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

import com.google.gwt.user.client.Random;
import java.util.HashSet;

public class PlaylistIndexOracle {

    private int _currentIndex, _indexSize;
    private boolean _randomMode;
    private HashSet<Integer> _usedRandomIndices;

    public PlaylistIndexOracle() {
        this(0);
    }

    public PlaylistIndexOracle(int indexSize) {
        _indexSize = indexSize;
        _usedRandomIndices = new HashSet<Integer>();
    }

    public void setRandomMode(boolean _randomMode) {
        this._randomMode = _randomMode;
    }

    public boolean isRandomMode() {
        return _randomMode;
    }

    public void incrementIndexSize() {
        _indexSize++;
    }

    public void reset() {
        _usedRandomIndices.clear();
        _indexSize = 0;
        _currentIndex = 0;
    }

    public int getCurrentIndex() {
        return _currentIndex;
    }

    public void removeFromCache(int index) {
        _usedRandomIndices.remove(Integer.valueOf(index));
        _indexSize--;
    }

    public int suggestIndex(boolean up, boolean canRepeat) {
        _currentIndex = suggestIndexImpl(up);
        if (_randomMode) {
            int _count = 0;
            while (_usedRandomIndices.contains(_currentIndex)) {
                _currentIndex = suggestIndexImpl(up);
                _count++;
                if (_count == _indexSize) {
                    _currentIndex = -1;
                    break;
                }
            }
        } else {
            if (_currentIndex == _indexSize) {
                _currentIndex = -1;
            }
        }

        if (_currentIndex < 0 && canRepeat) {  // prepare for another iteration ...
            _usedRandomIndices.clear();
            _currentIndex = up ? 0 : _indexSize;
            _currentIndex = suggestIndex(up, canRepeat);
        }

        if (_currentIndex >= 0) { // keep the used index ...
            _usedRandomIndices.add(_currentIndex);
        }
        return _currentIndex;
    }

    private int suggestIndexImpl(boolean up) {
        return _randomMode ? Random.nextInt(_indexSize)
                : (up ? _currentIndex++ : _currentIndex--);
    }
}
