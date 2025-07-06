package utils;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonUtils
{

   private WebDriver driver;
   private WebDriverWait wait;
   private static final Logger logger = LoggerFactory.getLogger(CommonUtils.class);

   public CommonUtils(WebDriver driver)
   {
      this.driver = driver;
      this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
   }

   public WebDriverWait getWait(int seconds)
   {
      return new WebDriverWait(driver, Duration.ofSeconds(seconds));
   }

   public WebElement waitForElementToBeClickable(By locator)
   {
      return wait.until(ExpectedConditions.elementToBeClickable(locator));
   }

   public WebElement waitForPresenceOfElement(By locator)
   {
      return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
   }

   public void clickOnElementUsingJavaScript(WebElement element)
   {
      JavascriptExecutor js = (JavascriptExecutor) driver;
      js.executeScript("arguments[0].click();", element);
   }

   public void safeClick(WebElement element)
   {
      try
      {
         element.click();
      }
      catch (ElementClickInterceptedException e)
      {
         logger.warn("Click intercepted, using JavaScript click as fallback", e);
         clickOnElementUsingJavaScript(element);
      }
   }

   public void handleDisclaimerPopup()
   {
      try
      {
         WebElement disclaimerTitle = wait
            .until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//h3[text()='DISCLAIMER']")));
         if (disclaimerTitle.isDisplayed())
         {
            WebElement agreeButton = driver.findElement(By.xpath("//button[text()='I have read and agree to the terms']"));
            safeClick(agreeButton);
            logger.info("Clicked on 'I have read and agree to the terms'.");
         }
      }
      catch (Exception e)
      {
         logger.info("DISCLAIMER popup not visible, continuing without clicking.");
      }
   }
}
