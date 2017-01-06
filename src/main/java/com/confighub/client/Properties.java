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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.*;

/**
 * The Properties Object represents a set of properties. The Properties can be saved to a local file.
 * Each key and its corresponding value in the property map is by default a string.
 * If a key @Type is assigned in the ConfigHub UI, it can be received through a matching method.  For example:<br>
 * <br>
 * <code>@Type: Integer</code><br>
 * db.port<br>
 * <br>
 * In the API this property value can be read in several ways:<br>
 *
 * <pre>
 * {@code
 * Properties properties = new ConfigHub(token)
 *          .setContext("Production;MyAppName")
 *          .setApplicationName("MyAppName")
 *          .properties;
 *
 * int dbPort = properties.getInteger("db.port");
 *
 * // As a String
 * String dbPort = properties.get("db.port");
 *
 * // Or auto-cast
 * long dbPort = properties.getLong("db.port");
 * }
 * </pre>
 *
 * Structured objects like @Map and @List, when keep their values when read as a String in JSON format.
 * To received them in the native definition, get the value with respective methods:
 *
 * <pre>
 * {@code
 * Map<String, String> map = properties.getMap("data.map");
 * List<String> list = properties.getList("data.list");
 * }
 * </pre>
 */
public class Properties
{
    private final ConfigHub configHub;
    private Map<String, Value> data = new HashMap<>();

    //------------------------------------------------------------------------------------------------
    // API
    //------------------------------------------------------------------------------------------------

    /**
     * If a property value is encrypted, return the name of the security group it belongs to.
     *
     * @param key property key
     * @return name of the encryption group.  If its not encrypted return null.
     */
    public String getEncryptionGroup(final String key)
    {
        Value v = getProperty(key);
        if (null == v)
            return null;

        return v.encryptionGroup;
    }

    /**
     * Returns the state of the @Deprecated property flag from the ConfigHub UI.
     *
     * @param key property key
     * @return true if property is marked deprecated
     */
    public boolean isDeprecated(final String key)
    {
        Value v = this.data.get(key);
        return null != v && v.deprecated;
    }

    /**
     * @param key property key
     * @return true if value is String type
     */
    public boolean isString(final String key)
    {
        Value v = getProperty(key);
        return null != v && v.isString();
    }

    /**
     * @param key property key
     * @return true if value is Boolean type
     */
    public boolean isBoolean(final String key)
    {
        Value v = getProperty(key);
        return null != v && v.isBoolean();
    }

    /**
     * @param key property key
     * @return true if value is Integer type
     */
    public boolean isInteger(final String key)
    {
        Value v = getProperty(key);
        return null != v && v.isBoolean();
    }

    /**
     * @param key property key
     * @return true if value is Long type
     */
    public boolean isLong(final String key)
    {
        Value v = getProperty(key);
        return null != v && v.isLong();
    }

    /**
     * @param key property key
     * @return true if value is Double type
     */
    public boolean isDouble(final String key)
    {
        Value v = getProperty(key);
        return null != v && v.isDouble();
    }

    /**
     * @param key property key
     * @return true if value is Float type
     */
    public boolean isFloat(final String key)
    {
        Value v = getProperty(key);
        return null != v && v.isFloat();
    }

    /**
     * @param key property key
     * @return true if value is List type
     */
    public boolean isList(final String key)
    {
        Value v = getProperty(key);
        return null != v && v.isList();
    }

    /**
     * @param key property key
     * @return true if value is Map type
     */
    public boolean isMap(final String key)
    {
        Value v = getProperty(key);
        return null != v && v.isMap();
    }


    /**
     * Searches for the property with the specified key in this property list.
     *
     * @param key property key
     * @return value as <code>String</code> in the property list with the specified key value
     */
    public String get(final String key)
    {
        return get(key, null);
    }

    /**
     * Searches for the property with the specified key in this property list.
     *
     * @param key property key
     * @param defaultValue if key is not in the list, return this defaultValue
     * @return value as <code>String</code> in the property list with the specified key value
     */
    public String get(final String key, String defaultValue)
    {
        Value v = getProperty(key);
        if (null == v)
            return defaultValue;

        return v.get();
    }

