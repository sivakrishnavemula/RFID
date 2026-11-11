package org.apache.poi.ss.formula.ptg;

import org.apache.poi.util.LittleEndianInput;

public final class AreaNPtg extends Area2DPtgBase {
   public static final short sid = 45;

   public AreaNPtg(LittleEndianInput in) {
      super(in);
   }

   protected byte getSid() {
      return 45;
   }
}
