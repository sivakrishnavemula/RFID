package org.apache.poi.ddf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.util.LittleEndian;

public abstract class AbstractEscherOptRecord extends EscherRecord {
   private List<EscherProperty> properties = new ArrayList();

   public void addEscherProperty(EscherProperty prop) {
      this.properties.add(prop);
   }

   public int fillFields(byte[] data, int offset, EscherRecordFactory recordFactory) {
      int bytesRemaining = this.readHeader(data, offset);
      short propertiesCount = readInstance(data, offset);
      int pos = offset + 8;
      EscherPropertyFactory f = new EscherPropertyFactory();
      this.properties = f.createProperties(data, pos, propertiesCount);
      return bytesRemaining + 8;
   }

   public List<EscherProperty> getEscherProperties() {
      return this.properties;
   }

   public EscherProperty getEscherProperty(int index) {
      return (EscherProperty)this.properties.get(index);
   }

   private int getPropertiesSize() {
      int totalSize = 0;

      for (EscherProperty property : this.properties) {
         totalSize += property.getPropertySize();
      }

      return totalSize;
   }

   public int getRecordSize() {
      return 8 + this.getPropertiesSize();
   }

   public <T extends EscherProperty> T lookup(int propId) {
      for (EscherProperty prop : this.properties) {
         if (prop.getPropertyNumber() == propId) {
            return (T)prop;
         }
      }

      return null;
   }

   public int serialize(int offset, byte[] data, EscherSerializationListener listener) {
      listener.beforeRecordSerialize(offset, this.getRecordId(), this);
      LittleEndian.putShort(data, offset, this.getOptions());
      LittleEndian.putShort(data, offset + 2, this.getRecordId());
      LittleEndian.putInt(data, offset + 4, this.getPropertiesSize());
      int pos = offset + 8;

      for (EscherProperty property : this.properties) {
         pos += property.serializeSimplePart(data, pos);
      }

      for (EscherProperty property : this.properties) {
         pos += property.serializeComplexPart(data, pos);
      }

      listener.afterRecordSerialize(pos, this.getRecordId(), pos - offset, this);
      return pos - offset;
   }

   public void sortProperties() {
      this.properties.sort(new Comparator<EscherProperty>() {
         public int compare(EscherProperty p1, EscherProperty p2) {
            short s1 = p1.getPropertyNumber();
            short s2 = p2.getPropertyNumber();
            return Short.compare(s1, s2);
         }
      });
   }

   public void setEscherProperty(EscherProperty value) {
      Iterator<EscherProperty> iterator = this.properties.iterator();

      while (iterator.hasNext()) {
         EscherProperty prop = (EscherProperty)iterator.next();
         if (prop.getId() == value.getId()) {
            iterator.remove();
         }
      }

      this.properties.add(value);
      this.sortProperties();
   }

   public void removeEscherProperty(int num) {
      Iterator<EscherProperty> iterator = this.getEscherProperties().iterator();

      while (iterator.hasNext()) {
         EscherProperty prop = (EscherProperty)iterator.next();
         if (prop.getPropertyNumber() == num) {
            iterator.remove();
         }
      }
   }

   protected Object[][] getAttributeMap() {
      List<Object> attrList = new ArrayList(this.properties.size() * 2 + 2);
      attrList.add("properties");
      attrList.add(this.properties.size());

      for (EscherProperty property : this.properties) {
         attrList.add(property.getName());
         attrList.add(property);
      }

      return new Object[][]{{"isContainer", this.isContainerRecord()}, {"numchildren", this.getChildRecords().size()}, attrList.toArray()};
   }
}
