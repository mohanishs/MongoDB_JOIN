import org.testng.IConfigurationListener;
import org.testng.ITestResult;
import org.testng.internal.Utils;

public class Tnl2 implements IConfigurationListener {

  @Override
  public void onConfigurationSuccess(ITestResult itr) {
    System.out.println(Utils.detailedMethodName(itr.getMethod(), true));
  }

  @Override
  public void onConfigurationFailure(ITestResult itr) {
  }

  @Override
  public void onConfigurationSkip(ITestResult itr) {
  }
}