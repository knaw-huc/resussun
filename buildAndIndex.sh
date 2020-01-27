#!/bin/bash

mvn clean package

./target/appassembler/bin/resussun createIndex --tim https://repository.huygens.knaw.nl --dataSet u519bd710306620fa7c56d541ae7b9f5b7f57a706__clusius
