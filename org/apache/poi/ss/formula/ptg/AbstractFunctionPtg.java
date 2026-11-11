package org.apache.poi.ss.formula.ptg;

import java.util.Locale;
import org.apache.poi.ss.formula.function.FunctionMetadata;
import org.apache.poi.ss.formula.function.FunctionMetadataRegistry;

public abstract class AbstractFunctionPtg extends OperationPtg {
   public static final String FUNCTION_NAME_IF = "IF";
   private static final short FUNCTION_INDEX_EXTERNAL = 255;
   private final byte returnClass;
   private final byte[] paramClass;
   private final int _numberOfArgs;
   private final short _functionIndex;

   protected AbstractFunctionPtg(int functionIndex, int pReturnClass, byte[] paramTypes, int nParams) {
      this._numberOfArgs = nParams;
      if (functionIndex >= -32768 && functionIndex <= 32767) {
         this._functionIndex = (short)functionIndex;
         if (pReturnClass >= -128 && pReturnClass <= 127) {
            this.returnClass = (byte)pReturnClass;
            this.paramClass = paramTypes;
         } else {
            throw new RuntimeException("pReturnClass " + pReturnClass + " cannot be cast to byte");
         }
      } else {
         throw new RuntimeException("functionIndex " + functionIndex + " cannot be cast to short");
      }
   }

   public final boolean isBaseToken() {
      return false;
   }

   public final String toString() {
      StringBuilder sb = new StringBuilder(64);
      sb.append(this.getClass().getName()).append(" [");
      sb.append(this.lookupName(this._functionIndex));
      sb.append(" nArgs=").append(this._numberOfArgs);
      sb.append("]");
      return sb.toString();
   }

   public final short getFunctionIndex() {
      return this._functionIndex;
   }

   public final int getNumberOfOperands() {
      return this._numberOfArgs;
   }

   public final String getName() {
      return this.lookupName(this._functionIndex);
   }

   public final boolean isExternalFunction() {
      return this._functionIndex == 255;
   }

   public final String toFormulaString() {
      return this.getName();
   }

   public String toFormulaString(String[] operands) {
      StringBuilder buf = new StringBuilder();
      if (this.isExternalFunction()) {
         buf.append(operands[0]);
         appendArgs(buf, 1, operands);
      } else {
         buf.append(this.getName());
         appendArgs(buf, 0, operands);
      }

      return buf.toString();
   }

   private static void appendArgs(StringBuilder buf, int firstArgIx, String[] operands) {
      buf.append('(');

      for (int i = firstArgIx; i < operands.length; i++) {
         if (i > firstArgIx) {
            buf.append(',');
         }

         buf.append(operands[i]);
      }

      buf.append(")");
   }

   public abstract int getSize();

   public static boolean isBuiltInFunctionName(String name) {
      short ix = FunctionMetadataRegistry.lookupIndexByName(name.toUpperCase(Locale.ROOT));
      return ix >= 0;
   }

   protected final String lookupName(short index) {
      if (index == 255) {
         return "#external#";
      } else {
         FunctionMetadata fm = FunctionMetadataRegistry.getFunctionByIndex(index);
         if (fm == null) {
            throw new RuntimeException("bad function index (" + index + ")");
         } else {
            return fm.getName();
         }
      }
   }

   protected static short lookupIndex(String name) {
      short ix = FunctionMetadataRegistry.lookupIndexByName(name.toUpperCase(Locale.ROOT));
      return ix < 0 ? 255 : ix;
   }

   public byte getDefaultOperandClass() {
      return this.returnClass;
   }

   public final byte getParameterClass(int index) {
      return index >= this.paramClass.length ? this.paramClass[this.paramClass.length - 1] : this.paramClass[index];
   }
}
