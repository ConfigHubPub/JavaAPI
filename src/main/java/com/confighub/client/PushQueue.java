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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages queuing and pushing data to the ConfigHub service.
 * <br>
 *
 * <pre>
 * {@code
 * ConfigHub confighub = new ConfigHub("Account", "RepositoryName")
 *          .setApplicationName("MyAppName");
 *
 * confighub.pushQueue.enableKeyCreation();
 * confighub.pushQueue.key("access.port")
 *                    .enablePush()
 *                    .setValueDataType(PushQueue.Key.ValueDataType.Integer)
 *                    .setValue(1002, "*;MyTestApp");
 *
 * confighub.pushQueue.flush();
 * }
 * </pre>
 */
public class PushQueue
{
    private Map<String, Key> keys = new HashMap<>();
    private final ConfigHub configHub;
    private boolean enableKeyCreation = false;
    private String changeComment = null;
    private static Gson gson = new Gson();

    protected PushQueue(final ConfigHub configHub)
    {
        this.configHub = configHub;
    }

    /**
     * Key object representing all updates that are to be made on this key, its attributes and values.
     */
    public static class Key
    {
        private final String name;
        private final Map<String, Value> values = new HashMap<>();
        private Map<String, Object> keyAttributes = new HashMap<>();

        /**
         * Data
         */
        public enum ValueDataType {
            Text, Code, Boolean, Integer, Long, Double, Float, Map, List
        }

        private Key(String name)
        {
            this.name = name;
        }

        /**
         * Create new value, or update an existing value with the same context.
         *
         * @param value property value
         * @param context value context
         * @return Key
         */
        public Key setValue(final Object value, final String context)
        {
            return setValue(value, context, true);
        }

        /**
         * Create new value, or update an existing value with the same context.
         * Active flag set according to the active flag.
         *
         * @param value property value
         * @param context value context
         * @param active flag
         * @return Key
         */
        public Key setValue(final Object value, final String context, boolean active)
        {
            values.put(context, new Value(value, context, active));
            return this;
        }

        /**
         * Property key's readme attribute.
         *
         * If not set, attribute will not be modified.
         * @param readme comments text
         * @return Key
         */
        public Key setReadme(final String readme)
        {
            this.keyAttributes.put("readme", readme);
            return this;
        }

        /**
         * Push flag for property key.  When enabled, API push updates for this key will be allowed.
         * If not set, attribute will not be modified.
         *
         * @return Key
         */
        public Key enablePush()
        {
            this.keyAttributes.put("push", true);
            return this;
        }

        /**
         * Push flag for property key.  When disabled, API push updates for this key will not be allowed.
         * If not set, attribute will not be modified.
         *
         * @return Key
         */
        public Key disablePush()
        {
            this.keyAttributes.put("push", false);
            return this;
        }

        /**
         * Value DataType is a field set on a property key which indicates a data type of the value.
         * Available types are defined by <code>enum ValueDataType</code>.
         *
         * If not specified, value already set for a key will not change, and if a new key is
         * created, its default value is <code>ValueDataType.Text</code>.
         *
         * @param vdt property key's value data type attribute
         * @return Key
         */
        public Key setValueDataType(ValueDataType vdt)
        {
            this.keyAttributes.put("vdt", vdt.name());
            return this;
        }

        /**
         * Mark this key a deprecated. If not set, attribute will not be modified.
         *
         * @return Key
         */
        public Key deprecate()
        {
            this.keyAttributes.put("deprecated", true);
            return this;
        }

        /**
         * Mark this key not deprecated.  If not set, attribute will not be modified.
         *
         * @return Key
         */
        public Key notDeprecated()
        {
            this.keyAttributes.put("deprecated", false);
            return this;
        }

        /**
         * Specify a security group to which a key should be assigned, and the group's password.
         * This method should also be used, if a key is already assigned to a security group,
         * and some of its attributes or values are being modified.
         *
         * If not set, attribute will not be modified.
         *
         * @param securityGroupName security group name
         * @param password security group password
         * @return Key
         */
        public Key setSecurityGroup(final String securityGroupName, final String password)
        {
            this.keyAttributes.put("securityGroup", securityGroupName);
            this.keyAttributes.put("password", password);
            return this;
        }

