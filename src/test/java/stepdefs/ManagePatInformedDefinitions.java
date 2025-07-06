package stepdefs;

import com.nebula.PatInformed;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class ManagePatInformedDefinitions
{
   PatInformed pat = new PatInformed();

   @Given("I open the Pat-INFORMED site")
   public void i_open_the_pat_informed_site()
   {
      pat.openSite();
   }

   @Then("I assert the title on Page is {string}")
   public void iAssertTheTitleOnPageIs(String expectedTitle)
   {
      pat.assertPageTitle(expectedTitle);
   }

   @When("I search for {string}")
   public void i_search_for(String keyword)
   {
      pat.search(keyword);
   }

   @Then("I verify the label with {string} is visible under {string} column")
   public void verifyLabelUnderColumn(String expectedLabel, String columnName)
   {
      pat.verifyLabelUnderColumn(expectedLabel, columnName);
   }

   @When("I open the first {string} result")
   public void i_open_the_first_patent_result(String expectedLabel)
   {
      pat.openFirstResultUnderColumn(expectedLabel);
   }

   @When("I extract and compare the patent dates from the first result with valid dates")
   public void extractAndComparePatentDatesFromFirstValidResult()
   {
      pat.extractAndCompareDatesFromFirstAvailableResult();
   }

   @Then("I close the browser")
   public void iCloseTheBrowser()
   {
      pat.closeBrowser();
   }

}
