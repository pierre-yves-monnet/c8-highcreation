# c8-highcreation

The goal od this project is to create as soon as possible a lot of traffic on a cluster, to study the back pressure.

It will check if there is no process instances duplicated.

# Check unique process instance
During the creation a unique transactionID is generate and placed in a process variable.
Then, a worker check that all TID are unique. If, during the creation, a process instance is duplicated, then the worker will detect it

# how to run it

1. deploy the process src/main/resources/DuplicateIssue.bpmn on a C8
2. Run src/main/java/com/camunda/highcreation/HighCreationApplication.java

By default, the configuration is set to access a cluster on localhost. Port forward a cloud cluster on your machine.
Else, you can change configuration and fulfill the constant to a different server.

Note: the process have two methods to create the process instance: 
* withResult
* standard

This is a constant in the code (third parameters)

````
createProcessInstances(client, 10000, true, "DuplicateIssue", null);
````



# Build an image

Build the image with:

````yaml
mvn spring-boot:build-image
````

# push to my docker package

```
docker tag c8-highcreation:1.0 docker.pkg.github.com/pierre-yves-monnet/c8-highcreation/c8-highcreation:1.0
docker login docker.pkg.github.com -u pierre-yves-monnet -p <token>
docker push docker.pkg.github.com/pierre-yves-monnet/c8-highcreation/c8-highcreation:1.0
```

# Deploy the image on the cluster

Check k8s/HighCreation.yaml and execute the command

````
kubectl create -f HighCreation.yaml
````

ATTENTION: I didn't test it