    /**
     * Searches for the property with the specified key in this property list.
     * If a property is has @Type of Boolean its value is returned.
     * Otherwise, a String value or "true" or "false" will be parsed to a Boolean.
     *
     * @param key property key
     * @return value as <code>Boolean</code> in the property list with the specified key value
     */
    public Boolean getBoolean(final String key)
    {
        return getBoolean(key, null);
    }


    /**
     * Searches for the property with the specified key in this property list.
     * If a property is has @Type of Boolean its value is returned.
     * Otherwise, a String value or "true" or "false" will be parsed to a Boolean.
     *
     * @param key property key
     * @param defaultValue if key is not in the list, return this defaultValue
     * @return value as <code>Boolean</code> in the property list with the specified key value
     */
    public Boolean getBoolean(final String key, Boolean defaultValue)
    {
        Value v = getProperty(key);
        if (null == v)
            return defaultValue;

        return v.getBoolean();
    }

    /**
     * Searches for the property with the specified key in this property list.
     *
     * @param key property key
     * @return value as <code>Integer</code> in the property list with the specified key value
     */
    public Integer getInteger(final String key)
    {
        return getInteger(key, null);
    }

    /**
     * Searches for the property with the specified key in this property list.
     *
     * @param key property key
     * @param defaultValue if key is not in the list, return this defaultValue
     * @return value as <code>Integer</code> in the property list with the specified key value
     */
    public Integer getInteger(final String key, Integer defaultValue)
    {
        Value v = getProperty(key);
        if (null == v)
            return defaultValue;

        return v.getInteger();
    }

    /**
     * Searches for the property with the specified key in this property list.
     *
     * @param key property key
     * @return value as <code>Long</code> in the property list with the specified key value
     */
    public Long getLong(final String key)
    {
        return getLong(key, null);
    }

    /**
     * Searches for the property with the specified key in this property list.
     *
     * @param key property key
     * @param defaultValue if key is not in the list, return this defaultValue
     * @return value as <code>Long</code> in the property list with the specified key value
     */
    public Long getLong(final String key, Long defaultValue)
    {
        Value v = getProperty(key);
        if (null == v)
            return defaultValue;

        return v.getLong();
    }

    /**
     * Searches for the property with the specified key in this property list.
     *
     * @param key property key
     * @return value as <code>Double</code> in the property list with the specified key value
     */
    public Double getDouble(final String key)
    {
        return getDouble(key, null);
    }

    /**
     * Searches for the property with the specified key in this property list.
     *
     * @param key property key
     * @param defaultValue if key is not in the list, return this defaultValue
     * @return value as <code>Double</code> in the property list with the specified key value
     */
    public Double getDouble(final String key, Double defaultValue)
    {
        Value v = getProperty(key);
        if (null == v)
            return defaultValue;

        return v.getDouble();
    }

    /**
     * Searches for the property with the specified key in this property list.
     *
     * @param key property key
     * @return value as <code>Float</code> in the property list with the specified key value
     */
    public Float getFloat(final String key)
    {
        return getFloat(key, null);
    }

    /**
     * Searches for the property with the specified key in this property list.
     *
     * @param key property key
     * @param defaultValue if key is not in the list, return this defaultValue
     * @return value as <code>Float</code> in the property list with the specified key value
     */
    public Float getFloat(final String key, Float defaultValue)
    {
        Value v = getProperty(key);
        if (null == v)
            return defaultValue;

        return v.getFloat();
    }

    /**
     * Searches for the property with the specified key in this property list.
     *
     * @param key property key
     * @return value as <code>List</code> in the property list with the specified key value
     */
    public List<String> getList(final String key)
    {
        return getList(key, null);
    }

    /**
     * Searches for the property with the specified key in this property list.
     *
     * @param key property key
     * @param defaultValue if key is not in the list, return this defaultValue
     * @return value as <code>List</code> in the property list with the specified key value
     */
    public List<String> getList(final String key, List<String> defaultValue)
    {
        Value v = getProperty(key);
        if (null == v)
            return defaultValue;

        return v.getList();
    }

    /**
     * Searches for the property with the specified key in this property list.
     *
     * @param key property key
     * @return value as <code>Map</code> in the property list with the specified key value
     */
    public Map<String, String> getMap(final String key)
    {
        return getMap(key, null);
    }

