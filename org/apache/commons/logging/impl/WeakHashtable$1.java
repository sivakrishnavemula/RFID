package org.apache.commons.logging.impl;

import java.util.Enumeration;
import org.apache.commons.logging.impl.WeakHashtable.Referenced;

class WeakHashtable$1 implements Enumeration {
   WeakHashtable$1(WeakHashtable var1, Enumeration var2) {
      this.this$0 = var1;
      this.val$enumer = var2;
   }

   public boolean hasMoreElements() {
      return this.val$enumer.hasMoreElements();
   }

   public Object nextElement() {
      Referenced nextReference = (Referenced)this.val$enumer.nextElement();
      return Referenced.access$100(nextReference);
   }
}
