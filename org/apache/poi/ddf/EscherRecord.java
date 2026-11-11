package org.apache.poi.ddf;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import org.apache.poi.util.BitField;
import org.apache.poi.util.BitFieldFactory;
import org.apache.poi.util.HexDump;
import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndian;

public abstract class EscherRecord implements Cloneable {
   private static final BitField fInstance = BitFieldFactory.getInstance(65520);
   private static final BitField fVersion = BitFieldFactory.getInstance(15);
   private short _options;
   private short _recordId;

   protected int fillFields(byte[] data, EscherRecordFactory f) {
      return this.fillFields(data, 0, f);
   }

   public abstract int fillFields(byte[] var1, int var2, EscherRecordFactory var3);

   protected int readHeader(byte[] data, int offset) {
      this._options = LittleEndian.getShort(data, offset);
      this._recordId = LittleEndian.getShort(data, offset + 2);
      return LittleEndian.getInt(data, offset + 4);
   }

   protected static short readInstance(byte[] data, int offset) {
      short options = LittleEndian.getShort(data, offset);
      return fInstance.getShortValue(options);
   }

   public boolean isContainerRecord() {
      return this.getVersion() == 15;
   }

   @Internal
   public short getOptions() {
      return this._options;
   }

   @Internal
   public void setOptions(short options) {
      this.setVersion(fVersion.getShortValue(options));
      this.setInstance(fInstance.getShortValue(options));
      this._options = options;
   }

   public byte[] serialize() {
      byte[] retval = new byte[this.getRecordSize()];
      this.serialize(0, retval);
      return retval;
   }

   public int serialize(int offset, byte[] data) {
      return this.serialize(offset, data, new NullEscherSerializationListener());
   }

   public abstract int serialize(int var1, byte[] var2, EscherSerializationListener var3);

   public abstract int getRecordSize();

   public short getRecordId() {
      return this._recordId;
   }

   public void setRecordId(short recordId) {
      this._recordId = recordId;
   }

   public List<EscherRecord> getChildRecords() {
      return Collections.emptyList();
   }

   public void setChildRecords(List<EscherRecord> childRecords) {
      throw new UnsupportedOperationException("This record does not support child records.");
   }

   public EscherRecord clone() throws CloneNotSupportedException {
      return (EscherRecord)super.clone();
   }

   public EscherRecord getChild(int index) {
      return (EscherRecord)this.getChildRecords().get(index);
   }

   public void display(PrintWriter w, int indent) {
      for (int i = 0; i < indent * 4; i++) {
         w.print(' ');
      }

      w.println(this.getRecordName());
   }

   public abstract String getRecordName();

   public short getInstance() {
      return fInstance.getShortValue(this._options);
   }

   public void setInstance(short value) {
      this._options = fInstance.setShortValue(this._options, value);
   }

   public short getVersion() {
      return fVersion.getShortValue(this._options);
   }

   public void setVersion(short value) {
      this._options = fVersion.setShortValue(this._options, value);
   }

   public String toXml() {
      return this.toXml("");
   }

   public final String toXml(String tab) {
      String nl = System.getProperty("line.separator");
      String clsNm = this.getClass().getSimpleName();
      StringBuilder sb = new StringBuilder(1000);
      sb.append(tab)
         .append("<")
         .append(clsNm)
         .append(" recordId=\"0x")
         .append(HexDump.toHex(this.getRecordId()))
         .append("\" version=\"0x")
         .append(HexDump.toHex(this.getVersion()))
         .append("\" instance=\"0x")
         .append(HexDump.toHex(this.getInstance()))
         .append("\" options=\"0x")
         .append(HexDump.toHex(this.getOptions()))
         .append("\" recordSize=\"")
         .append(this.getRecordSize());
      Object[][] attrList = this.getAttributeMap();
      if (attrList != null && attrList.length != 0) {
         sb.append("\">").append(nl);
         String childTab = tab + "   ";

         for (Object[] attrs : attrList) {
            String tagName = capitalizeAndTrim((String)attrs[0]);
            boolean hasValue = false;
            boolean lastChildComplex = false;

            for (int i = 0; i < attrs.length; i += 2) {
               Object value = attrs[i + 1];
               if (value != null) {
                  if (!hasValue) {
                     sb.append(childTab).append("<").append(tagName).append(">");
                  }

                  String optName = capitalizeAndTrim((String)attrs[i + 0]);
                  if (i > 0) {
                     sb.append(nl).append(childTab).append("  <").append(optName).append(">");
                  }

                  lastChildComplex = appendValue(sb, value, true, childTab);
                  if (i > 0) {
                     sb.append(nl).append(childTab).append("  </").append(optName).append(">");
                  }

                  hasValue = true;
               }
            }

            if (hasValue) {
               if (lastChildComplex) {
                  sb.append(nl).append(childTab);
               }

               sb.append("</").append(tagName).append(">").append(nl);
            }
         }

         sb.append(tab).append("</").append(clsNm).append(">");
      } else {
         sb.append("\" />").append(nl);
      }

      return sb.toString();
   }