    /**
     * Searches for the property with the specified key in this property list.
     *
     * @param key property key
     * @param defaultValue if key is not in the list, return this defaultValue
     * @return value as <code>Map</code> in the property list with the specified key value
     */
    public Map<String, String> getMap(final String key, Map<String, String> defaultValue)
    {
        Value v = getProperty(key);
        if (null == v)
            return defaultValue;

        return v.getMap();
    }


    /**
     * Get all configuration keys.
     *
     * @return Set of property keys
     */
    public Set<String> getKeys()
    {
        Set<String> keys = new HashSet<>();
        for (String key : this.data.keySet())
            keys.add(key);

        return keys;
    }


    //------------------------------------------------------------------------------------------------
    // Structure
    //------------------------------------------------------------------------------------------------

    private Value getProperty(final String key)
    {
        Value v = this.data.get(key);
        if (null == v)
            return null;

        if (v.deprecated)
            ConfigHub.log.warning("Deprecated property '" + key + "' used.");

        return v;
    }

    /**
     * @param configHub Object
     */
    protected Properties(final ConfigHub configHub)
    {
        this.configHub = configHub;
    }

    /*
     * Parse JSON configuration.
     */
    void readJson()
            throws ConfigHubException
    {
        this.data.clear();

        if (null == this.configHub.configJson)
            return;

        try
        {
            Iterator<Map.Entry<String, JsonElement>> itt = this.configHub.configJson.entrySet().iterator();

            while (itt.hasNext())
            {
                Map.Entry<String, JsonElement> entry = itt.next();
                String key = entry.getKey();
                JsonObject valueObject = entry.getValue().getAsJsonObject();

                parseEntry(key, valueObject);
            }
        }
        catch (Exception pe)
        {
            pe.printStackTrace();
            throw new ConfigHubException("Received invalid configuration.");
        }
    }

    /*
     * Parse a property
     */
    private void parseEntry(final String key, final JsonObject valueObject)
    {
        boolean deprecated = false;
        String encryptionGroup = null;

        if (valueObject.has("deprecated"))
            deprecated = valueObject.get("deprecated").getAsBoolean();

        String type = valueObject.has("type") ? valueObject.get("type").getAsString() : "Text";

        if (valueObject.has("encryption"))
        {
            encryptionGroup = valueObject.get("encryption").getAsString();
            this.data.put(key, new Value.TextValue(valueObject.get("val"), deprecated, encryptionGroup));
        } else
        {
            switch (type)
            {
                case "Text":
                case "Code":
                    this.data.put(key, new Value.TextValue(valueObject.get("val"), deprecated, encryptionGroup));
                    break;

                case "Boolean":
                    this.data.put(key, new Value.BooleanValue(valueObject.get("val"), deprecated, encryptionGroup));
                    break;

                case "Integer":
                    this.data.put(key, new Value.IntegerValue(valueObject.get("val"), deprecated, encryptionGroup));
                    break;

                case "Long":
                    this.data.put(key, new Value.LongValue(valueObject.get("val"), deprecated, encryptionGroup));
                    break;

                case "Double":
                    this.data.put(key, new Value.DoubleValue(valueObject.get("val"), deprecated, encryptionGroup));
                    break;

                case "Float":
                    this.data.put(key, new Value.FloatValue(valueObject.get("val"), deprecated, encryptionGroup));
                    break;

                case "Map":
                    this.data.put(key, new Value.MapValue(valueObject.get("val"), deprecated, encryptionGroup));
                    break;

                case "List":
                    this.data.put(key, new Value.ListValue(valueObject.get("val"), deprecated, encryptionGroup));
                    break;
            }
        }
    }

    abstract static class Value
    {
        final boolean deprecated;
        final String encryptionGroup;

        Value(final boolean deprecated, final String encryptionGroup)
        {
            this.deprecated = deprecated;
            this.encryptionGroup = encryptionGroup;
        }

        /**
         * @return true if value is String type
         */
        protected boolean isString() { return false; }

        /**
         * @return true if value is Boolean type
         */
        protected boolean isBoolean() { return false; }

        /**
         * @return true if value is Integer type
         */
        protected boolean isInteger() { return false; }

