package org.testng.internal;

import org.testng.IConfigureCallBack;
import org.testng.IHookCallBack;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.TestNGException;
import org.testng.collections.Lists;
import org.testng.internal.annotations.IAnnotationFinder;
import org.testng.internal.thread.IExecutor;
import org.testng.internal.thread.IFutureResult;
import org.testng.internal.thread.ThreadExecutionException;
import org.testng.internal.thread.ThreadTimeoutException;
import org.testng.internal.thread.ThreadUtil;
import org.testng.xml.XmlSuite;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.List;

/**
 * Collections of helper methods to help deal with invocation of TestNG methods
 *
 * @author Cedric Beust <cedric@beust.com>
 * @author nullin <nalin.makar * gmail.com>
 *
 */
public class MethodInvocationHelper {

  protected static Object invokeMethod(Method thisMethod, Object instance, Object[] parameters)
      throws InvocationTargetException, IllegalAccessException {
    Object result = null;
    Utils.checkInstanceOrStatic(instance, thisMethod);

    // TESTNG-326, allow IObjectFactory to load from non-standard classloader
    // If the instance has a different classloader, its class won't match the
    // method's class
    if (instance == null || !thisMethod.getDeclaringClass().isAssignableFrom(instance.getClass())) {
      // for some reason, we can't call this method on this class
      // is it static?
      boolean isStatic = Modifier.isStatic(thisMethod.getModifiers());
      if (!isStatic) {
        // not static, so grab a method with the same name and signature in this case
        Class<?> clazz = instance.getClass();
        try {
          thisMethod = clazz.getMethod(thisMethod.getName(), thisMethod.getParameterTypes());
        } catch (Exception e) {
          // ignore, the method may be private
          boolean found = false;
          for (; clazz != null; clazz = clazz.getSuperclass()) {
            try {
              thisMethod = clazz.getDeclaredMethod(thisMethod.getName(),
                  thisMethod.getParameterTypes());
              found = true;
              break;
            } catch (Exception e2) {
            }
          }
          if (!found) {
            // should we assert here? Or just allow it to fail on invocation?
            if (thisMethod.getDeclaringClass().getName().equals(instance.getClass().getName())) {
              throw new RuntimeException("Can't invoke method " + thisMethod
                  + ", probably due to classloader mismatch");
            }
            throw new RuntimeException("Can't invoke method " + thisMethod
                + " on this instance of " + instance.getClass() + " due to class mismatch");
          }
        }
      }
    }

    synchronized(thisMethod) {
      if (! Modifier.isPublic(thisMethod.getModifiers())) {
        thisMethod.setAccessible(true);
      }
    }
    return thisMethod.invoke(instance, parameters);
  }

  protected static Iterator<Object[]> invokeDataProvider(Object instance, Method dataProvider,
      ITestNGMethod method, ITestContext testContext, Object fedInstance,
      IAnnotationFinder annotationFinder) {
    Iterator<Object[]> result = null;
    Method testMethod = method.getMethod();

    // If it returns an Object[][], convert it to an Iterable<Object[]>
    try {
      List<Object> lParameters = Lists.newArrayList();

      // Go through all the parameters declared on this Data Provider and
      // make sure we have at most one Method and one ITestContext.
      // Anything else is an error
      Class<?>[] parameterTypes = dataProvider.getParameterTypes();
      if (parameterTypes.length > 2) {
        throw new TestNGException("DataProvider " + dataProvider
            + " cannot have more than two parameters");
      }

      int i = 0;
      for (Class<?> cls : parameterTypes) {
        boolean isTestInstance = annotationFinder.hasTestInstance(dataProvider, i++);
        if (cls.equals(Method.class)) {
          lParameters.add(testMethod);
        } else if (cls.equals(ITestContext.class)) {
          lParameters.add(testContext);
        } else if (isTestInstance) {
          lParameters.add(fedInstance);
        }
      }
      Object[] parameters = lParameters.toArray(new Object[lParameters.size()]);

      Class<?> returnType = dataProvider.getReturnType();
      if (Object[][].class.isAssignableFrom(returnType)) {
        Object[][] oResult = (Object[][]) invokeMethod(dataProvider, instance, parameters);
        method.setParameterInvocationCount(oResult.length);
        result = MethodHelper.createArrayIterator(oResult);
      } else if (Iterator.class.isAssignableFrom(returnType)) {
        // Already an Iterable<Object[]>, assign it directly
        result = (Iterator<Object[]>) invokeMethod(dataProvider, instance, parameters);
      } else {
        throw new TestNGException("Data Provider " + dataProvider + " must return"
            + " either Object[][] or Iterator<Object>[], not " + returnType);
      }
    } catch (InvocationTargetException e) {
      // Don't throw TestNGException here or this test won't be reported as a
      // skip or failure
      throw new RuntimeException(e.getCause());
    } catch (IllegalAccessException e) {
      // Don't throw TestNGException here or this test won't be reported as a
      // skip or failure
      throw new RuntimeException(e.getCause());
    }

    return result;
  }

