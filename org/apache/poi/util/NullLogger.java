package org.apache.poi.util;

@Internal
public class NullLogger extends POILogger {
   public void initialize(String cat) {
   }

   protected void _log(int level, Object obj1) {
   }

   protected void _log(int level, Object obj1, Throwable exception) {
   }

   public void log(int level, Object... objs) {
   }

   public boolean check(int level) {
      return false;
   }
}
