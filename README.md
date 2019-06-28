# quarkus-myfaces-native

A WIP to bring native support for quarkus-myfaces extension

 

## Steps to build

1 - Install myfaces:

```
git clone https://github.com/rmpestano/myfaces.git && cd myfaces && mvn clean install -DskipTests 
``` 

2 - Install quarkus myfaces `mvn clean install`

3 - run the quarkus-myfaces-showcase  native binary:

```
mvn clean package -Pnative -Dnative-image.docker-build=true && ./target/quarkus-myfaces-showcase-1.0-SNAPSHOT-runner

```

4 - Access http://localhost:8080/index.xhtml

 
 


> Logs can be accessed in `/tmp/debug.log`

 

 
