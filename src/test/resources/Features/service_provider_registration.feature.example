Feature: Service Provider Registration
  As an administrator, I want to register a new service provider
  so that they can start using the platform.

  Background:
    Given I am logged in as an administrator
    And I am on the service provider registration page

  @Registration
  Scenario: Successfully register a new service provider with all valid details
    When I fill the "Service Provider ID" field with "54689515"
    And I fill the "Company name" field with "BlueSky Telecom"
    And I fill the "Address" field with "100 Market Street"
    And I fill the "Description" field with "A new and promising telecom partner"
    And I fill the "White Listed Users" field with "98765432"
    And I select "SdpSp" from the "SP users" list
    And I select "SdpMktg" from the "Marketing Users" list
    And I select the "SMS" resource
    And I click the "Submit" button
    Then I should see the success message "Service Provider registered successfully"

  @Registration
  Scenario Outline: Validate the 'Service Provider ID' field
    When I fill the "Service Provider ID" field with "<VALUE>"
    And I click the "Submit" button
    Then I should see the error message "<ERROR_MESSAGE>"

    Examples:
      | VALUE    | ERROR_MESSAGE                                        | Description          |
      |          | Service provider Id required                         | Mandatory check      |
      |  1234567 | Please enter 8 digit number for Service Provider Id. | Length check (short) |
      | abcdefgh | Please enter 8 digit number for Service Provider Id. | Format check (text)  |

  @Registration
  Scenario Outline: Validate the 'Company name' field
    When I fill the "Company name" field with "<VALUE>"
    And I click the "Submit" button
    Then I should see the error message "<ERROR_MESSAGE>"

    Examples:
      | VALUE                                             | ERROR_MESSAGE                                  | Description      |
      |                                                   | Company Name required                          | Mandatory check  |
      | A Very Long Company Name That Exceeds Fifty Chars | Company Name should be less than 50 characters | Max length check |

  @Registration
  Scenario: Validate the 'SP users' field
    When I fill all mandatory fields for a valid registration
    But I do not select any user from the "SP users" list
    And I click the "Submit" button
    Then I should see the error message "Select atleast one User Name."

  @Registration
  Scenario: Validate the 'Marketing Users' field
    When I fill all mandatory fields for a valid registration
    But I do not select any user from the "Marketing Users" list
    And I click the "Submit" button
    Then I should see the error message "Allowed Marketing Users required."

  @Registration
  Scenario: Validate the 'Resources' field
    When I fill all mandatory fields for a valid registration
    But I do not select any "Resources"
    And I click the "Submit" button
    Then I should see the error message "Select atleast one list resource."
