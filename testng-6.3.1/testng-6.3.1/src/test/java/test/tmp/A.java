package test.tmp;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class A {
  @DataProvider
  public Object[][] dp() {
    String[] things = new String[] { "a", "b" };
    return new Object[][] {
        new String[][] { things }
    };
  }

  @Test(dataProvider = "dp")
  public void f(String... s) {
    
  }
}
