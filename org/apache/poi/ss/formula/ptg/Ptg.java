package org.apache.poi.ss.formula.ptg;

import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.formula.ptg.ArrayPtg.Initial;
import org.apache.poi.util.LittleEndianByteArrayOutputStream;
import org.apache.poi.util.LittleEndianInput;
import org.apache.poi.util.LittleEndianOutput;

public abstract class Ptg {
   public static final Ptg[] EMPTY_PTG_ARRAY = new Ptg[0];
   public static final byte CLASS_REF = 0;
   public static final byte CLASS_VALUE = 32;
   public static final byte CLASS_ARRAY = 64;
   private byte ptgClass = 0;

   public static Ptg[] readTokens(int size, LittleEndianInput in) {
      List<Ptg> temp = new ArrayList(4 + size / 2);
      int pos = 0;
      boolean hasArrayPtgs = false;

      while (pos < size) {
         Ptg ptg = createPtg(in);
         if (ptg instanceof Initial) {
            hasArrayPtgs = true;
         }

         pos += ptg.getSize();
         temp.add(ptg);
      }

      if (pos != size) {
         throw new RuntimeException("Ptg array size mismatch");
      }

      if (hasArrayPtgs) {
         Ptg[] result = toPtgArray(temp);

         for (int i = 0; i < result.length; i++) {
            if (result[i] instanceof Initial) {
               result[i] = ((Initial)result[i]).finishReading(in);
            }
         }

         return result;
      } else {
         return toPtgArray(temp);
      }
   }

   public static Ptg createPtg(LittleEndianInput in) {
      byte id = in.readByte();
      if (id < 32) {
         return createBasePtg(id, in);
      }

      Ptg retval = createClassifiedPtg(id, in);
      if (id >= 96) {
         retval.setClass((byte)64);
      } else if (id >= 64) {
         retval.setClass((byte)32);
      } else {
         retval.setClass((byte)0);
      }

      return retval;
   }

   private static Ptg createClassifiedPtg(byte id, LittleEndianInput in) {
      int baseId = id & 31 | 32;
      switch (baseId) {
         case 32:
            return new Initial(in);
         case 33:
            return FuncPtg.create(in);
         case 34:
            return FuncVarPtg.create(in);
         case 35:
            return new NamePtg(in);
         case 36:
            return new RefPtg(in);
         case 37:
            return new AreaPtg(in);
         case 38:
            return new MemAreaPtg(in);
         case 39:
            return new MemErrPtg(in);
         case 40:
         case 46:
         case 47:
         case 48:
         case 49:
         case 50:
         case 51:
         case 52:
         case 53:
         case 54:
         case 55:
         case 56:
         default:
            throw new UnsupportedOperationException(" Unknown Ptg in Formula: 0x" + Integer.toHexString(id) + " (" + id + ")");
         case 41:
            return new MemFuncPtg(in);
         case 42:
            return new RefErrorPtg(in);
         case 43:
            return new AreaErrPtg(in);
         case 44:
            return new RefNPtg(in);
         case 45:
            return new AreaNPtg(in);
         case 57:
            return new NameXPtg(in);
         case 58:
            return new Ref3DPtg(in);
         case 59:
            return new Area3DPtg(in);
         case 60:
            return new DeletedRef3DPtg(in);
         case 61:
            return new DeletedArea3DPtg(in);
      }
   }

