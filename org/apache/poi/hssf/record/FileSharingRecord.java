package org.apache.poi.hssf.record;

import org.apache.poi.util.LittleEndianOutput;
import org.apache.poi.util.StringUtil;

public final class FileSharingRecord extends StandardRecord implements Cloneable {
   public static final short sid = 91;
   private short field_1_readonly;
   private short field_2_password;
   private byte field_3_username_unicode_options;
   private String field_3_username_value;

   public FileSharingRecord() {
   }

   public FileSharingRecord(RecordInputStream in) {
      this.field_1_readonly = in.readShort();
      this.field_2_password = in.readShort();
      int nameLen = in.readShort();
      if (nameLen > 0) {
         this.field_3_username_unicode_options = in.readByte();
         this.field_3_username_value = in.readCompressedUnicode(nameLen);
      } else {
         this.field_3_username_value = "";
      }
   }

   public void setReadOnly(short readonly) {
      this.field_1_readonly = readonly;
   }

   public short getReadOnly() {
      return this.field_1_readonly;
   }

   public void setPassword(short password) {
      this.field_2_password = password;
   }

   public short getPassword() {
      return this.field_2_password;
   }

   public String getUsername() {
      return this.field_3_username_value;
   }

   public void setUsername(String username) {
      this.field_3_username_value = username;
   }

   public String toString() {
      StringBuffer buffer = new StringBuffer();
      buffer.append("[FILESHARING]\n");
      buffer.append("    .readonly       = ").append(this.getReadOnly() == 1 ? "true" : "false").append("\n");
      buffer.append("    .password       = ").append(Integer.toHexString(this.getPassword())).append("\n");
      buffer.append("    .username       = ").append(this.getUsername()).append("\n");
      buffer.append("[/FILESHARING]\n");
      return buffer.toString();
   }

   public void serialize(LittleEndianOutput out) {
      out.writeShort(this.getReadOnly());
      out.writeShort(this.getPassword());
      out.writeShort(this.field_3_username_value.length());
      if (this.field_3_username_value.length() > 0) {
         out.writeByte(this.field_3_username_unicode_options);
         StringUtil.putCompressedUnicode(this.getUsername(), out);
      }
   }

   protected int getDataSize() {
      int nameLen = this.field_3_username_value.length();
      return nameLen < 1 ? 6 : 7 + nameLen;
   }

   public short getSid() {
      return 91;
   }

   public FileSharingRecord clone() {
      FileSharingRecord clone = new FileSharingRecord();
      clone.setReadOnly(this.field_1_readonly);
      clone.setPassword(this.field_2_password);
      clone.setUsername(this.field_3_username_value);
      return clone;
   }
}
