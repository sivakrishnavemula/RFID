package org.apache.poi.common.usermodel.fonts;

public enum FontPitch {
   DEFAULT(0),
   FIXED(1),
   VARIABLE(2);

   private int nativeId;

   FontPitch(int nativeId) {
      this.nativeId = nativeId;
   }

   public int getNativeId() {
      return this.nativeId;
   }

   public static FontPitch valueOf(int flag) {
      for (FontPitch fp : values()) {
         if (fp.nativeId == flag) {
            return fp;
         }
      }

      return null;
   }

   public static byte getNativeId(FontPitch pitch, FontFamily family) {
      return (byte)(pitch.getNativeId() | family.getFlag() << 4);
   }

   public static FontPitch valueOfPitchFamily(byte pitchAndFamily) {
      return valueOf(pitchAndFamily & 3);
   }
}
