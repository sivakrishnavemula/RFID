package org.apache.commons.logging;

import java.io.IOException;
import java.security.PrivilegedAction;

final class LogFactory$4 implements PrivilegedAction {
   LogFactory$4(ClassLoader var1, String var2) {
      this.val$loader = var1;
      this.val$name = var2;
   }

   public Object run() {
      try {
         return this.val$loader != null ? this.val$loader.getResources(this.val$name) : ClassLoader.getSystemResources(this.val$name);
      } catch (IOException e) {
         if (LogFactory.isDiagnosticsEnabled()) {
            LogFactory.access$000("Exception while trying to find configuration file " + this.val$name + ":" + e.getMessage());
         }

         return null;
      } catch (NoSuchMethodError e) {
         return null;
      }
   }
}
