package elastic;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestRequest.Method.POST;

import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.function.Supplier;

import elastic.settings.RawSettings;
import elastic.settings.SettingsUtils;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.support.ActionFilterChain;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.IndexScopedSettings;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsFilter;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.env.Environment;
import org.elasticsearch.env.NodeEnvironment;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.plugins.ActionPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.action.support.ActionFilter;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.watcher.ResourceWatcherService;
import org.pmw.tinylog.Logger;

import elastic.filter.SearchActionFilter;
import elastic.settings.SettingsObservableImpl;
import elastic.settings.SettingsUtils;


/**
 * Custom Api plugin:
 * 
 * Notes:
 * env.configFile() is the Elasticsearch configuration directory
 * env.configFile().resolve("custom.yml") resolves a custom settings file
 * 
 * All Api parameters must be consumed via RestRequest#param(parameterName) or an exception is thrown;
 * When registering a "path paramete" - include the base path, and then the parameter path. e.g:
 *   controller.registerHandler(GET, "/_api/img", this);
 *   controller.registerHandler(GET, "/_api/img/{id}", this);
 * 
 * IMPORTANT:
 *   End points did not register properly when BaseRestHandlers were "non static" inner classes of the outer Action Plugin
 *   The reason is that they were expected to declare a getSettings() method public List<Setting<?>> getSettings() { return Collections.emptyList() };
 *   
 *   As soon as they were not "instances" of ActionPlugin, end points register fine.
 */ 
public class DocApiPlugin extends Plugin implements ActionPlugin {

    private Settings settings;
    private SearchActionFilter saf;
    private SettingsObservableImpl settingsObservable;
    private Environment environment;
    private RawSettings basicSettings;
	private String settingStr;

    @Inject
    public DocApiPlugin(Settings s, Path p) {
        this.settings = s;
        this.environment = new Environment(s, p);
        this.settingStr = new SettingsUtils().slurpFile(p + File.separator + "custom.yml");
        this.basicSettings = new RawSettings(settingStr);
    }

    @Override
    public Collection<Object> createComponents(Client client, ClusterService clusterService, ThreadPool threadPool, ResourceWatcherService resourceWatcherService,
                                               ScriptService scriptService, NamedXContentRegistry xContentRegistry, Environment environment, NodeEnvironment nodeEnvironment,
                                               NamedWriteableRegistry namedWriteableRegistry) {

        final List<Object> components = new ArrayList<>(3);

        // Wrap all ROR logic into privileged action
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            this.environment = environment;
            settingsObservable = new SettingsObservableImpl((NodeClient) client, settings, environment);
            this.saf = new SearchActionFilter(settings, clusterService, (NodeClient) client, threadPool, settingsObservable, environment);
            components.add(settingsObservable);
            return null;
        });

        return components;
    }

