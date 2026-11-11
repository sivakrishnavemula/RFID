package org.apache.poi;

public class Version {
   private static final String VERSION_STRING = "4.0.0";
   private static final String RELEASE_DATE = "20180907";

   public static String getVersion() {
      return "4.0.0";
   }

   public static String getReleaseDate() {
      return "20180907";
   }

   public static String getProduct() {
      return "POI";
   }

   public static String getImplementationLanguage() {
      return "Java";
   }

   public static void main(String[] args) {
      System.out.println("Apache " + getProduct() + " " + getVersion() + " (" + getReleaseDate() + ")");
   }
}
