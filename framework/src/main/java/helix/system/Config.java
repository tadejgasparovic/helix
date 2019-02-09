package helix.system;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config
{
    private static Config config;

    private Properties properties;

    private String[] REQUIRED_PROPERTIES = new String[]{
            "version",
            "versionCheck",
            "clearnetUpdateAllowed"
    };

    /**
     * Attempts to load the config
     * @throws IOException If config load fails
     * **/
    private Config() throws IOException
    {
        InputStream inputStream = Config.class.getResourceAsStream("/config");

        if(inputStream == null) throw new RuntimeException("No global config found");

        properties = new Properties();
        properties.load(inputStream);

        for(String prop : REQUIRED_PROPERTIES)
        {
            if(!properties.containsKey(prop)) throw new RuntimeException("Missing config properties!");
        }
    }

    /**
     * Initializes a new config instance if necessary and returns it
     * @return Instance of the config class
     * @throws IOException If instance initialization fails
     * **/
    public static Config instance() throws IOException
    {
        if(config == null) config = new Config();
        return config;
    }

    /**
     * Wrapper for Properties.getProperty(String)
     * @param key Config property key
     * @return Config property value
     * **/
    public String getProperty(String key)
    {
        return properties.getProperty(key);
    }

    /**
     * Wrapper for Properties.getProperty(String, String)
     * @param key Config property key
     * @param defaultValue Default value if the key doesn't exist
     * @return Config property value or <code>defaultValue</code>
     * **/
    public String getProperty(String key, String defaultValue)
    {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Wrapper for Properties.get(Object)
     * @param key Config property key
     * @return Config property value
     * **/
    public Object get(Object key)
    {
        return properties.get(key);
    }

    /**
     * Wrapper for Properties.getOrDefault(Object, Object)
     * @param key Config property key
     * @param defaultValue Default value if the key doesn't exist
     * @return Config property value or <code>defaultValue</code>
     * **/
    public Object getOrDefault(Object key, Object defaultValue)
    {
        return properties.getOrDefault(key, defaultValue);
    }
}
