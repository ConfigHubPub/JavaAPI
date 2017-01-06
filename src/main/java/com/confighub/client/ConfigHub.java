/*
 * The MIT License
 *
 *  Copyright (c) 2016, ConfigHub, LLC (support@configHub.com)
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package com.confighub.client;

import com.confighub.client.error.ConfigHubException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * The ConfigHub Object is an interface for either pulling configuration from ConfigHub API servers,
 * of reading from a locally stored configuration file.<br>
 * <pre>
 * {@code
 * ConfigHub confighub = new ConfigHub(token)
 *          .setContext("Production;MyAppName")
 *          .setApplicationName("MyAppName");
 *
 * Properties properties = confighub.properties;
 *
 * int dbPort = properties.getInteger("db.port");
 * }
 * </pre>
 *
 * Or, to also pull resolved files in addition to properties:
 *
 * <pre>
 * {@code
 * ConfigHub configHub = new ConfigHub(token, context)
 *          .setApplicationName("MyAppName");
 *
 * Files files = configHub.files;
 *
 * // Get settings file as a String
 * String settings = files.get("settings.conf");
 *
 * // Write a server.xml to file
 * files.writeFile("server.xml", "/local/path/to/server.xml");
 *
 * // Get the properties
 * int dbPort = configHub.properties.getInteger("db.port");
 * }
 * </pre>
 */
public final class ConfigHub
{
    static final Logger log = Logger.getLogger("ConfigHub");
    static final String clientApiVersion = "v1.3.0";

    private final String token;
    String context;

    private String tag;
    private String date;
    private Map<String, String> securityGroupAuth;
    private boolean includeContext;
    private boolean includeComments;

    String applicationName;
    private String confighubServerAddress;
    private boolean secureConnection = true;
    String account = null;
    String repositoryName = null;
    JsonObject configJson;
    JsonObject filesJson;

    public final Properties properties;
    public final Files files;
    public final PushQueue pushQueue;

    /**
     * Entry point of the configuration pull/read.
     *
     * @param token   for the repository
     * @throws ConfigHubException if token or context are missing
     */
    public ConfigHub(final String token)
            throws ConfigHubException
    {
        if (null == token || "".equals(token.trim()))
            throw new ConfigHubException("Token cannot be blank");

        this.token = token;
        this.pushQueue = new PushQueue(this);
        this.properties = new Properties(this);
        this.files = new Files(this);
    }

    /**
     * If accessing the repository without the token, account and repository name have to be set.
     *
     * @param account owner of the repository
     * @param repositoryName to identify a repository to pull configuration from
     */
    public ConfigHub(final String account, final String repositoryName)
            throws ConfigHubException
    {
        if (null == account || "".equals(account.trim()))
            throw new ConfigHubException("Account cannot be blank");

        if (null == repositoryName || "".equals(repositoryName.trim()))
            throw new ConfigHubException("Repository name cannot be blank");

        this.account = account;
        this.repositoryName = repositoryName;
        this.token = null;
        this.pushQueue = new PushQueue(this);
        this.properties = new Properties(this);
        this.files = new Files(this);
    }

    /**
     * Set the context for the pull request
     *
     * @param context of the configuration.  Context items are semi-colon (;) delimited.
     * @return ConfigHub
     */
    public ConfigHub setContext(final String context)
    {
        if (null == context || "".equals(context.trim()))
            throw new ConfigHubException("Context cannot be blank");

        this.context = context;
        return this;
    }

    /**
     * Set the address for the ConfigHub server.
     *
     * @param confighubServerAddress from which to pull configuration
     * @return ConfigHub object
     */
    public ConfigHub setConfighubServerAddress(String confighubServerAddress)
    {
        this.confighubServerAddress = confighubServerAddress;
        return this;
    }

    /**
     * http or https connection to the ConfigHub server.
     *
     * @param secureConnection true if secure https protocol should be used.  Otherwise, http.  Default: true.
     * @return ConfigHub object
     */
    public ConfigHub setSecureConnection(boolean secureConnection)
    {
        this.secureConnection = secureConnection;
        return this;
    }

    /**
     * Set the name of client application
     *
     * @param applicationName client application name
     * @return ConfigHub object
     */
    public ConfigHub setApplicationName(String applicationName)
    {
        this.applicationName = applicationName;
        return this;
    }

    /**
     * Get configuration from a date in a pre-defined tag.
     *
     * @param tag name
     * @return ConfigHub object
     */
    public ConfigHub setTag(String tag)
    {
        this.tag = tag;
        return this;
    }

    /**
     * Specify a date of the configuration.
     *
     * @param date String in UTC ISO 8601 format "YYYY-MM-DDTHH:MM:SSZ"
     * @return ConfigHub object
     */
    public ConfigHub setDate(String date)
    {
        this.date = date;
        return this;
    }

    /**
     * Properties that are assigned to a Security-Group can be decrypted before they are returned by
     * supplying the security group name and the password.
     * <p>
     * Alternatively, properties may be decrypted by assigning a security group to a token used by the
     * client application.
     * </p>
     * If neither method is used, property value will be returned encrypted, and it will be up to the
     * application to decrypt it.
     *
     * @param groupName to which property is assigned
     * @param password  of the security group
     * @return ConfigHub object
     */
    public ConfigHub decryptSecurityGroup(String groupName, String password)
    {
        if (null == this.securityGroupAuth)
            this.securityGroupAuth = new HashMap<>();

        this.securityGroupAuth.put(groupName, password);
        return this;
    }

