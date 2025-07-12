package EEssentials.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom logger for EEssentials with consistent formatting
 */
public class EEssentialsLogger {
    private final Logger LOGGER = LoggerFactory.getLogger("EEssentials");
    
    public void info(Object message) {
        LOGGER.info("[EEssentials] " + message);
    }
    
    public void warn(Object message) {
        LOGGER.warn("[EEssentials] " + message);
    }
    
    public void warn(Object message, Throwable throwable) {
        LOGGER.warn("[EEssentials] " + message, throwable);
    }
    
    public void error(Object message) {
        LOGGER.error("[EEssentials] " + message);
    }
    
    public void error(Object message, Throwable throwable) {
        LOGGER.error("[EEssentials] " + message, throwable);
    }
    
    public void debug(Object message) {
        LOGGER.debug("[EEssentials] " + message);
    }
    
    public void trace(Object message) {
        LOGGER.trace("[EEssentials] " + message);
    }
} 