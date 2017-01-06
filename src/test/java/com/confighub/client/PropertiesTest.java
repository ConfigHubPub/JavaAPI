package com.confighub.client;

import com.confighub.client.error.ConfigHubException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests config pull, push, storing configuration to, and reading it from local file.
 * Test is exercised against https://demo.confighub.com/ConfigHub/UnitTest repository.
 */
public class PropertiesTest
{
    private ConfigHub configHub;

    @Before
    public void setup()
    {
        configHub = new ConfigHub("ConfigHub", "UnitTest");
        configHub.setApplicationName("Properties UnitTest");
        configHub.setConfighubServerAddress("demo.confighub.com");
    }

    @Test
    public void push()
    {
        try
        {
            configHub.pushQueue.enableKeyCreation();
            configHub.pushQueue.key("unittest.count.total")
                               .setReadme("Counts for some totals")
                               .enablePush()
                               .deprecate()
                               .setValueDataType(PushQueue.Key.ValueDataType.Integer)
                               .setValue(32, "Development;UnitTest");

            configHub.pushQueue.key("unittest.logger.level")
                               .enablePush()
                               .setValue("DEBUG", "*;MyApp")
                               .setValue("INFO", "Development;UnitTest");

            PushQueue.PushResponse response = configHub.pushQueue.flush();
            assertEquals(200, response.getResponseCode());

            configHub.pushQueue.key("unittest.count.total")
                               .enablePush()
                               .setValue(64, "Development;UnitTest");

            response = configHub.pushQueue.flush();
            assertEquals(200, response.getResponseCode());

            List<String> countries = new ArrayList<>();
            countries.add("US");
            countries.add("UK");
            countries.add("BA");

            configHub.pushQueue.key("unittest.countries")
                               .enablePush()
                               .setValueDataType(PushQueue.Key.ValueDataType.List)
                               .setValue(countries, "Development;UnitTest");

            response = configHub.pushQueue.flush();
            assertEquals(200, response.getResponseCode());
        }
        catch (ConfigHubException e)
        {
            System.out.println("Error: " + e.getMessage());
            Assert.fail();
        }
    }

    @Test
    public void pull()
    {
        configHub.setContext("Development;UnitTest");
        configHub.pull();

        try
        {
            configHub.toFile("/tmp/conf.json");
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail();
        }
        assertFalse(configHub.properties.isDeprecated("db.port"));
        assertFalse(configHub.properties.isLong("db.port"));

        assertEquals(new Integer(3306), configHub.properties.getInteger("db.port"));
        assertEquals(new Long(3306), configHub.properties.getLong("db.port"));
        assertEquals("3306", configHub.properties.get("db.port"));
    }

    @Test
    public void pullRead()
    {
        try {
            configHub.fromFile("/tmp/conf.json");
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail();
        }

        assertFalse(configHub.properties.isDeprecated("db.port"));
        assertFalse(configHub.properties.isLong("db.port"));

        assertEquals(new Integer(3306), configHub.properties.getInteger("db.port"));
        assertEquals(new Long(3306), configHub.properties.getLong("db.port"));
        assertEquals("3306", configHub.properties.get("db.port"));

        assertTrue(configHub.files.hasFile("server/conf/tomee.xml"));
    }
}
