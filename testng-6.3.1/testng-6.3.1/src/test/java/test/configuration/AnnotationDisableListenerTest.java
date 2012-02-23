package test.configuration;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@Listeners(AnnotationDisableListener.class)
public class AnnotationDisableListenerTest {

  @BeforeTest
  public void beforeTest() {
    System.out.println("Before Test");
  }

  @Test
  public void testing() {
    System.out.println("Testing");
  }

  @AfterTest
  public void afterTest() {
    assert false;
  }
}