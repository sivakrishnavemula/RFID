package org.apache.poi.ss.formula.functions;

public class Finance {
   public static double pmt(double r, int nper, double pv, double fv, int type) {
      return -r * (pv * Math.pow(1.0 + r, nper) + fv) / ((1.0 + r * type) * (Math.pow(1.0 + r, nper) - 1.0));
   }

   public static double pmt(double r, int nper, double pv, double fv) {
      return pmt(r, nper, pv, fv, 0);
   }

   public static double pmt(double r, int nper, double pv) {
      return pmt(r, nper, pv, 0.0);
   }

   public static double ipmt(double r, int per, int nper, double pv, double fv, int type) {
      double ipmt = fv(r, per - 1, pmt(r, nper, pv, fv, type), pv, type) * r;
      if (type == 1) {
         ipmt /= 1.0 + r;
      }

      return ipmt;
   }

   public static double ipmt(double r, int per, int nper, double pv, double fv) {
      return ipmt(r, per, nper, pv, fv, 0);
   }

   public static double ipmt(double r, int per, int nper, double pv) {
      return ipmt(r, per, nper, pv, 0.0);
   }

   public static double ppmt(double r, int per, int nper, double pv, double fv, int type) {
      return pmt(r, nper, pv, fv, type) - ipmt(r, per, nper, pv, fv, type);
   }

   public static double ppmt(double r, int per, int nper, double pv, double fv) {
      return pmt(r, nper, pv, fv) - ipmt(r, per, nper, pv, fv);
   }

   public static double ppmt(double r, int per, int nper, double pv) {
      return pmt(r, nper, pv) - ipmt(r, per, nper, pv);
   }

   public static double fv(double r, int nper, double pmt, double pv, int type) {
      return -(pv * Math.pow(1.0 + r, nper) + pmt * (1.0 + r * type) * (Math.pow(1.0 + r, nper) - 1.0) / r);
   }

   public static double fv(double r, int nper, double c, double pv) {
      return fv(r, nper, c, pv, 0);
   }
}
