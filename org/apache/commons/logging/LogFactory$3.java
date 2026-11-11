package org.apache.commons.logging;

import java.security.PrivilegedAction;

final class LogFactory$3 implements PrivilegedAction {
   LogFactory$3(ClassLoader var1, String var2) {
      this.val$loader = var1;
      this.val$name = var2;
   }

   public Object run() {
      return this.val$loader != null ? this.val$loader.getResourceAsStream(this.val$name) : ClassLoader.getSystemResourceAsStream(this.val$name);
   }
}
