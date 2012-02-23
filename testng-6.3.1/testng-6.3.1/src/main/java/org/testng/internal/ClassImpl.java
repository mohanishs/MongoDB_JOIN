package org.testng.internal;

import com.google.inject.Injector;
import com.google.inject.Module;

import org.testng.IClass;
import org.testng.IModuleFactory;
import org.testng.ITest;
import org.testng.ITestContext;
import org.testng.ITestObjectFactory;
import org.testng.TestNGException;
import org.testng.annotations.Guice;
import org.testng.collections.Lists;
import org.testng.internal.annotations.AnnotationHelper;
import org.testng.internal.annotations.IAnnotationFinder;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlTest;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of an IClass.
 *
 * @author <a href="mailto:cedric@beust.com">Cedric Beust</a>
 */
public class ClassImpl implements IClass {
  private static final long serialVersionUID = 1118178273317520344L;
  transient private Class m_class = null;
  transient private Object m_defaultInstance = null;
  private XmlTest m_xmlTest = null;
  transient private IAnnotationFinder m_annotationFinder = null;
  transient private List<Object> m_instances = Lists.newArrayList();
  transient private Map<Class, IClass> m_classes = null;
  private int m_instanceCount;
  private long[] m_instanceHashCodes;
  private transient Object m_instance;
  private ITestObjectFactory m_objectFactory;
  private String m_testName = null;
  private XmlClass m_xmlClass;
  private ITestContext m_testContext;

  public ClassImpl(ITestContext context, Class cls, XmlClass xmlClass, Object instance,
      Map<Class, IClass> classes, XmlTest xmlTest, IAnnotationFinder annotationFinder,
      ITestObjectFactory objectFactory) {
    m_testContext = context;
    m_class = cls;
    m_classes = classes;
    m_xmlClass = xmlClass;
    m_xmlTest = xmlTest;
    m_annotationFinder = annotationFinder;
    m_instance = instance;
    m_objectFactory = objectFactory;
    if (instance instanceof ITest) {
      m_testName = ((ITest) instance).getTestName();
    }
  }

  private static void ppp(String s) {
    System.out.println("[ClassImpl] " + s);
  }

  @Override
  public String getTestName() {
    return m_testName;
  }

  @Override
  public String getName() {
    return m_class.getName();
  }

  @Override
  public Class getRealClass() {
    return m_class;
  }

  @Override
  public int getInstanceCount() {
    return m_instanceCount;
  }

  @Override
  public long[] getInstanceHashCodes() {
    return m_instanceHashCodes;
  }

  @Override
  public XmlTest getXmlTest() {
    return m_xmlTest;
  }

  @Override
  public XmlClass getXmlClass() {
    return m_xmlClass;
  }

  private Object getDefaultInstance() {
    if (m_defaultInstance == null) {
      if (m_instance != null) {
        m_defaultInstance = m_instance;
      } else {
        Object instance = getInstanceFromGuice();

        if (instance != null) {
          m_defaultInstance = instance;
        } else {
          m_defaultInstance =
              ClassHelper.createInstance(m_class, m_classes, m_xmlTest,
                  m_annotationFinder, m_objectFactory);
        }
      }
    }

    return m_defaultInstance;
  }

  /**
   * @return an instance from Guice if @Test(guiceModule) attribute was found, null otherwise
   */
  @SuppressWarnings("unchecked")
  private Object getInstanceFromGuice() {
    Annotation annotation = AnnotationHelper.findAnnotationSuperClasses(Guice.class, m_class);
    if (annotation == null) return null;

    Guice guice = (Guice) annotation;
    Module[] modules = getModules(guice, m_class);

    if (modules.length == 0) {
      throw new TestNGException("Couldn't find any Guice module for class " + m_class);
    }

    List<Module> moduleInstances = new ArrayList<Module>();
    for (Module module : modules) {
      moduleInstances.add(module);
    }

    // Reuse the previous injector, if any
    Injector injector = m_testContext.getInjector(moduleInstances);
    if (injector == null) {
      injector = com.google.inject.Guice.createInjector(moduleInstances);
      m_testContext.addInjector(moduleInstances, injector);
    }
    return injector.getInstance(m_class);
  }

  private Module[] getModules(Guice guice, Class<?> testClass) {
    List<Module> result = Lists.newArrayList();
    for (Class<? extends Module> moduleClass : guice.modules()) {
      try {
        List<Module> modules = m_testContext.getGuiceModules(moduleClass);
        if (modules != null && modules.size() > 0) {
          result.addAll(modules);
        } else {
          Module instance = moduleClass.newInstance();
          result.add(instance);
          m_testContext.addGuiceModule(moduleClass, instance);
        }
      } catch (InstantiationException e) {
        throw new TestNGException(e);
      } catch (IllegalAccessException e) {
        throw new TestNGException(e);
      }
    }
    Class<? extends IModuleFactory> factory = guice.moduleFactory();
    if (factory != IModuleFactory.class) {
      try {
        IModuleFactory factoryInstance = factory.newInstance();
        Module moduleClass = factoryInstance.createModule(m_testContext, testClass);
        if (moduleClass != null) {
          result.add(moduleClass);
        }
      } catch (InstantiationException e) {
        throw new TestNGException(e);
      } catch (IllegalAccessException e) {
        throw new TestNGException(e);
      }
    }

    return result.toArray(new Module[result.size()]);
  }

  @Override
  public Object[] getInstances(boolean create) {
    Object[] result = {};

    if (m_xmlTest.isJUnit()) {
      if (create) {
        result = new Object[] { ClassHelper.createInstance(m_class, m_classes,
            m_xmlTest, m_annotationFinder, m_objectFactory) };
      }
    } else {
      result = new Object[] { getDefaultInstance() };
    }
    if (m_instances.size() > 0) {
      result = m_instances.toArray(new Object[m_instances.size()]);
    }

    m_instanceCount = m_instances.size();
    m_instanceHashCodes = new long[m_instanceCount];
    for (int i = 0; i < m_instanceCount; i++) {
      m_instanceHashCodes[i] = m_instances.get(i).hashCode();
    }
    return result;
  }

  @Override
  public String toString() {
    return "[ClassImpl " + m_class.getName() + "]";
  }

  @Override
  public void addInstance(Object instance) {
    m_instances.add(instance);
  }

}
