package com.nebula;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

   public void extractAndCompareDatesAcrossResults()
   {
      try
      {
         // Wait until at least one result item is present
         wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("ul.results > li.result")));
      }
      catch (TimeoutException e)
      {
         logger.warn("No result items found on the page.");
         return;
      }

      // List of required date labels to look for
      List<String> requiredLabels = Arrays.asList("filing date", "grant date", "publication date");

      // This map will store the first occurrence of each required date
      Map<String, LocalDate> collectedDates = new LinkedHashMap<>();

      List<WebElement> results = driver.findElements(By.cssSelector("ul.results > li.result"));

      // Loop through each result box
      for (WebElement result : results)
      {
         // Extract all date-related labels and values from this result box
         Map<String, LocalDate> dates = extractDatesFromResult(result);

         // Go through each extracted date and collect if it's required and not already collected
         for (Map.Entry<String, LocalDate> entry : dates.entrySet())
         {
            String label = entry.getKey().toLowerCase();
            if (requiredLabels.contains(label) && !collectedDates.containsKey(label))
            {
               collectedDates.put(label, entry.getValue());
            }
         }

         // If all required labels have been found, break the loop early
         if (collectedDates.keySet().containsAll(requiredLabels))
         {
            logger.info("All required dates found. Stopping search.");
            break;
         }
      }

      // Identify any missing required labels
      List<String> missingLabels = new ArrayList<>();
      for (String label : requiredLabels)
      {
         if (!collectedDates.containsKey(label))
         {
            missingLabels.add(label);
         }
      }

      // Print the available dates and their differences
      if (!collectedDates.isEmpty())
      {
         printAvailableDatesAndDifferences(collectedDates);
      }

      // Log any missing date labels
      if (!missingLabels.isEmpty())
      {
         logger.warn("Missing required date(s): {}", String.join(", ", missingLabels));
      }

      // If no dates were found at all
      if (collectedDates.isEmpty())
      {
         logger.warn("None of the required dates were found in any result.");
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
         String dateText = cells.get(1).getText().trim().split("\\(")[0].trim(); // Remove any timezone or additional text

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

   private void printAvailableDatesAndDifferences(Map<String, LocalDate> dates)
   {
      LocalDate filing = dates.get("filing date");
      LocalDate grant = dates.get("grant date");
      LocalDate publication = dates.get("publication date");

      if (publication != null)
      {
         logger.info("Publication date: {}", publication);
      }
      if (grant != null)
      {
         logger.info("Grant date: {}", grant);
      }
      if (filing != null)
      {
         logger.info("Filing date: {}", filing);
      }

      if (publication != null && grant != null)
      {
         logger.info("Difference between Publication date and Grant date: {} days", Math.abs(ChronoUnit.DAYS.between(publication, grant)));
      }

      if (publication != null && filing != null)
      {
         logger.info("Difference between Publication date and Filing date: {} days", Math.abs(ChronoUnit.DAYS.between(publication, filing)));
      }

      if (grant != null && filing != null)
      {
         logger.info("Difference between Grant date and Filing date: {} days", Math.abs(ChronoUnit.DAYS.between(grant, filing)));
      }
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
            pat.extractAndCompareDatesAcrossResults();
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
