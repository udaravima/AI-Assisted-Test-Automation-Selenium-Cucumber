# M1 Test Automation Framework

## Overview
This is an enhanced Selenium WebDriver test automation framework for the M1 application, built with Java, Cucumber, and Maven. The framework includes comprehensive test coverage for login functionality, user management, dashboard operations, security testing, and performance testing.

## Features

## Project Structure

```
m1/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/sdp/m1/
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       ├── java/
│       │   └── com/sdp/m1/
│       │       ├── Pages/
│       │       │   └── LoginPage.java
│       │       ├── Runner/
│       │       │   └── TestRunner.java
│       │       ├── Steps/
│       │       │   └── LoginSteps.java
│       │       └── Utils/
│       │       │   ├── TestConfigs.java
│       │           └── TestUtils.java
│       └── resources/
│           ├── Features/
│           │   ├── login.feature
│           │   ├── user-management.feature
│           │   ├── dashboard.feature
│           │   ├── security.feature
│           │   └── performance.feature
│           ├── Drivers/
│           │   └── chrome-linux64/
│           ├── test.properties
│           └── testdata/
│               └── testdata.json
├── pom.xml
└── README.md
```

## Test Scenarios

### 1. Login Functionality (`login.feature`)
- **Positive Tests**: Valid credential login, successful redirect
- **Negative Tests**: Invalid credentials, empty fields, special characters
- **Security Tests**: SQL injection, XSS, command injection prevention
- **Validation Tests**: Field validation, error message verification

## Configuration

### Environment Configuration
The framework supports multiple environments through configuration files:

```properties
# test.properties
environment=test
login.url=https://m1-impl.hsenidmobile.com/cas/login
browser=chrome
headless=true
timeout=30
```

### Browser Configuration
Support for multiple browsers with fallback options:
- Chrome (default)
- Firefox
- Edge

### Test Execution Options
```bash
# Run with specific browser
mvn test -Dbrowser=firefox

# Run with specific environment
mvn test -Denvironment=staging

# Run with custom configuration
mvn test -Dconfig.file=custom.properties
```

## Running Tests

### Prerequisites
- Java 21
- Maven 3.6+
- Chrome/Firefox/Edge browser drivers

### Basic Test Execution
```bash
# Run all tests
mvn test

# Run specific feature
mvn test -Dcucumber.features="src/test/resources/Features/login.feature"

# Run with tags
mvn test -Dcucumber.filter.tags="@smoke"
```

### Tag-based Execution
```bash
# Run smoke tests only
mvn test -Dcucumber.filter.tags="@smoke"

# Run security tests
mvn test -Dcucumber.filter.tags="@security"

# Run performance tests
mvn test -Dcucumber.filter.tags="@performance"

# Run multiple tag combinations
mvn test -Dcucumber.filter.tags="@smoke and @positive"
```

## Test Data Management

### Dynamic Test Data
The framework includes a comprehensive test data file (`testdata.json`) with:
- Valid and invalid user credentials
- Security test payloads
- Performance test parameters
- Environment-specific configurations

### Test Data Utilities
```java
// Generate random test data
String randomEmail = TestUtils.generateRandomEmail();
String randomPassword = TestUtils.generateRandomPassword();
String randomUsername = TestUtils.generateRandomUsername();

// Validate data formats
boolean isValidEmail = TestUtils.isValidEmail("test@example.com");
boolean isStrongPassword = TestUtils.isStrongPassword("Pass123!");
```

## Reporting

### Test Reports
The framework generates multiple report formats:
- **HTML Reports**: `target/HtmlReports/login.html`
- **JSON Reports**: `target/JsonReports/login.json`
- **JUnit Reports**: `target/JunitReports/login.xml`

### Screenshots
Automatic screenshot capture on test failures:
- Location: `target/screenshots/`
- Naming: `{TestName}_{Timestamp}.png`

## Best Practices

### 1. Test Organization
- Use descriptive feature names and scenario titles
- Group related tests with appropriate tags
- Maintain clear test data separation

### 2. Error Handling
- Implement proper exception handling
- Use meaningful error messages
- Include fallback mechanisms

### 3. Performance Considerations
- Use explicit waits instead of thread.sleep
- Implement proper cleanup in @After methods
- Optimize test data generation

### 4. Security Testing
- Test both positive and negative security scenarios
- Validate input sanitization
- Test authentication and authorization thoroughly

## Troubleshooting

### Common Issues

1. **Driver Initialization Failures**
   - Check browser driver compatibility
   - Verify system dependencies
   - Use fallback browser options

2. **Element Not Found Errors**
   - Verify element locators
   - Check page load timing
   - Use SelfHealingDriver features

3. **Test Execution Failures**
   - Check network connectivity
   - Verify test environment availability
   - Review test data validity

4. **Check whether Cucumber docker file is correctly up and running**
### Debug Mode
Enable debug logging for troubleshooting:
```properties
logging.level=DEBUG
logging.file=target/debug.log
```

## Future Enhancements

### Planned Features
- **Parallel Execution**: Multi-threaded test execution
- **Automatic Web Component Tracking**: Automatically create a JSON structure of web components for unknown Elements.
- **AI Assisted Automation**: Build test cases automatically to SRS Document and webElements.

### Integration Opportunities
- **CI/CD Integration**: Jenkins, GitLab CI, GitHub Actions
- **Test Management**: TestRail, Jira integration
- **Monitoring**: Integration with APM tools
- **Reporting**: Enhanced reporting with Allure

## Contributing

### Code Standards
- Follow Java coding conventions
- Use meaningful variable and method names
- Include proper documentation
- Write unit tests for utility methods

### Testing Guidelines
- Maintain test independence
- Use appropriate test data
- Implement proper cleanup
- Follow page object model pattern

## Support

For questions or issues:
1. Check the troubleshooting section
2. Review test logs and reports
3. Verify configuration settings
4. Check browser driver compatibility

## License

This project is proprietary and confidential. All rights reserved.
