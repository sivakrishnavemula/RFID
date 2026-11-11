package org.apache.poi.ddf;

public abstract class EscherProperty {
   private short _id;

   public EscherProperty(short id) {
      this._id = id;
   }

   public EscherProperty(short propertyNumber, boolean isComplex, boolean isBlipId) {
      this._id = (short)(propertyNumber + (isComplex ? 32768 : 0) + (isBlipId ? 16384 : 0));
   }

   public short getId() {
      return this._id;
   }

   public short getPropertyNumber() {
      return (short)(this._id & 16383);
   }

   public boolean isComplex() {
      return (this._id & -32768) != 0;
   }

   public boolean isBlipId() {
      return (this._id & 16384) != 0;
   }

   public String getName() {
      return EscherProperties.getPropertyName(this.getPropertyNumber());
   }

   public int getPropertySize() {
      return 6;
   }

   public String toXml(String tab) {
      StringBuilder builder = new StringBuilder();
      builder.append(tab)
         .append("<")
         .append(this.getClass().getSimpleName())
         .append(" id=\"")
         .append(this.getId())
         .append("\" name=\"")
         .append(this.getName())
         .append("\" blipId=\"")
         .append(this.isBlipId())
         .append("\"/>\n");
      return builder.toString();
   }

   public abstract int serializeSimplePart(byte[] var1, int var2);

   public abstract int serializeComplexPart(byte[] var1, int var2);

   public abstract String toString();
}
