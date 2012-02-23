package test;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;

public class ConcreteTest extends AbstractTest {
  @BeforeClass
  public void prepareSomething() {
    Assert.fail("testSomething() should be skipped");
  }
 }