   public final String toString() {
      String nl = System.getProperty("line.separator");
      StringBuilder sb = new StringBuilder(1000);
      sb.append(this.getClass().getName())
         .append(" (")
         .append(this.getRecordName())
         .append("):")
         .append(nl)
         .append("  RecordId: 0x")
         .append(HexDump.toHex(this.getRecordId()))
         .append(nl)
         .append("  Version: 0x")
         .append(HexDump.toHex(this.getVersion()))
         .append(nl)
         .append("  Instance: 0x")
         .append(HexDump.toHex(this.getInstance()))
         .append(nl)
         .append("  Options: 0x")
         .append(HexDump.toHex(this.getOptions()))
         .append(nl)
         .append("  Record Size: ")
         .append(this.getRecordSize());
      Object[][] attrList = this.getAttributeMap();
      if (attrList != null && attrList.length > 0) {
         String childTab = "  ";

         for (Object[] attrs : attrList) {
            for (int i = 0; i < attrs.length; i += 2) {
               Object value = attrs[i + 1];
               if (value != null) {
                  String name = (String)attrs[i + 0];
                  sb.append(nl).append(childTab).append(name).append(": ");
                  appendValue(sb, value, false, childTab);
               }
            }
         }
      }

      return sb.toString();
   }

   private static boolean appendValue(StringBuilder sb, Object value, boolean toXML, String childTab) {
      String nl = System.getProperty("line.separator");
      boolean isComplex = false;
      if (value instanceof String) {
         if (toXML) {
            escapeXML((String)value, sb);
         } else {
            sb.append((String)value);
         }
      } else if (value instanceof Byte) {
         sb.append("0x").append(HexDump.toHex((Byte)value));
      } else if (value instanceof Short) {
         sb.append("0x").append(HexDump.toHex((Short)value));
      } else if (value instanceof Integer) {
         sb.append("0x").append(HexDump.toHex((Integer)value));
      } else if (value instanceof byte[]) {
         sb.append(nl).append(HexDump.toHex((byte[])value, 32).replaceAll("(?m)^", childTab + "   "));
      } else if (value instanceof Boolean) {
         sb.append((Boolean)value);
      } else if (value instanceof EscherRecord) {
         EscherRecord er = (EscherRecord)value;
         if (toXML) {
            sb.append(nl).append(er.toXml(childTab + "    "));
         } else {
            sb.append(er.toString().replaceAll("(?m)^", childTab));
         }

         isComplex = true;
      } else {
         if (!(value instanceof EscherProperty)) {
            throw new IllegalArgumentException("unknown attribute type " + value.getClass().getSimpleName());
         }

         EscherProperty ep = (EscherProperty)value;
         if (toXML) {
            sb.append(nl).append(ep.toXml(childTab + "  "));
         } else {
            sb.append(ep.toString().replaceAll("(?m)^", childTab));
         }

         isComplex = true;
      }

      return isComplex;
   }

   @Internal
   protected abstract Object[][] getAttributeMap();

   private static String capitalizeAndTrim(String str) {
      if (str != null && str.length() != 0) {
         StringBuilder sb = new StringBuilder(str.length());
         boolean capitalizeNext = true;

         for (char ch : str.toCharArray()) {
            if (!Character.isLetterOrDigit(ch)) {
               capitalizeNext = true;
            } else {
               if (capitalizeNext) {
                  if (!Character.isLetter(ch)) {
                     sb.append('_');
                  } else {
                     ch = Character.toTitleCase(ch);
                  }

                  capitalizeNext = false;
               }

               sb.append(ch);
            }
         }

         return sb.toString();
      } else {
         return str;
      }
   }

   private static void escapeXML(String s, StringBuilder out) {
      if (s != null && !s.isEmpty()) {
         for (char c : s.toCharArray()) {
            if (c <= 127 && c != '"' && c != '<' && c != '>' && c != '&') {
               out.append(c);
            } else {
               out.append("&#");
               out.append(c);
               out.append(';');
            }
         }
      }
   }
}
