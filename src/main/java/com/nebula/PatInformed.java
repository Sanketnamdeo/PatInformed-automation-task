package com.nebula;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
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
   Map<String, LocalDate> dates = new HashMap<>();
   private static final Logger logger = LoggerFactory.getLogger(PatInformed.class);

   public PatInformed()
   {
      this.driver = DriverFactory.initDriver();
      this.utils = new CommonUtils(driver);
   }

   public void openSite()
   {
      driver.get("https://patinformed.wipo.int/");
   }

   public void assertPageTitle(String expectedTitle)
   {
      String actualTitle = driver.getTitle().trim();
      logger.info("Asserting page title. Expected: '{}', Actual: '{}'", expectedTitle, actualTitle);
      Assert.assertEquals("Page title does not match", expectedTitle, actualTitle);
   }

   public void search(String keyword)
   {
      WebElement searchBox = utils.waitForElementToBeClickable(By.xpath("//input[@class='searchField']"));
      searchBox.sendKeys(keyword);
   }

   /**
    * Verifies that the expected label appears under a given table column.
    */
   public void verifyLabelUnderColumn(String expectedLabel, String columnName)
   {
      WebElement table = utils.waitForPresenceOfElement(By.cssSelector("table.results"));
      int columnIndex = getColumnIndex(table, columnName);
      List<WebElement> rows = table.findElements(By.cssSelector("tbody tr"));

      boolean found = rows
         .stream()
         .map(row -> row.findElements(By.tagName("td")))
         .filter(cells -> cells.size() > columnIndex)
         .map(cells -> cells.get(columnIndex).getText().trim())
         .anyMatch(text -> text.equalsIgnoreCase(expectedLabel));

      if (!found)
      {
         throw new AssertionError("Label '" + expectedLabel + "' not found under column '" + columnName + "'");
      }
   }

   /**
    * Clicks on the first available row under the specified column name.
    */
   public void openFirstResultUnderColumn(String columnName)
   {
      utils.handleDisclaimerPopup();
      utils.getWait(10);
      logger.info("Opening first result under column: {}", columnName);

      WebElement table = utils.waitForPresenceOfElement(By.cssSelector("table.results"));
      int columnIndex = getColumnIndex(table, columnName);
      List<WebElement> rows = table.findElements(By.cssSelector("tbody tr"));

      if (rows.isEmpty())
      {
         throw new AssertionError("No rows found in table");
      }

      List<WebElement> cells = rows.get(0).findElements(By.tagName("td"));
      if (cells.size() > columnIndex)
      {
         utils.safeClick(cells.get(columnIndex));
      }
      else
      {
         throw new AssertionError("No cell found at column index " + columnIndex);
      }
   }

   /**
    * Extracts dates (filing, publication, grant) from the first two valid search results and prints their differences.
    */
   public void extractAndCompareDatesFromFirstAvailableResult()
   {
      WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
      try
      {
         wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("ul.results > li.result")));
      }
      catch (TimeoutException e)
      {
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
   }

   /**
    * Extracts date-related fields from a given search result item.
    */
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
         String dateText = cells.get(1).getText().trim().split("\\(")[0].trim(); // Remove extra notes if any

         if (label.contains("date") && !dateText.isEmpty())
         {
            try
            {
               LocalDate parsedDate = LocalDate.parse(dateText, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
               dates.put(label, parsedDate);
            }
            catch (DateTimeParseException e)
            {

            }
         }
      }
      return dates;
   }

   /**
    * Checks if the map contains all required date labels.
    */
   private boolean containsAllRequiredDates(Map<String, LocalDate> dates, List<String> requiredLabels)
   {
      Set<String> foundLabels = dates.keySet().stream().map(String::toLowerCase).collect(Collectors.toSet());
      return requiredLabels.stream().allMatch(foundLabels::contains);
   }

   /**
    * Logs all extracted dates and their differences.
    */
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

   /**
    * Retrieves the column index from the header row based on the column name.
    */
   private int getColumnIndex(WebElement table, String columnName)
   {
      List<WebElement> headers = table.findElements(By.cssSelector("thead th"));
      for (int i = 0; i < headers.size(); i++)
      {
         if (headers.get(i).getText().trim().equalsIgnoreCase(columnName))
         {
            return i;
         }
      }
      throw new AssertionError("Column '" + columnName + "' not found");
   }

   public void closeBrowser()
   {
      DriverFactory.quitDriver();
   }
}
