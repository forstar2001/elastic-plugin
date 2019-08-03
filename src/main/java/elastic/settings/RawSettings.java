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


import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class RawSettings {

  private final Map<String, ?> raw;
  private final String rawYaml;

  public RawSettings(String rawYaml) {
    this.rawYaml = replaceEnvVars(rawYaml);
    this.raw = SettingsUtils.yaml2Map(rawYaml);
  }

  public RawSettings(Map<String, ?> raw) {
    this.rawYaml = replaceEnvVars(SettingsUtils.map2yaml(raw));
    this.raw = SettingsUtils.yaml2Map(rawYaml);
  }

  private static String replaceEnvVars(String rawYaml) {
    String out = rawYaml;
    for (String key : System.getenv().keySet()) {
      out = out.replaceAll(Pattern.quote("${" + key + "}"), System.getenv(key));
    }
    return out;
  }

  static RawSettings fromMap(Map<String, ?> r) {
    String syntheticYaml = SettingsUtils.map2yaml(r);
    return new RawSettings(syntheticYaml);
  }


  public Map<String, ?> asMap() {
    return raw;
  }


  public String yaml() {
    return rawYaml;
  }

  @Override
  public String toString() {
    return rawYaml;
  }
}
