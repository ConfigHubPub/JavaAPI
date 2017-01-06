# JavaAPI
ConfigHub Java Client API

[JavaDoc] (http://localhost:8080/api/docs/Java/v1.3.0/index.html?com/confighub/client/ConfigHub.html)

## Configuration Pull Example:
```java
import com.confighub.client.*;

public class PullTest
{
    public static void main(String... args)
    {
        ConfigHub configHub = new ConfigHub("ConfigHub", "Demo")
            .setContext("Production;PullTest")
            .setApplicationName("PullTest");
            .setConfighubServerAddress("demo.confighub.com");

        confighub.pull();

        // Get a few properties returned from the pull request
        int dbPort = configHub.properties.getInteger("db.port");
        String dbHost = configHub.properties.get("db.host");

        // Get structured data
        Map<String, String> map =
                configHub.properties.getMap("client.labels");

        // Get some config files
        String log4jxml = confighub.files.get("/logger/log4j2.xml");
        String tomeeXml = confighub.files.get("/server/tomee.xml");
    }
}
```

## Configuration Push Example
```java
import com.confighub.client.*;

public class PushTest
{
    public static void main(String... args)
    {
        ConfigHub configHub = new ConfigHub("ConfigHub", "Demo")
            .setApplicationName("PushTest")
            .setConfighubServerAddress("demo.confighub.com");

        // Update a single value
        configHub.pushQueue.key("count.total")
                           .setValue(32, "Production;PushTest");

        // Update multiple values for a single key
        configHub.pushQueue.key("logger.level")
                           .setValue("DEBUG", "*;PushTest")
                           .setValue("INFO", "Production;PushTest");

        PushResponse response = configHub.pushQueue.flush();
    }
}
```