   private static Ptg createBasePtg(byte id, LittleEndianInput in) {
      switch (id) {
         case 0:
            return new UnknownPtg(id);
         case 1:
            return new ExpPtg(in);
         case 2:
            return new TblPtg(in);
         case 3:
            return AddPtg.instance;
         case 4:
            return SubtractPtg.instance;
         case 5:
            return MultiplyPtg.instance;
         case 6:
            return DividePtg.instance;
         case 7:
            return PowerPtg.instance;
         case 8:
            return ConcatPtg.instance;
         case 9:
            return LessThanPtg.instance;
         case 10:
            return LessEqualPtg.instance;
         case 11:
            return EqualPtg.instance;
         case 12:
            return GreaterEqualPtg.instance;
         case 13:
            return GreaterThanPtg.instance;
         case 14:
            return NotEqualPtg.instance;
         case 15:
            return IntersectionPtg.instance;
         case 16:
            return UnionPtg.instance;
         case 17:
            return RangePtg.instance;
         case 18:
            return UnaryPlusPtg.instance;
         case 19:
            return UnaryMinusPtg.instance;
         case 20:
            return PercentPtg.instance;
         case 21:
            return ParenthesisPtg.instance;
         case 22:
            return MissingArgPtg.instance;
         case 23:
            return new StringPtg(in);
         case 24:
         case 26:
         case 27:
         default:
            throw new RuntimeException("Unexpected base token id (" + id + ")");
         case 25:
            return new AttrPtg(in);
         case 28:
            return ErrPtg.read(in);
         case 29:
            return BoolPtg.read(in);
         case 30:
            return new IntPtg(in);
         case 31:
            return new NumberPtg(in);
      }
   }

   private static Ptg[] toPtgArray(List<Ptg> l) {
      if (l.isEmpty()) {
         return EMPTY_PTG_ARRAY;
      }

      Ptg[] result = new Ptg[l.size()];
      l.toArray(result);
      return result;
   }

   public static int getEncodedSize(Ptg[] ptgs) {
      int result = 0;

      for (Ptg ptg : ptgs) {
         result += ptg.getSize();
      }

      return result;
   }

   public static int getEncodedSizeWithoutArrayData(Ptg[] ptgs) {
      int result = 0;

      for (Ptg ptg : ptgs) {
         if (ptg instanceof ArrayPtg) {
            result += 8;
         } else {
            result += ptg.getSize();
         }
      }

      return result;
   }

   public static int serializePtgs(Ptg[] ptgs, byte[] array, int offset) {
      LittleEndianByteArrayOutputStream out = new LittleEndianByteArrayOutputStream(array, offset);
      List<Ptg> arrayPtgs = null;

      for (Ptg ptg : ptgs) {
         ptg.write(out);
         if (ptg instanceof ArrayPtg) {
            if (arrayPtgs == null) {
               arrayPtgs = new ArrayList(5);
            }

            arrayPtgs.add(ptg);
         }
      }

      if (arrayPtgs != null) {
         for (Ptg arrayPtg : arrayPtgs) {
            ArrayPtg p = (ArrayPtg)arrayPtg;
            p.writeTokenValueBytes(out);
         }
      }

      return out.getWriteIndex() - offset;
   }

   public abstract int getSize();

   public abstract void write(LittleEndianOutput var1);

   public abstract String toFormulaString();

   public String toString() {
      return this.getClass().toString();
   }

   public final void setClass(byte thePtgClass) {
      if (this.isBaseToken()) {
         throw new RuntimeException("setClass should not be called on a base token");
      }

      this.ptgClass = thePtgClass;
   }

   public final byte getPtgClass() {
      return this.ptgClass;
   }

   public final char getRVAType() {
      if (this.isBaseToken()) {
         return '.';
      }

      switch (this.ptgClass) {
         case 0:
            return 'R';
         case 32:
            return 'V';
         case 64:
            return 'A';
         default:
            throw new RuntimeException("Unknown operand class (" + this.ptgClass + ")");
      }
   }

   public abstract byte getDefaultOperandClass();

   public abstract boolean isBaseToken();

   public static boolean doesFormulaReferToDeletedCell(Ptg[] ptgs) {
      for (Ptg ptg : ptgs) {
         if (isDeletedCellRef(ptg)) {
            return true;
         }
      }

      return false;
   }

   private static boolean isDeletedCellRef(Ptg ptg) {
      if (ptg == ErrPtg.REF_INVALID) {
         return true;
      } else if (ptg instanceof DeletedArea3DPtg) {
         return true;
      } else if (ptg instanceof DeletedRef3DPtg) {
         return true;
      } else {
         return ptg instanceof AreaErrPtg ? true : ptg instanceof RefErrorPtg;
      }
   }
}
