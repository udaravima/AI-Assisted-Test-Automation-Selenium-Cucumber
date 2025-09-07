#!/bin/bash

# M1 Test Automation Framework - Test Execution Script
# This script provides easy execution of different test scenarios

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
BROWSER="chrome"
ENVIRONMENT="test"
TAGS=""
FEATURE=""
PARALLEL="false"
THREADS="2"
VERBOSE="false"

# Function to print usage
print_usage() {
    echo -e "${BLUE}M1 Test Automation Framework - Test Execution Script${NC}"
    echo ""
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -b, --browser BROWSER     Browser to use (chrome, firefox, edge) [default: chrome]"
    echo "  -e, --environment ENV     Environment to test (test, staging, production) [default: test]"
    echo "  -t, --tags TAGS           Cucumber tags to run (e.g., @smoke, @security)"
    echo "  -f, --feature FEATURE     Specific feature file to run"
    echo "  -p, --parallel            Enable parallel execution"
    echo "  -n, --threads NUM         Number of threads for parallel execution [default: 2]"
    echo "  -v, --verbose             Enable verbose output"
    echo "  -h, --help                Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                                    # Run all tests with default settings"
    echo "  $0 -t @smoke                          # Run only smoke tests"
    echo "  $0 -t @security                       # Run only security tests"
    echo "  $0 -b firefox -e staging             # Run tests with Firefox on staging"
    echo "  $0 -f login.feature                  # Run only login feature"
    echo "  $0 -p -n 4                           # Run tests in parallel with 4 threads"
    echo ""
}

# Function to print section header
print_section() {
    echo -e "\n${BLUE}=== $1 ===${NC}"
}

# Function to print status
print_status() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# Function to check prerequisites
check_prerequisites() {
    print_section "Checking Prerequisites"
    
    # Check Java
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
        print_status "Java found: $JAVA_VERSION"
    else
        print_error "Java is not installed or not in PATH"
        exit 1
    fi
    
    # Check Maven
    if command -v mvn &> /dev/null; then
        MAVEN_VERSION=$(mvn -version | head -n 1 | cut -d' ' -f3)
        print_status "Maven found: $MAVEN_VERSION"
    else
        print_error "Maven is not installed or not in PATH"
        exit 1
    fi
    
    # Check if we're in the right directory
    if [ ! -f "pom.xml" ]; then
        print_error "pom.xml not found. Please run this script from the project root directory."
        exit 1
    fi
    
    print_status "All prerequisites met"
}

# Function to build the project
build_project() {
    print_section "Building Project"
    
    if mvn clean compile test-compile; then
        print_status "Project built successfully"
    else
        print_error "Project build failed"
        exit 1
    fi
}

# Function to run tests
run_tests() {
    print_section "Running Tests"
    
    # Build Maven command
    MVN_CMD="mvn test"
    
    # Add browser parameter
    MVN_CMD="$MVN_CMD -Dbrowser=$BROWSER"
    
    # Add environment parameter
    MVN_CMD="$MVN_CMD -Denvironment=$ENVIRONMENT"
    
    # Add tags if specified
    if [ ! -z "$TAGS" ]; then
        MVN_CMD="$MVN_CMD -Dcucumber.filter.tags=\"$TAGS\""
    fi
    
    # Add feature if specified
    if [ ! -z "$FEATURE" ]; then
        MVN_CMD="$MVN_CMD -Dcucumber.features=\"src/test/resources/Features/$FEATURE\""
    fi
    
    # Add parallel execution if enabled
    if [ "$PARALLEL" = "true" ]; then
        MVN_CMD="$MVN_CMD -Dparallel.execution=true -Dthread.count=$THREADS"
    fi
    
    # Add verbose output if enabled
    if [ "$VERBOSE" = "true" ]; then
        MVN_CMD="$MVN_CMD -Dlogging.level=DEBUG"
    fi
    
    echo "Executing: $MVN_CMD"
    echo ""
    
    # Execute the command
    if eval $MVN_CMD; then
        print_status "Tests completed successfully"
    else
        print_error "Tests failed"
        exit 1
    fi
}

# Function to show test results
show_results() {
    print_section "Test Results"
    
    # Check if reports exist
    if [ -f "target/HtmlReports/login.html" ]; then
        print_status "HTML Report: target/HtmlReports/login.html"
    fi
    
    if [ -f "target/JsonReports/login.json" ]; then
        print_status "JSON Report: target/JsonReports/login.json"
    fi
    
    if [ -f "target/JunitReports/login.xml" ]; then
        print_status "JUnit Report: target/JunitReports/login.xml"
    fi
    
    # Check for screenshots
    if [ -d "target/screenshots" ]; then
        SCREENSHOT_COUNT=$(find target/screenshots -name "*.png" | wc -l)
        if [ $SCREENSHOT_COUNT -gt 0 ]; then
            print_status "Screenshots: target/screenshots/ ($SCREENSHOT_COUNT files)"
        fi
    fi
}

# Function to run specific test scenarios
run_smoke_tests() {
    print_section "Running Smoke Tests"
    TAGS="@smoke"
    run_tests
}

run_security_tests() {
    print_section "Running Security Tests"
    TAGS="@security"
    run_tests
}

run_performance_tests() {
    print_section "Running Performance Tests"
    TAGS="@performance"
    run_tests
}

run_positive_tests() {
    print_section "Running Positive Tests"
    TAGS="@positive"
    run_tests
}

run_negative_tests() {
    print_section "Running Negative Tests"
    TAGS="@negative"
    run_tests
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -b|--browser)
            BROWSER="$2"
            shift 2
            ;;
        -e|--environment)
            ENVIRONMENT="$2"
            shift 2
            ;;
        -t|--tags)
            TAGS="$2"
            shift 2
            ;;
        -f|--feature)
            FEATURE="$2"
            shift 2
            ;;
        -p|--parallel)
            PARALLEL="true"
            shift
            ;;
        -n|--threads)
            THREADS="$2"
            shift 2
            ;;
        -v|--verbose)
            VERBOSE="true"
            shift
            ;;
        -h|--help)
            print_usage
            exit 0
            ;;
        --smoke)
            run_smoke_tests
            exit 0
            ;;
        --security)
            run_security_tests
            exit 0
            ;;
        --performance)
            run_performance_tests
            exit 0
            ;;
        --positive)
            run_positive_tests
            exit 0
            ;;
        --negative)
            run_negative_tests
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            print_usage
            exit 1
            ;;
    esac
done

# Main execution
echo -e "${BLUE}M1 Test Automation Framework${NC}"
echo "Browser: $BROWSER"
echo "Environment: $ENVIRONMENT"
if [ ! -z "$TAGS" ]; then
    echo "Tags: $TAGS"
fi
if [ ! -z "$FEATURE" ]; then
    echo "Feature: $FEATURE"
fi
if [ "$PARALLEL" = "true" ]; then
    echo "Parallel Execution: Enabled ($THREADS threads)"
fi

# Check prerequisites
check_prerequisites

# Build project
build_project

# Run tests
run_tests

# Show results
show_results

print_section "Execution Complete"
print_status "All operations completed successfully"