    /**
     * Return comments entered for keys.
     *
     * @param includeComments true if comments should be returned.  Default is false.
     * @return ConfigHub object
     */
    public ConfigHub includeComments(boolean includeComments)
    {
        this.includeComments = includeComments;
        return this;
    }

    /**
     * Context of the property value returned.
     *
     * @param includeContext true if context should be returned.  Default is false.
     * @return ConfigHub object
     */
    public ConfigHub includeContext(boolean includeContext)
    {
        this.includeContext = includeContext;
        return this;
    }

    /**
     * Rather than pulling from ConfigHub servers, read properties from a saved ConfigHub
     * properties file.
     *
     * @param file configuration JSON file
     * @return Properties Object
     * @throws IOException is thrown if file cannot be read
     */
    public Properties fromFile(String file)
            throws IOException
    {
        BufferedReader br = new BufferedReader(new FileReader(file));

        String line;
        StringBuilder sb = new StringBuilder();
        while ((line = br.readLine()) != null)
            sb.append(line);
        br.close();

        readJson(sb.toString());
        return this.properties;
    }

    /**
     * Path and name of file where configuration will be saved.
     *
     * @param out path and name
     * @throws IOException is thrown if file cannot be written
     */
    public void toFile(String out)
            throws IOException
    {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject json = new JsonObject();
        json.addProperty("context", this.context);
        json.addProperty("account", this.account);
        json.addProperty("repo", this.repositoryName);
        json.add("properties", this.configJson);
        json.add("files", this.filesJson);

        String js = gson.toJson(json);

        File output = new File(out);
        output.getParentFile().mkdirs();

        FileWriter fw = new FileWriter(output);
        fw.write(js);
        fw.close();

        ConfigHub.log.info("Wrote configuration to file: " + output.getCanonicalPath());
    }

    HttpURLConnection getHttpsConnection(String rest)
        throws IOException
    {
        StringBuilder url = new StringBuilder();
        url.append(this.secureConnection ? "https" : "http")
           .append("://")
           .append(this.confighubServerAddress)
           .append(rest);

        if (null == token)
        {
            if (null == this.account || null == this.repositoryName)
                throw new ConfigHubException("Either token, or account and repository name have to be specified.");

            url.append("/")
               .append(this.account)
               .append("/")
               .append(this.repositoryName);
        }

        URL chUrl = new URL(url.toString());
        log.info("Connecting to ConfigHub via url: " + url.toString());

        HttpURLConnection connection;

        if (this.secureConnection)
            connection = (HttpsURLConnection)chUrl.openConnection();
        else
            connection = (HttpURLConnection)chUrl.openConnection();

        if (null != this.token)
            connection.setRequestProperty("Client-Token", this.token);

        return connection;
    }


    public synchronized void pull()
            throws ConfigHubException
    {
        try
        {
            HttpURLConnection connection = getHttpsConnection("/rest/pull");

            connection.setRequestProperty("Context", this.context);
            connection.setRequestProperty("Repository-Date", this.date);
            connection.setRequestProperty("Tag", this.tag);

            connection.setRequestProperty("Application-Name", this.applicationName);
            connection.setRequestProperty("Security-Profile-Auth", null == this.securityGroupAuth ? null :
                    (new Gson()).toJson(this.securityGroupAuth));
            connection.setRequestProperty("Include-Comments", this.includeComments ? "true" : "false");
            connection.setRequestProperty("Include-Value-Context", this.includeContext ? "true" : "false");

            int code = connection.getResponseCode();

            switch (code)
            {
                case 200:
                {
                    InputStreamReader io = new InputStreamReader(connection.getInputStream());
                    BufferedReader in = new BufferedReader(io);

                    String line;
                    StringBuilder sb = new StringBuilder();
                    while ((line = in.readLine()) != null)
                        sb.append(line);
                    in.close();

                    readJson(sb.toString());
                    break;
                }

                case 401:
                    log.severe("Token no longer authorized");
                    break;

                case 404:
                    log.severe("Requested repository not found");
                    break;

                case 406:
                    log.severe("Invalid token");
                    break;

                case 500:
                    log.severe("ConfigHub - Internal server error");
                    break;
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new ConfigHubException("Failed to get configuration: " + e.getMessage());
        }

    }


    /*
     * Parse JSON configuration.
     */
    private void readJson(String json)
            throws ConfigHubException
    {
        if (null == json)
            return;

        Gson gson = new Gson();
        try
        {
            JsonObject data = gson.fromJson(json, JsonObject.class);

            if (null != context && !context.equals(data.get("context").getAsString()))
            {
                String message = "Requested context '" + context + "' is not the same as context " +
                        "in the configuration file: '" + data
                        .get("context")
                        .getAsString() + "'.";

                log.severe(message);
                throw new ConfigHubException(message);
            }

            String errorMessage = data.has("error") ? data.get("error").getAsString() : null;

            if (null != errorMessage)
            {
                log.severe(errorMessage);
                throw new ConfigHubException(errorMessage);
            }

            this.account = data.get("account").getAsString();
            this.repositoryName = data.get("repo").getAsString();

            this.configJson = data.getAsJsonObject("properties");
            this.properties.readJson();

            this.filesJson = data.getAsJsonObject("files");
            this.files.readJson();
        }
        catch (Exception pe)
        {
            pe.printStackTrace();
            throw new ConfigHubException("Received invalid configuration.");
        }
    }


}
