package org.apache.commons.logging.impl;

import java.security.PrivilegedAction;

final class SimpleLog$1 implements PrivilegedAction {
   SimpleLog$1(String var1) {
      this.val$name = var1;
   }

   public Object run() {
      ClassLoader threadCL = SimpleLog.access$000();
      return threadCL != null ? threadCL.getResourceAsStream(this.val$name) : ClassLoader.getSystemResourceAsStream(this.val$name);
   }
}
