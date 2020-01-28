# Resussun

## How to start the Resussun application

### Use a local build
1. Run `mvn clean install` to build your application
1. Run Elasticsearch: `docker run -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" docker.elastic.co/elasticsearch/elasticsearch:7.5.2`
1. Start application with `./target/appassembler/bin/resussun server config.yml`
1. To check that your application is running enter url `http://localhost:8080`

### Use docker
1. run the docker-compose file: `docker-compose -f docker/docker-compose.yml up -d`  
1. To check that your application is running enter url `http://localhost:8080`

### Create an index
1. Create an index: `curl -X POST --data "timbuctooUrl={timbuctooUrl}&dataSetId={dataSetId}" http://{resussun_host}:{admin_port}/tasks/createIndex`


##Health Check
---

To see your applications health enter url `http://localhost:8081/healthcheck`
