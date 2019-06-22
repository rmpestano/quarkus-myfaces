# quarkus-myfaces

Quarkus myfaces native build issue: https://groups.google.com/forum/#!topic/quarkus-dev/lLRjtXcuXKQ


## Steps to reproduce the issue

1 - Install myfaces:

```
git clone https://github.com/rmpestano/myfaces.git && cd myfaces && clean install -DskipTests 
``` 

2 - Install quarkus myfaces `mvn clean install`

3 - run the quarkus-myfaces-showcase  native binary:

```
mvn clean package -Pnative -Dnative-image.docker-build=true && ./target/quarkus-myfaces-showcase-1.0-SNAPSHOT-runner

```

4 - Access `http://localhost:8080/index.xhtml

5 - The following error should be raised:

```
No Factories configured for this Application. This happens if the faces-initialization does not work at all - make sure that you properly include all configuration settings necessary for a basic faces application and that all the necessary libs are included. Also check the logging output of your web application and your container for any exceptions! If you did that and find nothing, the mistake might be due to the fact that you use some special web-containers which do not support registering context-listeners via TLD files and a context listener is not setup in your web.xml. A typical config looks like this; <listener> <listener-class>org.apache.myfaces.webapp.StartupServletContextListener</listener-class> </listener> 
```


> Full logs can be accessed in `/tmp/debug.log`

 

 