        private JsonObject toJson()
        {
            JsonObject json = new JsonObject();
            json.addProperty("key", this.name);
            this.keyAttributes.forEach((k, v) -> json.addProperty(k, v.toString()));

            if (this.values.size() > 0)
            {
                JsonArray values = new JsonArray();
                this.values.forEach((context, value) -> values.add(value.toJson()));
                json.add("values", values);
            }

            return json;
        }

        static class Value
        {
            final Object value;
            final String context;
            boolean active = true;

            Value(final Object value, final String context)
            {
                this.value = value;
                this.context = context;
            }

            Value(final Object value, final String context, boolean active)
            {
                this.value = value;
                this.context = context;
                this.active = active;
            }

            JsonObject toJson()
            {
                JsonObject json = new JsonObject();
                json.addProperty("context", this.context);
                json.addProperty("active", this.active);

                if (value instanceof Map)
                {
                    try
                    {
                        JsonObject map = new JsonObject();
                        Map<String, String> o = (Map) value;
                        o.forEach((k,v) -> map.addProperty(k,v));
                        json.add("value", map);
                    }
                    catch (Exception e)
                    {
                        throw new ConfigHubException("Invalid map value specified.  Map<String, String> is accepted as map value");
                    }
                }
                else if (value instanceof List)
                {
                    try
                    {
                        JsonArray list = new JsonArray();
                        List<String> o = (List) value;
                        o.forEach(v -> list.add(v));
                        json.add("value", list);
                    }
                    catch (Exception e)
                    {
                        throw new ConfigHubException("Invalid list value specified.  List<String> is accepted as list value");
                    }
                }
                else
                    json.addProperty("value", this.value.toString());

                return json;
            }
        }
    }

    /**
     * Get or create a property key.
     *
     * @param name property key
     * @return Key object
     */
    public Key key(final String name)
    {
        Key key = keys.get(name);
        if (null == key)
        {
            key = new Key(name);
            keys.put(name, key);
        }

        return key;
    }

    /**
     * Enable new key creation.  If specified key does not already exist, a new one will be created.
     */
    public void enableKeyCreation()
    {
        this.enableKeyCreation = true;
    }

    /**
     * Disable new key creation.  If a key is not already defined, new key will be rejected.
     */
    public void disableKeyCreation()
    {
        this.enableKeyCreation = false;
    }

    /**
     * Add change comment that will be visible when looking at revisions.
     *
     * @param changeComment revision comment
     */
    public void setChangeComment(String changeComment)
    {
        this.changeComment = changeComment;
    }

    private JsonObject buildJson()
    {
        JsonObject json = new JsonObject();

        JsonArray data = new JsonArray();
        keys.forEach((name, key) -> data.add(key.toJson()));

        if (null != this.changeComment)
            json.addProperty("changeComment", this.changeComment);

        if (this.enableKeyCreation)
            json.addProperty("enableKeyCreation", true);

        json.add("data", data);

        return json;
    }

    /**
     * Clears all changes added to the pushQueue.
     */
    public void clear()
    {
        this.keys.clear();
    }

    /**
     * Pushes all added changes to the ConfigHub service and clears the queue.
     *
     * @return PushResponse containing response status and message (if any).
     * @throws ConfigHubException containing error details
     */
    public synchronized PushResponse flush()
            throws ConfigHubException
    {
        PushResponse r = new PushResponse();

        try
        {
            JsonObject json = buildJson();
            String jsonString = gson.toJson(json);

            HttpURLConnection connection = configHub.getHttpsConnection("/rest/push");
            connection.setRequestProperty("Application-Name", configHub.applicationName);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Client-Version", configHub.clientApiVersion);
            connection.setRequestProperty("Content-Length", Integer.toString(jsonString.getBytes().length));

            connection.setRequestMethod( "POST" );

            connection.setUseCaches (false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            //Send request
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes (jsonString);
            wr.flush();
            wr.close();

            r.responseCode = connection.getResponseCode();
            r.message = connection.getHeaderField("ETag");

            clear();
        }
        catch (Exception e)
        {
            e.printStackTrace();

            r.responseCode = 0;
            r.message = e.getMessage();
        }

        return r;
    }

    /**
     * Contains status and message received from the pushQueue.flush response.
     */
    public static class PushResponse
    {
        int responseCode;
        String message;

        public int getResponseCode()
        {
            return responseCode;
        }

        public String getMessage()
        {
            return message;
        }
    }

}
