/*
 *    This file is part of ReadonlyREST.
 *
 *    ReadonlyREST is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    ReadonlyREST is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with ReadonlyREST.  If not, see http://www.gnu.org/licenses/
 */

package elastic.settings;

import java.nio.file.Path;
import java.util.Observable;

abstract public class SettingsObservable extends Observable {

  public static final String SETTINGS_NOT_FOUND_MESSAGE = "no settings found in index";

  protected RawSettings current;
  private boolean printedInfo = false;

  abstract protected Path getConfigPath();

  protected abstract boolean isClusterReady();

  protected abstract RawSettings getFromIndex();

//  protected abstract void writeToIndex(RawSettings rawSettings, FutureCallback f);

  public RawSettings getCurrent() {
    return current;
  }

//  public RawSettings getFromFile() {
//    return BasicSettings.fromFile(getLogger(), getConfigPath(), current.asMap()).getRaw();
//  }

//  public void refreshFromIndex() {
//    try {
//      RawSettings fromIndex = getFromIndex();
//
//      if (!fromIndex.asMap().equals(current.asMap())) {
//        updateSettings(fromIndex);
//      }
//    } catch (Throwable t) {
//      if (SETTINGS_NOT_FOUND_MESSAGE.equals(t.getMessage())) {
//        if (!printedInfo) {
//        }
//        printedInfo = true;
//      }
//      else {
//        t.printStackTrace();
//      }
//    }
//  }
//
//  public void forceRefresh() {
//    setChanged();
//    notifyObservers();
//  }

//  public void pollForIndex() {
//    new SettingsPoller(this, 1, 5, true).poll();
//  }

  private void updateSettings(RawSettings newSettings) {
    this.current = newSettings;
    setChanged();
    notifyObservers();
  }

}
