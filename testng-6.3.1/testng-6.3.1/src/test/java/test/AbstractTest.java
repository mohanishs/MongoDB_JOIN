package test;

import org.testng.Assert;
import org.testng.annotations.Test;

public abstract class AbstractTest {
  @Test
  public void testSomething() {
    Assert.assertEquals(1, 1);
  }
 }