//    @Override
//    public List<ActionFilter> getActionFilters() {
//        return Collections.singletonList(saf);
//    }

	@Override
	public List<RestHandler> getRestHandlers(
			final Settings settings, 
			final RestController restController,
			final ClusterSettings clusterSettings, 
			final IndexScopedSettings indexScopedSettings,
			final SettingsFilter settingsFilter, 
			final IndexNameExpressionResolver indexNameExpressionResolver,
			final Supplier<DiscoveryNodes> nodesInCluster) {

		return Arrays.asList(
				new ApiAudit(settings, restController), new ApiSettings(settings, restController, this.settingsObservable));
	}


	@Override
	public void close() {
	     AccessController.doPrivileged(new PrivilegedAction<Void>() {
	         public Void run() {
	             return null; // nothing to return
	         }
	     });		
	}

	
	/**
	 * All Settings have to be registered so Elasticsearch can validate them. 
	 * The ActionPlugin has a getSettings() method used to define the settings required by the plugin
	 */
	@Override
	public List<Setting<?>> getSettings() {
        return Arrays.asList();
	}

	
	/** Base Doc API Handler */
	public static class DocApiAction extends BaseRestHandler {
	    private final String HELP;
	
	    @Inject
	    public DocApiAction(Settings settings, RestController controller) {
	        super(settings);
	        controller.registerHandler(GET, "/_api/help", this);
			logger.info("_api/help");

	        StringBuilder sb = new StringBuilder();
	        sb.append("=^.^=\n");
	        HELP = sb.toString();
	    }
	
	    @Override
	    public String getName() {
	        return "api-help-action";
	    }

	    @Override
	    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {
	        return channel -> channel.sendResponse(new BytesRestResponse(RestStatus.OK, HELP));
	    }	
	}

	public static class ApiAudit extends BaseRestHandler {
	    @Inject
	    public ApiAudit(Settings settings, RestController controller) {
	        super(settings);
	        controller.registerHandler(POST, "/_api/audit", this);
	    }
	
	    @Override
	    public String getName() {
	        return "api-audit";
	    }

	    @Override
	    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {
	    	try {
				Tuple<XContentType, BytesReference> c = request.contentOrSourceParam();
				final BytesReference data = c.v2();
				final String json = data.utf8ToString();
				logger.info("audit:", json);
				SocketAddress s = request.getRemoteAddress();
				Map<String, List<String>> headers = request.getHeaders();
		        List<String> token = headers.get("Authorization");    	
				//ThreadContext
			    AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
				    Logger.info("audit: [{}] {} [{}]", s.toString(), json, headers);
					return null;
			    });				
		        return channel -> channel.sendResponse(new BytesRestResponse(RestStatus.OK, "{\"acknowledged\": true}"));
	    	}
	    	catch (Exception e) {
	    		return channel -> channel.sendResponse(new BytesRestResponse(RestStatus.SERVICE_UNAVAILABLE, "{\"acknowledged\": false, \"error\": \"" + e.getMessage() + "\"}"));
	    	}
	    }

        private boolean hasRemoteClusters(ClusterService clusterService) {
            try {
                return !clusterService.getSettings().getAsGroups().get("cluster").getGroups("remote").isEmpty();
            } catch (Exception ex) {
                if(logger.isDebugEnabled()) {
                    logger.warn("could not check if had remote ES clusters", ex);
                } else {
                    logger.warn("could not check if had remote ES clusters: " + ex.getMessage());
                }
                return false;
            }
        }
	}

	public static class ApiSettings extends BaseRestHandler {
		private SettingsObservableImpl settingsObservable;
		@Inject
		public ApiSettings(Settings settings, RestController controller, SettingsObservableImpl settingsObservable) {
			super(settings);
			controller.registerHandler(GET, "/_api/settings", this);
			this.settingsObservable = settingsObservable;
		}

		@Override
		public String getName() {
			return "api-settings";
		}

		@Override
		public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {
			SocketAddress s = request.getRemoteAddress();
			Map<String, List<String>> headers = request.getHeaders();
			List<String> token = headers.get("Authorization");
            String settingStr = settingsObservable.getFromIndex().yaml();
			try {
				Tuple<XContentType, BytesReference> c = request.contentOrSourceParam();
				final BytesReference data = c.v2();
				final String json = data.utf8ToString();
				logger.info("audit:", json);

				//ThreadContext
				AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
					Logger.info("audit: [{}] {} [{}]", s.toString(), json, headers);
					return null;
				});
				return channel -> channel.sendResponse(new BytesRestResponse(RestStatus.OK, "{\"settings\": "+settingStr+"}"));
			}
			catch (Exception e) {
				//ThreadContext
				AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
					Logger.info("audit: [{}] {} [{}]", s.toString(), "", headers);
					return null;
				});
				return channel -> channel.sendResponse(new BytesRestResponse(RestStatus.OK, "{\"settings\": "+settingStr+"}"));
//				return channel -> channel.sendResponse(new BytesRestResponse(RestStatus.SERVICE_UNAVAILABLE, "{\"acknowledged\": false, \"error\": \"" + e.getMessage() + "\"}"));
			}
		}

		private boolean hasRemoteClusters(ClusterService clusterService) {
			try {
				return !clusterService.getSettings().getAsGroups().get("cluster").getGroups("remote").isEmpty();
			} catch (Exception ex) {
				if(logger.isDebugEnabled()) {
					logger.warn("could not check if had remote ES clusters", ex);
				} else {
					logger.warn("could not check if had remote ES clusters: " + ex.getMessage());
				}
				return false;
			}
		}
	}
}

