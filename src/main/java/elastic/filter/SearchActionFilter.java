package elastic.filter;

import java.net.SocketAddress;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.support.ActionFilter;
import org.elasticsearch.action.support.ActionFilterChain;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.env.Environment;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.rest.RestRequest;

import elastic.common.Common;
import elastic.utils.RestRequestUtil;
import org.elasticsearch.threadpool.ThreadPool;
import org.pmw.tinylog.Logger;
import elastic.settings.SettingsObservableImpl;

public class SearchActionFilter implements ActionFilter {

    private final ThreadPool threadPool;
    private final ClusterService clusterService;

    public void log(int level, Object o) {
        if (level > Common.logLevel) {
            Common.log0(o);
        }
    }

    public SearchActionFilter(Settings settings,
       ClusterService clusterService,
       NodeClient client,
       ThreadPool threadPool,
       SettingsObservableImpl settingsObservable,
       Environment env
    ){
        log(3, "---> SearchActionFilter init");
        this.clusterService = clusterService;
        this.threadPool = threadPool;
//        settingsObservable.forceRefresh();
//        settingsObservable.pollForIndex();
    }

    @Override
    public int order() {
        // TODO Auto-generated method stub
        return -1;
    }

    // apply... internal:gateway/local/meta_state
    // apply... cluster:monitor/nodes/stats
    // apply... indices:monitor/stats

    Pattern systemAction = Pattern.compile("(inter.+?)|((cluster:monitor/nodes/stats)|(indices:monitor/stats))");

    @Override
    public <Request extends ActionRequest, Response extends ActionResponse> void apply(Task task, String action, Request request,
                                                                                       ActionListener<Response> listener, ActionFilterChain<Request, Response> chain) {
        // TODO Auto-generated method stub
        log(1, "apply... " + action);

        boolean isSystemAction = checkSystemAction(task, action);
        if (!isSystemAction) {
            SearchActionFilterUtil.checkRemoteAddress(request, listener);
            String remoteAddress = request.remoteAddress() + "";
            log(1, "remoteAddress:" + remoteAddress);
            try {
                RestRequest restRequest = RestRequestUtil.getRestRequest(listener);
                Tuple<XContentType, BytesReference> c = restRequest.contentOrSourceParam();
                final BytesReference data = c.v2();
                final String content = data.utf8ToString();
                log(1, "content:" + content);

                if (action.endsWith("/search")){
                    AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                        Logger.info("search---> [remoteAddress:{}]-[action:{}]-[content:{}]", remoteAddress, action, content);
                        return null;
                    });
                }
            }catch (Exception e) {
                if (action.endsWith("/search")){
                    AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                        Logger.info("search---> [remoteAddress:{}]-[action:{}]-[content:{}]", remoteAddress, action, "");
                        return null;
                    });
                }
                chain.proceed(task, action, request, listener);
                return;
            }
        }
        chain.proceed(task, action, request, listener);
        return;

    }

    private <Request extends ActionRequest> boolean isJavaClient(Task task, Request request) {
        // transport
        boolean isJavaClient = false;
        //isJavaClient = request.remoteAddress() != null ? true : false;
        isJavaClient = !task.getType().equals("transport");
        return isJavaClient;
    }

    private boolean checkSystemAction(Task task, String action) {
        boolean isSystemAction = systemAction.matcher(action).find();

        // boolean isSystemAction = action.endsWith("stat");
        return isSystemAction;
    }

}