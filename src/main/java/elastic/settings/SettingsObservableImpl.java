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

import org.apache.logging.log4j.LogManager;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Singleton;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.env.Environment;

import java.io.File;
import java.nio.file.Path;

import elastic.settings.SettingsUtils;

/**
 * Created by sscarduzio on 25/06/2017.
 */

@Singleton
public class SettingsObservableImpl extends SettingsObservable {

  private final NodeClient client;
  private final Environment environment;

  @Inject
  public SettingsObservableImpl(NodeClient client, Settings s, Environment env) {
    this.environment = env;
    this.client = client;
    String settinngStr = SettingsUtils.slurpFile(env.configFile().toAbsolutePath() + File.separator + "custom.yml");
    current = new RawSettings(settinngStr);
  }

  @Override
  protected Path getConfigPath() {
    return environment.configFile().toAbsolutePath();
  }


  public RawSettings getFromIndex() {
    GetResponse resp;
    try {
      resp = client.prepareGet("settings", "_doc", "1").get();
    } catch (ResourceNotFoundException rnfe) {
      throw new ElasticsearchException(SETTINGS_NOT_FOUND_MESSAGE, rnfe);
    } catch (Throwable t) {
      throw new ElasticsearchException(t.getMessage(), t);
    }
    if (resp == null || !resp.isExists()) {
      throw new ElasticsearchException(SETTINGS_NOT_FOUND_MESSAGE, new ElasticsearchException("null response from index query"));
    }
    String yamlString = (String) resp.getSource().get("settings");
    if(yamlString == null)
      yamlString = "";
    return new RawSettings(yamlString);
  }

  @Override
  public boolean isClusterReady() {
    try {
      ClusterHealthStatus status = client.admin().cluster().prepareHealth().get().getStatus();
      return !status.equals(ClusterHealthStatus.RED);
    } catch (Throwable e) {
      return false;
    }
  }

}
