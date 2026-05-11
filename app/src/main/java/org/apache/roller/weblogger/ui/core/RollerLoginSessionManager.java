package org.apache.roller.weblogger.ui.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.pojos.User;
import org.apache.roller.weblogger.util.cache.Cache;
import org.apache.roller.weblogger.util.cache.CacheHandler;
import org.apache.roller.weblogger.util.cache.CacheManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Session manager for handling Roller user sessions and login state.
 * Provides singleton access to centralized session cache management.
 */
public class RollerLoginSessionManager {
   private static final Log log = LogFactory.getLog(RollerLoginSessionManager.class);
   private static final String CACHE_ID = "roller.session.cache";
   private final Cache sessionCache;

   // Singleton instance initialized on class load
   private static final RollerLoginSessionManager INSTANCE = new RollerLoginSessionManager();

   public static RollerLoginSessionManager getInstance() {
      return INSTANCE;
   }

   /**
    * Internal cache handler implementing CacheHandler interface.
    * Handles session invalidation when users are removed from cache.
    */
   class SessionCacheHandler implements CacheHandler {
      @Override
      public void invalidate(User user) {
         if (user != null && user.getUserName() != null) {
            sessionCache.remove(user.getUserName());
         }
      }
   }

   /**
    * Testing constructor accepting a mock cache for unit testing.
    * @param cache Mock or test cache instance
    */
   RollerLoginSessionManager(Cache cache) {
      this.sessionCache = cache;
      CacheManager.registerHandler(new SessionCacheHandler());
   }

   /**
    * Production constructor initializing cache with default configuration.
    */
   private RollerLoginSessionManager() {
      Map<String, String> cacheProps = new HashMap<>();
      cacheProps.put("id", CACHE_ID);
      cacheProps.put("size", "1000");  // Cache up to 1000 sessions
      cacheProps.put("timeout", "3600"); // Session timeout in seconds (1 hour)
      this.sessionCache = CacheManager.constructCache(null, cacheProps);
      CacheManager.registerHandler(new SessionCacheHandler());
   }

   /**
    * Register a session for a user.
    * @param userName The username
    * @param session The RollerSession object
    */
   public void register(String userName, RollerSession session) {
      if (userName != null && session != null) {
         this.sessionCache.put(userName, session);
         log.debug("Registered session for user: " + userName);
      }
   }

   /**
    * Retrieve a session for a user.
    * @param userName The username
    * @return The RollerSession or null if not found
    */
   public RollerSession get(String userName) {
      if (userName != null) {
         return (RollerSession) this.sessionCache.get(userName);
      }
      return null;
   }

   /**
    * Invalidate a session by username.
    * @param userName The username
    */
   public void invalidate(String userName) {
      if (userName != null) {
         this.sessionCache.remove(userName);
         log.debug("Invalidated session for user: " + userName);
      }
   }
}


