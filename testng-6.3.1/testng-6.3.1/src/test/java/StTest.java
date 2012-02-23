import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Test;

public class StTest {

  @Test
  public void y() {
    // empty
  }

  @AfterMethod
  public void afterMethod() {
    System.out.println("AM");
  }

  @AfterSuite
  public void afterSuite() {
    System.out.println("AS");
  }

  @AfterGroups
  public void afterGroups() {
    System.out.println("AG");
  }

  @AfterClass
  public void afterClass() {
    System.out.println("AC");
  }
}