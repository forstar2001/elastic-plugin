# elastic-plugins
 Prerequisite : Java 8 and Later version, Elastic Search 6.4.2 

### Start ElasticSearch
1) Download elasticsearch from [here](https://www.elastic.co/downloads/elasticsearch)   
2) Extract downloaded elasticsearch     
3) cd elasticsearch-6.4.2       
4) $ bin/elasticsearch 

### Working Plugin 
1) Please navigate to browser and call RestAPI "http://localhost:9200/_api/settings"
2) Browser will be displayed settings as json.
Settings index must be already created and existed settings fields.

### How the plugin code works
 Plugin Main Class - DocApiPlugin
 Plugin RestApi Class - ApiSettings
 RestApi Class 'ApiSettings' - prepareRequest Function
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
 			}
 		}