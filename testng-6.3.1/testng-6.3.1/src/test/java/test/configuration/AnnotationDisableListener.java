package test.configuration;

import org.testng.IAnnotationTransformer2;
import org.testng.annotations.IConfigurationAnnotation;
import org.testng.annotations.IDataProviderAnnotation;
import org.testng.annotations.IFactoryAnnotation;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class AnnotationDisableListener implements IAnnotationTransformer2 {
  @Override
  public void transform(IConfigurationAnnotation iConfigurationAnnotation,
      Class aClass, Constructor constructor, Method method) {
    System.out.format("%s %s %s\r\n", method.getName(),
        iConfigurationAnnotation.getBeforeTest(),
        iConfigurationAnnotation.getAfterTest());

//    iConfigurationAnnotation.setEnabled(false); // this works.
  }

  @Override
  public void transform(IDataProviderAnnotation iDataProviderAnnotation,
      Method method) {
    // Default implementation
  }

  @Override
  public void transform(IFactoryAnnotation iFactoryAnnotation, Method method) {
    // Default implementation
  }

  @Override
  public void transform(ITestAnnotation iTestAnnotation, Class aClass,
      Constructor constructor, Method method) {
    // Default implementation
  }
}