        /**
         * @return true if value is Long type
         */
        protected boolean isLong() { return false; }

        /**
         * @return true if value is Double type
         */
        protected boolean isDouble() { return false; }

        /**
         * @return true if value is Float type
         */
        protected boolean isFloat() { return false; }

        /**
         * @return true if value is List type
         */
        protected boolean isList() { return false; }

        /**
         * @return true if value is Map type
         */
        protected boolean isMap() { return false; }

        abstract String get();
        abstract Boolean getBoolean();
        abstract Integer getInteger();
        abstract Long getLong();
        abstract Double getDouble();
        abstract Float getFloat();
        abstract List<String> getList();
        abstract Map<String, String> getMap();


        protected static class TextValue
                extends Value
        {
            private final String value;

            protected TextValue(final JsonElement v, final boolean deprecated, final String encryptionProfile)
            {
                super(deprecated, encryptionProfile);
                value = v.getAsString();
            }

            protected TextValue(String v)
            {
                super(false, null);
                value = v;
            }

            @Override
            public boolean isString() { return true; }

            @Override
            String get()
            {
                return value;
            }

            @Override
            Boolean getBoolean() { return Boolean.parseBoolean(value); }

            @Override
            Integer getInteger()
            {
                return Integer.parseInt(value);
            }

            @Override
            Long getLong()
            {
                return Long.parseLong(value);
            }

            @Override
            Double getDouble()
            {
                return Double.parseDouble(value);
            }

            @Override
            Float getFloat()
            {
                return Float.parseFloat(value);
            }

            @Override
            List<String> getList() { throw new ClassCastException(); }

            @Override
            Map<String, String> getMap() { throw new ClassCastException(); }
        }

        protected static class BooleanValue
                extends Value
        {
            private final Boolean value;

            protected BooleanValue(final JsonElement v, final boolean deprecated, final String encryptionProfile)
            {
                super(deprecated, encryptionProfile);
                this.value = v.getAsBoolean();
            }

            @Override
            public boolean isBoolean() { return true; }

            @Override
            String get()
            {
                return value.toString();
            }

            @Override
            Boolean getBoolean() { return value; }

            @Override
            Integer getInteger()
            {
                throw new ClassCastException();
            }

            @Override
            Long getLong()
            {
                throw new ClassCastException();
            }

            @Override
            Double getDouble()
            {
                throw new ClassCastException();
            }

            @Override
            Float getFloat()
            {
                throw new ClassCastException();
            }

            @Override
            List<String> getList() { throw new ClassCastException(); }

            @Override
            Map<String, String> getMap() { throw new ClassCastException(); }
        }

        protected static class IntegerValue
                extends Value
        {
            private final Integer value;

            protected IntegerValue(final JsonElement v, final boolean deprecated, final String encryptionProfile)
            {
                super(deprecated, encryptionProfile);
                this.value = v.getAsInt();
            }

            @Override
            public boolean isInteger() { return true; }

            @Override
            String get()
            {
                return value.toString();
            }

            @Override
            Boolean getBoolean() { throw new ClassCastException(); }

            @Override
            Integer getInteger()
            {
                return value;
            }

            @Override
            Long getLong()
            {
                return value.longValue();
            }

            @Override
            Double getDouble()
            {
                return value.doubleValue();
            }

            @Override
            Float getFloat()
            {
                return value.floatValue();
            }

            @Override
            List<String> getList() { throw new ClassCastException(); }

            @Override
            Map<String, String> getMap() { throw new ClassCastException(); }
        }

        protected static class LongValue
                extends Value
        {
            private final Long value;

            protected LongValue(final JsonElement v, final boolean deprecated, final String encryptionProfile)
            {
                super(deprecated, encryptionProfile);
                this.value = v.getAsLong();
            }

            @Override
            public boolean isLong() { return true; }

            @Override
            String get()
            {
                return value.toString();
            }

            @Override
            Boolean getBoolean() { throw new ClassCastException(); }

            @Override
            Integer getInteger()
            {
                return value.intValue();
            }

            @Override
            Long getLong()
            {
                return value;
            }

            @Override
            Double getDouble()
            {
                return value.doubleValue();
            }

            @Override
            Float getFloat()
            {
                return value.floatValue();
            }

            @Override
            List<String> getList() { throw new ClassCastException(); }

            @Override
            Map<String, String> getMap() { throw new ClassCastException(); }
        }

