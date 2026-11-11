package org.apache.poi.ss.formula.functions;

public final class Sumifs extends Baseifs {
   public static final FreeRefFunction instance = new Sumifs();

   protected boolean hasInitialRange() {
      return true;
   }
}
