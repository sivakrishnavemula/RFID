package org.apache.poi.util;

import java.util.HashMap;
import java.util.Map;

@Internal
public final class POILogFactory {
   private static final Map<String, POILogger> _loggers = new HashMap();
   private static final POILogger _nullLogger = new NullLogger();
   static String _loggerClassName;

   private POILogFactory() {
   }

   public static POILogger getLogger(Class<?> theclass) {
      return getLogger(theclass.getName());
   }

   public static POILogger getLogger(String cat) {
      if (_loggerClassName == null) {
         try {
            _loggerClassName = System.getProperty("org.apache.poi.util.POILogger");
         } catch (Exception var4) {
         }

         if (_loggerClassName == null) {
            _loggerClassName = _nullLogger.getClass().getName();
         }
      }

      if (_loggerClassName.equals(_nullLogger.getClass().getName())) {
         return _nullLogger;
      }

      POILogger logger = (POILogger)_loggers.get(cat);
      if (logger == null) {
         try {
            Class<? extends POILogger> loggerClass = Class.forName(_loggerClassName);
            logger = (POILogger)loggerClass.newInstance();
            logger.initialize(cat);
         } catch (Exception e) {
            logger = _nullLogger;
            _loggerClassName = _nullLogger.getClass().getName();
         }

         _loggers.put(cat, logger);
      }

      return logger;
   }
}
