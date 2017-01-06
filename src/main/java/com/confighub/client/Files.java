package com.confighub.client;

import com.confighub.client.error.ConfigHubException;
import com.google.gson.JsonElement;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Files Object holds all references to files that were requested and pulled.<br>
 * <br>
 * To request file(s):
 * <pre>
 * {@code
 * ConfigHub configHub = new ConfigHub(token)
 *          .setContext("Production;MyAppName")
 *          .setApplicationName("MyAppName");
 *
 * Files files = configHub.files;
 *
 * // Get settings file as a String
 * String settings = files.get("settings.conf");
 *
 * // Write a server.xml to file
 * files.writeFile("server.xml", "/local/path/to/server.xml");
 * }
 * </pre>
 */
public class Files
{
    private final ConfigHub configHub;
    private final Map<String, String> files = new HashMap<>();

    /**
     * @param configHub Object
     */
    protected Files(final ConfigHub configHub)
    {
        this.configHub = configHub;
    }

    public Set<String> getFileNames()
    {
        if (null == files) return null;
        return this.files.keySet();
    }

    /**
     * Get the resolved file as a String.
     *
     * @param fileName name of the file in your repository
     * @return file as a String
     */
    public String get(final String fileName)
    {
        return files.get(fileName.trim());
    }

    /**
     * White a resolved file locally.
     *
     * @param fileName name of the file in your repository
     * @param output local path and filename
     * @throws IOException is thrown if file cannot be written
     * @throws ConfigHubException is thrown if there was an error while pulling the file, of file is not found
     */
    public void writeFile(String fileName, String output)
            throws IOException, ConfigHubException
    {
        if (!this.files.containsKey(fileName))
            throw new ConfigHubException("Requested file '" + fileName + "' not pulled.");

        File file = new File(output);
        file.getParentFile().mkdirs();

        FileWriter fw = new FileWriter(file);
        fw.write(get(fileName));
        fw.close();
    }


    public boolean hasFile(String file)
    {
        return this.files.containsKey(file);
    }


    /*
     * Parse JSON configuration.
     */
    void readJson()
            throws ConfigHubException
    {
        this.files.clear();
        if (null == this.configHub.filesJson)
            return;

        try
        {
            Iterator<Map.Entry<String, JsonElement>> itt = this.configHub.filesJson.entrySet().iterator();

            while (itt.hasNext())
            {
                Map.Entry<String, JsonElement> entry = itt.next();
                files.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
        catch (Exception pe)
        {
            pe.printStackTrace();
            throw new ConfigHubException("Received invalid configuration.");
        }
    }
}
