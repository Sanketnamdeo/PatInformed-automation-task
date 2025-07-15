package utils;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import io.github.bonigarcia.wdm.WebDriverManager;

public class DriverFactory
{
   private static WebDriver driver;

   public static WebDriver initDriver()
   {
      if (driver == null)
      {
         WebDriverManager.chromedriver().setup();
         ChromeOptions options = new ChromeOptions();
//         options.addArguments("--headless=new");
         options.addArguments("--incognito", "--start-maximized");
         driver = new ChromeDriver(options);
      }
      return driver;
   }

   public static void quitDriver()
   {
      if (driver != null)
      {
         try
         {
            driver.quit(); // Fully shuts down Chrome and background threads
         }
         catch (Exception e)
         {
            System.err.println("Error quitting WebDriver: " + e.getMessage());
         }
         finally
         {
            driver = null;
         }
      }
   }
}
