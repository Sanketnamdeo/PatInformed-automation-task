package com.nebula;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.CommonUtils;
import utils.DriverFactory;

public class PatInformed
{

   WebDriver driver;
   WebDriverWait wait;
   CommonUtils utils;
   private static final Logger logger = LoggerFactory.getLogger(PatInformed.class);

   public PatInformed()
   {
      this.driver = DriverFactory.initDriver();
      this.utils = new CommonUtils(driver);
      this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
   }

   // Opens the PatInformed website
   public void openSite()
   {
      driver.get("https://patinformed.wipo.int/");
   }

   // Validates the page title after loading
   public void assertPageTitle(String expectedTitle)
   {
      try
      {
         String actualTitle = driver.getTitle().trim();
         logger.info("Asserting page title. Expected: '{}', Actual: '{}'", expectedTitle, actualTitle);
         Assert.assertEquals("Page title does not match", expectedTitle, actualTitle);
      }
      catch (Exception e)
      {
         logger.error("Title validation failed: {}", e.getMessage());
      }
   }

   // Enters the search keyword
   public void search(String keyword)
   {
      try
      {
         WebElement searchBox = utils.waitForElementToBeClickable(By.xpath("//input[@class='searchField']"));
         searchBox.clear();
         searchBox.sendKeys(keyword);
         logger.info("Searched for keyword: {}", keyword);
      }
      catch (Exception e)
      {
         logger.error("Search failed for keyword '{}': {}", keyword, e.getMessage());
      }
   }

   // Clicks on the first result dynamically
   public void openFirstResult()
   {
      try
      {
         // Handle disclaimer or cookie popups if present
         utils.handleDisclaimerPopup();
         // Wait for the results table to be present
         utils.getWait(10);
         WebElement table = utils.waitForPresenceOfElement(By.cssSelector("table.results"));
         List<WebElement> rows = table.findElements(By.cssSelector("tbody tr"));

         if (!rows.isEmpty())
         {
            rows.get(0).click();
            logger.info("Clicked on the first search result.");
         }
         else
         {
            logger.warn("No search results found.");
         }
      }
      catch (Exception e)
      {
         logger.error("Error while opening first result: {}", e.getMessage());
      }
   }

   // Extract and compare dates (filing, publication, grant) from valid results
   public void extractAndCompareDatesFromFirstAvailableResult()
   {
      try
      {
         wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("ul.results > li.result")));
      }
      catch (TimeoutException e)
      {
         logger.warn("No result items found.");
         return;
      }

      List<WebElement> results = driver.findElements(By.cssSelector("ul.results > li.result"));
      List<String> requiredLabels = Arrays.asList("filing date", "publication date", "grant date");

      Map<String, LocalDate> box1Dates = null, box2Dates = null;

      for (WebElement result : results)
      {
         Map<String, LocalDate> dates = extractDatesFromResult(result);
         if (containsAllRequiredDates(dates, requiredLabels))
         {
            if (box1Dates == null)
            {
               box1Dates = dates;
            }
            else
            {
               box2Dates = dates;
               break;
            }
         }
      }

      if (box1Dates != null && box2Dates != null)
      {
         printDatesAndDifferences(box1Dates);
      }
      else
      {
         logger.warn("Insufficient valid results with all required dates.");
      }
   }

   // Parses the date values from a result row
   private Map<String, LocalDate> extractDatesFromResult(WebElement result)
   {
      Map<String, LocalDate> dates = new LinkedHashMap<>();
      List<WebElement> rows = result.findElements(By.cssSelector("table.patentDetails tr"));

      for (WebElement row : rows)
      {
         List<WebElement> cells = row.findElements(By.tagName("td"));
         if (cells.size() < 2)
         {
            continue;
         }

         String label = cells.get(0).getText().trim().toLowerCase();
         String dateText = cells.get(1).getText().trim().split("\\(")[0].trim(); // Handle "(expected)" etc.

         if (label.contains("date") && !dateText.isEmpty())
         {
            try
            {
               LocalDate parsedDate = LocalDate.parse(dateText, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
               dates.put(label, parsedDate);
            }
            catch (DateTimeParseException e)
            {
               logger.warn("Failed to parse date '{}': {}", dateText, e.getMessage());
            }
         }
      }
      return dates;
   }

   // Ensures required date labels are present
   private boolean containsAllRequiredDates(Map<String, LocalDate> dates, List<String> requiredLabels)
   {
      Set<String> foundLabels = dates.keySet().stream().map(String::toLowerCase).collect(Collectors.toSet());
      return requiredLabels.stream().allMatch(foundLabels::contains);
   }

   // Logs date differences
   private void printDatesAndDifferences(Map<String, LocalDate> dates)
   {
      LocalDate publication = dates.get("publication date");
      LocalDate grant = dates.get("grant date");
      LocalDate filing = dates.get("filing date");

      logger.info("Publication date: {}", publication);
      logger.info("Grant date: {}", grant);
      logger.info("Filing date: {}", filing);

      logger.info("Difference between Publication and Grant: {} days", Math.abs(ChronoUnit.DAYS.between(publication, grant)));
      logger.info("Difference between Publication and Filing: {} days", Math.abs(ChronoUnit.DAYS.between(publication, filing)));
      logger.info("Difference between Grant and Filing: {} days", Math.abs(ChronoUnit.DAYS.between(grant, filing)));
   }

   // Close the browser cleanly
   public void closeBrowser()
   {
      DriverFactory.quitDriver();
   }

   // Main method to allow execution via CLI with arguments
   public static void main(String[] args)
   {
      PatInformed pat = new PatInformed();

      try
      {
         if (args.length == 0)
         {
            throw new IllegalArgumentException("Please provide at least one keyword to search.");
         }

         pat.openSite();
         pat.assertPageTitle("Pat-INFORMED");

         for (String keyword : args)
         {
            pat.search(keyword);
            pat.openFirstResult();
            pat.extractAndCompareDatesFromFirstAvailableResult();
         }

      }
      catch (Exception e)
      {
         logger.error("Unhandled exception occurred: {}", e.getMessage());
      }
      finally
      {
         pat.closeBrowser();
         // Attempt to forcefully exit and kill lingering threads
         System.exit(0);
      }
   }

}
