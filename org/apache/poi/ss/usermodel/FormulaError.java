package org.apache.poi.ss.usermodel;

import java.util.HashMap;
import java.util.Map;
import org.apache.poi.util.Internal;

public enum FormulaError {
   @Internal
   _NO_ERROR(-1, "(no error)"),
   NULL(0, "#NULL!"),
   DIV0(7, "#DIV/0!"),
   VALUE(15, "#VALUE!"),
   REF(23, "#REF!"),
   NAME(29, "#NAME?"),
   NUM(36, "#NUM!"),
   NA(42, "#N/A"),
   CIRCULAR_REF(-60, "~CIRCULAR~REF~"),
   FUNCTION_NOT_IMPLEMENTED(-30, "~FUNCTION~NOT~IMPLEMENTED~");

   private final byte type;
   private final int longType;
   private final String repr;
   private static final Map<String, FormulaError> smap = new HashMap();
   private static final Map<Byte, FormulaError> bmap = new HashMap();
   private static final Map<Integer, FormulaError> imap = new HashMap();

   FormulaError(int type, String repr) {
      this.type = (byte)type;
      this.longType = type;
      this.repr = repr;
   }

   public byte getCode() {
      return this.type;
   }

   public int getLongCode() {
      return this.longType;
   }

   public String getString() {
      return this.repr;
   }

   public static boolean isValidCode(int errorCode) {
      for (FormulaError error : values()) {
         if (error.getCode() == errorCode) {
            return true;
         }

         if (error.getLongCode() == errorCode) {
            return true;
         }
      }

      return false;
   }

   public static FormulaError forInt(byte type) throws IllegalArgumentException {
      FormulaError err = (FormulaError)bmap.get(type);
      if (err == null) {
         throw new IllegalArgumentException("Unknown error type: " + type);
      } else {
         return err;
      }
   }

   public static FormulaError forInt(int type) throws IllegalArgumentException {
      FormulaError err = (FormulaError)imap.get(type);
      if (err == null) {
         err = (FormulaError)bmap.get((byte)type);
      }

      if (err == null) {
         throw new IllegalArgumentException("Unknown error type: " + type);
      } else {
         return err;
      }
   }

   public static FormulaError forString(String code) throws IllegalArgumentException {
      FormulaError err = (FormulaError)smap.get(code);
      if (err == null) {
         throw new IllegalArgumentException("Unknown error code: " + code);
      } else {
         return err;
      }
   }

   static {
      for (FormulaError error : values()) {
         bmap.put(error.getCode(), error);
         imap.put(error.getLongCode(), error);
         smap.put(error.getString(), error);
      }
   }
}
