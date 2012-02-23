package test.dataprovider;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class DisplayTextTest {
  @DataProvider
  public Object[][] testStrings() {
    return new Object[][] {
        new Object[] { new String[] { "one" } },
        new Object[] { new String[] { "two" } },
        new Object[] { new String[] { "three", "four" } } };
  }

  @Test(dataProvider = "testStrings")
  public void badTest(String... strings) {
    for (String item : strings) {
      System.out.println("An item is \"" + item + "\"");
    }
  }

  @Test(dataProvider = "testStrings")
  public void goodTest(Object... strings) {
    for (Object item : strings) {
      System.out.println("An item is \"" + item + "\"");
    }
  }

  public static void main(String[] args) {
    new DisplayTextTest().badTest(new String[] { "one" });
  }
}