  /**
   * Invokes the <code>run</code> method of the <code>IHookable</code>.
   *
   * @param testInstance
   *          the instance to invoke the method in
   * @param parameters
   *          the parameters to be passed to <code>IHookCallBack</code>
   * @param thisMethod
   *          the method to be invoked through the <code>IHookCallBack</code>
   * @param testResult
   *          the current <code>ITestResult</code> passed to
   *          <code>IHookable.run</code>
   * @throws NoSuchMethodException
   * @throws IllegalAccessException
   * @throws InvocationTargetException
   * @throws Throwable
   *           thrown if the reflective call to
   *           <tt>thisMethod</code> results in an exception
   */
  protected static void invokeHookable(final Object testInstance, final Object[] parameters,
      Object hookableInstance, final Method thisMethod, TestResult testResult) throws Throwable {
    Method runMethod = hookableInstance.getClass().getMethod("run",
        new Class[] { IHookCallBack.class, ITestResult.class });
    final Throwable[] error = new Throwable[1];

    IHookCallBack callback = new IHookCallBack() {
      @Override
      public void runTestMethod(ITestResult tr) {
        try {
          invokeMethod(thisMethod, testInstance, parameters);
        } catch (Throwable t) {
          error[0] = t;
          tr.setThrowable(t); // make Throwable available to IHookable
        }
      }

      @Override
      public Object[] getParameters() {
        return parameters;
      }
    };
    runMethod.invoke(hookableInstance, new Object[] { callback, testResult });
    if (error[0] != null) {
      throw error[0];
    }
  }

  /**
   * Invokes a method on a separate thread in order to allow us to timeout the
   * invocation. It uses as implementation an <code>Executor</code> and a
   * <code>CountDownLatch</code>.
   */
  protected static void invokeWithTimeout(ITestNGMethod tm, Object instance,
      Object[] parameterValues, ITestResult testResult)
      throws InterruptedException, ThreadExecutionException {
    if (ThreadUtil.isTestNGThread()) {
      // We are already running in our own executor, don't create another one (or we will
      // lose the time out of the enclosing executor).
      invokeWithTimeoutWithNoExecutor(tm, instance, parameterValues, testResult);
    } else {
      invokeWithTimeoutWithNewExecutor(tm, instance, parameterValues, testResult);
    }
  }

  private static void invokeWithTimeoutWithNoExecutor(ITestNGMethod tm, Object instance,
      Object[] parameterValues, ITestResult testResult) {

    InvokeMethodRunnable imr = new InvokeMethodRunnable(tm, instance, parameterValues);
    try {
      imr.run();
      testResult.setStatus(ITestResult.SUCCESS);
    } catch (Exception ex) {
      testResult.setThrowable(ex.getCause());
      testResult.setStatus(ITestResult.FAILURE);
    }
  }

  private static void invokeWithTimeoutWithNewExecutor(ITestNGMethod tm, Object instance,
      Object[] parameterValues, ITestResult testResult)
      throws InterruptedException, ThreadExecutionException {
    IExecutor exec = ThreadUtil.createExecutor(1, tm.getMethod().getName());

    InvokeMethodRunnable imr = new InvokeMethodRunnable(tm, instance, parameterValues);
    IFutureResult future = exec.submitRunnable(imr);
    exec.shutdown();
    long realTimeOut = MethodHelper.calculateTimeOut(tm);
    boolean finished = exec.awaitTermination(realTimeOut);

    if (!finished) {
      exec.stopNow();
      ThreadTimeoutException exception = new ThreadTimeoutException("Method "
          + tm.getClass().getName() + "." + tm.getMethodName() + "()"
          + " didn't finish within the time-out " + realTimeOut);
      exception.setStackTrace(exec.getStackTraces()[0]);
      testResult.setThrowable(exception);
      testResult.setStatus(ITestResult.FAILURE);
    } else {
      Utils.log("Invoker " + Thread.currentThread().hashCode(), 3, "Method " + tm.getMethod()
          + " completed within the time-out " + tm.getTimeOut());

      // We don't need the result from the future but invoking get() on it
      // will trigger the exception that was thrown, if any
      future.get();
      // done.await();

      testResult.setStatus(ITestResult.SUCCESS); // if no exception till here
                                                 // than SUCCESS
    }
  }

  protected static void invokeConfigurable(final Object instance, final Object[] parameters,
      Object configurableInstance, final Method thisMethod, ITestResult testResult)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, Throwable {
    Method runMethod = configurableInstance.getClass().getMethod("run",
        new Class[] { IConfigureCallBack.class, ITestResult.class });
    final Throwable[] error = new Throwable[1];

    IConfigureCallBack callback = new IConfigureCallBack() {
      @Override
      public void runConfigurationMethod(ITestResult tr) {
        try {
          invokeMethod(thisMethod, instance, parameters);
        } catch (Throwable t) {
          error[0] = t;
          tr.setThrowable(t); // make Throwable available to IConfigurable
        }
      }

      @Override
      public Object[] getParameters() {
        return parameters;
      }
    };
    runMethod.invoke(configurableInstance, new Object[] { callback, testResult });
    if (error[0] != null) {
      throw error[0];
    }
  }

}