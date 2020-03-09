#!/bin/bash

mvn clean package

docker run -d -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" docker.elastic.co/elasticsearch/elasticsearch:7.6.1
docker run -d -p 6379:6379 redis:5.0.7

./target/appassembler/bin/resussun server config.yml 

