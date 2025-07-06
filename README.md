This project automates search functionality on the Pat-INFORMED website using Java, Selenium WebDriver, and Maven. It supports execution via Java CLI and Maven.

Prerequisites

Java 17 or later

Maven 3.6 or later

Google Chrome browser installed

Internet connection to download dependencies


1. Run via Maven

Compile the project:    mvn clean compile

Execute with arguments (e.g., search keywords):   mvn clean compile exec:java -Dexec.args="paracetamol"

2. Run as Java Application in IDE
   
Set Program arguments to:
paracetamol
Then run the application.
