#!/bin/bash

mvn clean package

./target/appassembler/bin/resussun server config.yml 

