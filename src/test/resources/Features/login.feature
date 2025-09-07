Feature: User Login Functionality

  Background:
    Given I navigate to the login page
    Then the login form should be visible

  @accessibility @Login
  Scenario: Login Form Accessibility
    Given I am on the login page
    Then the username field should have proper label
    And the password field should have proper label
    And the login button should be accessible

  @smoke @positive @Login
  Scenario Outline: Successful Login with Valid Credentials
    When I enter valid "<username>" and "<password>"
    And I click the login button
    Then I should be redirected to the dashboard

    Examples:
      | username | password |
      | sdpadmin | test     |
      | sdpsp    | test     |

  @negative @validation @Login
  Scenario Outline: Failed Login with Invalid Credentials
    When I enter invalid "<username>" and "<password>"
    And I click the login button
    Then I should see an error message
    And I should remain on the login page

    Examples:
      | username | password | description        |
      | sdpsp    | sdpsp    | wrong password     |
      | sdpsp    | passwd   | incorrect password |
      | test     | test123  | invalid username   |
      | test     | sdpsp    | both invalid       |
      | user     | test     | non-existent user  |

  @negative @validation @Login
  Scenario: Login with Empty Username
    When I clear the username field
    And I enter valid password
    And I click the login button
    Then I should see an error message
    And I should remain on the login page

  @negative @validation @Login
  Scenario: Login with Empty Password
    When I enter valid username
    And I clear the password field
    And I click the login button
    Then I should see an error message
    And I should remain on the login page

  @negative @validation @Login
  Scenario: Login with Both Fields Empty
    When I clear the username field
    And I clear the password field
    And I click the login button
    Then I should see an error message
    And I should remain on the login page

  @negative @validation @Login
  Scenario: Login with Special Characters in Username
    When I enter invalid "user@123" and "test"
    And I click the login button
    Then I should see an error message
    And I should remain on the login page

  @negative @validation @Login
  Scenario: Login with Very Long Username
    When I enter invalid "verylongusername123456789012345678901234567890" and "test"
    And I click the login button
    Then I should see an error message
    And I should remain on the login page

  @negative @validation @Login
  Scenario: Login with SQL Injection Attempt
    When I enter invalid "'; DROP TABLE users; --" and "test"
    And I click the login button
    Then I should see an error message
    And I should remain on the login page

  @negative @validation @Login
  Scenario: Login with XSS Attempt
    When I enter invalid "<script>alert('xss')</script>" and "test"
    And I click the login button
    Then I should see an error message
    And I should remain on the login page

  @negative @validation @Login
  Scenario: Login with Spaces Only
    When I enter invalid "   " and "   "
    And I click the login button
    Then I should see an error message
    And I should remain on the login page

  @negative @validation @Login
  Scenario: Login with Null Values
    When I enter invalid "" and ""
    And I click the login button
    Then I should see an error message
    And I should remain on the login page

  @performance @Login
  Scenario: Login Page Load Performance
    Given I navigate to the login page
    And the page should load within acceptable time

  @security @Login
  Scenario: Password Field Security
    Given I am on the login page
    When I enter valid "sdpsp" and "test"
    Then the password field should mask the input
    And password should not be visible in page source

  @browser @cross-browser @Login
  Scenario: Cross Browser Login Compatibility
    Given I navigate to the login page using <browser>
    When I enter valid "sdpsp" and "test"
    And I click the login button
    Then I should be redirected to the dashboard

    Examples:
      | browser |
      | chrome  |
      | firefox |
      | edge    |

  @session @Security
  Scenario: Session Management After Login
    Given I am logged in successfully
    Then the session should be properly established
    And I should not be able to access login page again

  @logout @security @Login
  Scenario: Logout Functionality
    Given I am logged in successfully
    When I click the logout button
    Then I should be redirected to the login page
    And the session should be cleared
