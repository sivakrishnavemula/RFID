package org.apache.poi.ss.util;

import java.math.BigInteger;

final class MutableFPNumber {
   private static final BigInteger BI_MIN_BASE = new BigInteger("0B5E620F47FFFE666", 16);
   private static final BigInteger BI_MAX_BASE = new BigInteger("0E35FA9319FFFE000", 16);
   private static final int C_64 = 64;
   private static final int MIN_PRECISION = 72;
   private BigInteger _significand;
   private int _binaryExponent;

   public MutableFPNumber(BigInteger frac, int binaryExponent) {
      this._significand = frac;
      this._binaryExponent = binaryExponent;
   }

   public MutableFPNumber copy() {
      return new MutableFPNumber(this._significand, this._binaryExponent);
   }

   public void normalise64bit() {
      int oldBitLen = this._significand.bitLength();
      int sc = oldBitLen - 64;
      if (sc != 0) {
         if (sc < 0) {
            throw new IllegalStateException("Not enough precision");
         }

         this._binaryExponent += sc;
         if (sc > 32) {
            int highShift = sc - 1 & 16777184;
            this._significand = this._significand.shiftRight(highShift);
            sc -= highShift;
            oldBitLen -= highShift;
         }

         if (sc < 1) {
            throw new IllegalStateException();
         }

         this._significand = MutableFPNumber.Rounder.round(this._significand, sc);
         if (this._significand.bitLength() > oldBitLen) {
            sc++;
            this._binaryExponent++;
         }

         this._significand = this._significand.shiftRight(sc);
      }
   }

   public int get64BitNormalisedExponent() {
      return this._binaryExponent + this._significand.bitLength() - 64;
   }

   public boolean isBelowMaxRep() {
      int sc = this._significand.bitLength() - 64;
      return this._significand.compareTo(BI_MAX_BASE.shiftLeft(sc)) < 0;
   }

   public boolean isAboveMinRep() {
      int sc = this._significand.bitLength() - 64;
      return this._significand.compareTo(BI_MIN_BASE.shiftLeft(sc)) > 0;
   }

   public NormalisedDecimal createNormalisedDecimal(int pow10) {
      int missingUnderBits = this._binaryExponent - 39;
      int fracPart = this._significand.intValue() << missingUnderBits & 16777088;
      long wholePart = this._significand.shiftRight(64 - this._binaryExponent - 1).longValue();
      return new NormalisedDecimal(wholePart, fracPart, pow10);
   }

   public void multiplyByPowerOfTen(int pow10) {
      MutableFPNumber.TenPower tp = MutableFPNumber.TenPower.getInstance(Math.abs(pow10));
      if (pow10 < 0) {
         this.mulShift(tp._divisor, tp._divisorShift);
      } else {
         this.mulShift(tp._multiplicand, tp._multiplierShift);
      }
   }

   private void mulShift(BigInteger multiplicand, int multiplierShift) {
      this._significand = this._significand.multiply(multiplicand);
      this._binaryExponent += multiplierShift;
      int sc = this._significand.bitLength() - 72 & -32;
      if (sc > 0) {
         this._significand = this._significand.shiftRight(sc);
         this._binaryExponent += sc;
      }
   }

   public ExpandedDouble createExpandedDouble() {
      return new ExpandedDouble(this._significand, this._binaryExponent);
   }

   private static final class Rounder {
      private static final BigInteger[] HALF_BITS;

      public static BigInteger round(BigInteger bi, int nBits) {
         return nBits < 1 ? bi : bi.add(HALF_BITS[nBits]);
      }

      static {
         BigInteger[] bis = new BigInteger[33];
         long acc = 1L;

         for (int i = 1; i < bis.length; i++) {
            bis[i] = BigInteger.valueOf(acc);
            acc <<= 1;
         }

         HALF_BITS = bis;
      }
   }

   private static final class TenPower {
      private static final BigInteger FIVE = new BigInteger("5");
      private static final MutableFPNumber.TenPower[] _cache = new MutableFPNumber.TenPower[350];
      public final BigInteger _multiplicand;
      public final BigInteger _divisor;
      public final int _divisorShift;
      public final int _multiplierShift;

      private TenPower(int index) {
         BigInteger fivePowIndex = FIVE.pow(index);
         int bitsDueToFiveFactors = fivePowIndex.bitLength();
         int px = 80 + bitsDueToFiveFactors;
         BigInteger fx = BigInteger.ONE.shiftLeft(px).divide(fivePowIndex);
         int adj = fx.bitLength() - 80;
         this._divisor = fx.shiftRight(adj);
         bitsDueToFiveFactors -= adj;
         this._divisorShift = -(bitsDueToFiveFactors + index + 80);
         int sc = fivePowIndex.bitLength() - 68;
         if (sc > 0) {
            this._multiplierShift = index + sc;
            this._multiplicand = fivePowIndex.shiftRight(sc);
         } else {
            this._multiplierShift = index;
            this._multiplicand = fivePowIndex;
         }
      }

      static MutableFPNumber.TenPower getInstance(int index) {
         MutableFPNumber.TenPower result = _cache[index];
         if (result == null) {
            result = new MutableFPNumber.TenPower(index);
            _cache[index] = result;
         }

         return result;
      }
   }
}
