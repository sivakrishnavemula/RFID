package org.apache.poi.ss.formula.ptg;

import org.apache.poi.ss.formula.function.FunctionMetadata;
import org.apache.poi.ss.formula.function.FunctionMetadataRegistry;
import org.apache.poi.util.LittleEndianInput;
import org.apache.poi.util.LittleEndianOutput;

public final class FuncVarPtg extends AbstractFunctionPtg {
   public static final byte sid = 34;
   private static final int SIZE = 4;
   public static final OperationPtg SUM = create("SUM", 1);

   private FuncVarPtg(int functionIndex, int returnClass, byte[] paramClasses, int numArgs) {
      super(functionIndex, returnClass, paramClasses, numArgs);
   }

   public static FuncVarPtg create(LittleEndianInput in) {
      return create(in.readByte(), in.readShort());
   }

   public static FuncVarPtg create(String pName, int numArgs) {
      return create(numArgs, lookupIndex(pName));
   }

   private static FuncVarPtg create(int numArgs, int functionIndex) {
      FunctionMetadata fm = FunctionMetadataRegistry.getFunctionByIndex(functionIndex);
      return fm == null
         ? new FuncVarPtg(functionIndex, 32, new byte[]{32}, numArgs)
         : new FuncVarPtg(functionIndex, fm.getReturnClassCode(), fm.getParameterClassCodes(), numArgs);
   }

   public void write(LittleEndianOutput out) {
      out.writeByte(34 + this.getPtgClass());
      out.writeByte(this.getNumberOfOperands());
      out.writeShort(this.getFunctionIndex());
   }

   public int getSize() {
      return 4;
   }
}
