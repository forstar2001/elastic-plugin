set ES_HOME=e:\elasticsearch-6.4.2\elasticsearch-6.4.2
set ES_DEPLOY=file:///e:/work//java/elastic-plugins/elastic-plugins/target
echo %ES_HOME%

call %ES_HOME%\bin\elasticsearch-plugin remove doc-api-plugin
call %ES_HOME%\bin\elasticsearch-plugin install --batch %ES_DEPLOY%/elasticsearch-6.4.2.1-doc-api-plugin.zip
