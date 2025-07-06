Feature: Validate patent dates on Pat-INFORMED

  Scenario: Extract and calculate patent date differences
    Given I open the Pat-INFORMED site
    Then I assert the title on Page is "Pat-INFORMED"
    When I search for "paracetamol"
    Then I verify the label with "paracetamol" is visible under "INN" column 
    And I open the first "PATENTS" result
    When I extract and compare the patent dates from the first result with valid dates
    Then I close the browser


