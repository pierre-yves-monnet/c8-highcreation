# c8-highcreation

The goal od this project is to create as soon as possible a lot of traffic on a cluster, to study the back pressure.

It will check if there is no process instances duplicated.

# Check unique process instance
During the creation a unique transactionID is generate and placed in a process variable.
Then, a worker check that all TID are unique. If, during the creation, a process instance is duplicated, then the worker will detect it

# Build an image
Different command:

Build the image:

````yaml
mvn spring-boot:build-image
````

# push to my docker package

```
docker tag c8-highcreation:1.0 docker.pkg.github.com/pierre-yves-monnet/c8-highcreation/c8-highcreation:1.0
docker login docker.pkg.github.com -u pierre-yves-monnet -p <token>
docker push docker.pkg.github.com/pierre-yves-monnet/c8-highcreation/c8-highcreation:1.0
```