        protected static class DoubleValue
                extends Value
        {
            private final Double value;

            protected DoubleValue(final JsonElement v, final boolean deprecated, final String encryptionProfile)
            {
                super(deprecated, encryptionProfile);
                this.value = v.getAsDouble();
            }

            @Override
            public boolean isDouble() { return true; }

            @Override
            String get()
            {
                return value.toString();
            }

            @Override
            Boolean getBoolean() { throw new ClassCastException(); }

            @Override
            Integer getInteger()
            {
                return value.intValue();
            }

            @Override
            Long getLong()
            {
                return value.longValue();
            }

            @Override
            Double getDouble()
            {
                return value;
            }

            @Override
            Float getFloat()
            {
                return value.floatValue();
            }

            @Override
            List<String> getList() { throw new ClassCastException(); }

            @Override
            Map<String, String> getMap() { throw new ClassCastException(); }
        }

        protected static class FloatValue
                extends Value
        {
            private final Float value;

            protected FloatValue(final JsonElement v, final boolean deprecated, final String encryptionProfile)
            {
                super(deprecated, encryptionProfile);
                this.value = v.getAsFloat();
            }

            @Override
            public boolean isFloat() { return true; }

            @Override
            String get()
            {
                return value.toString();
            }

            @Override
            Boolean getBoolean() { throw new ClassCastException(); }

            @Override
            Integer getInteger()
            {
                return value.intValue();
            }

            @Override
            Long getLong()
            {
                return value.longValue();
            }

            @Override
            Double getDouble()
            {
                return value.doubleValue();
            }

            @Override
            Float getFloat()
            {
                return value;
            }

            @Override
            List<String> getList() { throw new ClassCastException(); }

            @Override
            Map<String, String> getMap() { throw new ClassCastException(); }
        }

        protected static class MapValue
                extends Value
        {
            private final Map<String, String> value;
            private final String json;

            protected MapValue(final JsonElement v, final boolean deprecated, final String encryptionProfile)
            {
                super(deprecated, encryptionProfile);

                Type mapType = new TypeToken<Map<String, String>>() {}.getType();
                JsonObject obj = v.getAsJsonObject();
                this.json = v.toString();
                this.value = new Gson().fromJson(obj, mapType);
            }

            @Override
            public boolean isMap() { return true; }

            @Override
            String get()
            {
                return value.toString();
            }

            @Override
            Boolean getBoolean() { throw new ClassCastException(); }

            @Override
            Integer getInteger()
            {
                throw new ClassCastException();
            }

            @Override
            Long getLong()
            {
                throw new ClassCastException();
            }

            @Override
            Double getDouble()
            {
                throw new ClassCastException();
            }

            @Override
            Float getFloat()
            {
                throw new ClassCastException();
            }

            @Override
            List<String> getList() { throw new ClassCastException(); }

            @Override
            Map<String, String> getMap() { return value; }
        }

        protected static class ListValue
                extends Value
        {
            private final List<String> value;
            private final String json;

            protected ListValue(final JsonElement v, final boolean deprecated, final String encryptionProfile)
            {
                super(deprecated, encryptionProfile);

                Type listType = new TypeToken<List<String>>() {}.getType();
                JsonArray arr = v.getAsJsonArray();
                this.json = v.toString();
                this.value = new Gson().fromJson(arr, listType);
            }

            @Override
            public boolean isList() { return true; }

            @Override
            public boolean isMap() { return true; }

            @Override
            String get()
            {
                return value.toString();
            }

            @Override
            Boolean getBoolean() { throw new ClassCastException(); }

            @Override
            Integer getInteger()
            {
                throw new ClassCastException();
            }

            @Override
            Long getLong()
            {
                throw new ClassCastException();
            }

            @Override
            Double getDouble()
            {
                throw new ClassCastException();
            }

            @Override
            Float getFloat()
            {
                throw new ClassCastException();
            }

            @Override
            List<String> getList() { return value; }

            @Override
            Map<String, String> getMap() { throw new ClassCastException(); }
        }
    }
}
