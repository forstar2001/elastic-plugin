package elastic.filter;

import java.net.InetSocketAddress;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.rest.RestRequest;

import elastic.utils.RestRequestUtil;

public class SearchActionFilterUtil {

    public static boolean using = true;

    static <Request extends ActionRequest, Response extends ActionResponse> void checkRemoteAddress(Request request, ActionListener<Response> listener) {

        if (!using) {
            return;
        }

        listener.getClass();

        RestRequest rq = RestRequestUtil.getRestRequest(listener);

        if (rq != null) {
            rq.getRemoteAddress();
            InetSocketAddress rq1 = (InetSocketAddress) rq.getRemoteAddress();
            request.remoteAddress(new TransportAddress(rq1));
        }

    }

    public static void init() {
        // RestRequestUtil.init();
    }

}