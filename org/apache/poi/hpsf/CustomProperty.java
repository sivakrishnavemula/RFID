package org.apache.poi.hpsf;

public class CustomProperty extends Property {
   private String name;

   public CustomProperty() {
      this.name = null;
   }

   public CustomProperty(Property property) {
      this(property, null);
   }

   public CustomProperty(Property property, String name) {
      super(property);
      this.name = name;
   }

   public String getName() {
      return this.name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public boolean equalsContents(Object o) {
      CustomProperty c = (CustomProperty)o;
      String name1 = c.getName();
      String name2 = this.getName();
      boolean equalNames = true;
      if (name1 == null) {
         equalNames = name2 == null;
      } else {
         equalNames = name1.equals(name2);
      }

      return equalNames && c.getID() == this.getID() && c.getType() == this.getType() && c.getValue().equals(this.getValue());
   }

   public int hashCode() {
      return (int)this.getID();
   }

   public boolean equals(Object o) {
      return o instanceof CustomProperty ? this.equalsContents(o) : false;
   }
}
