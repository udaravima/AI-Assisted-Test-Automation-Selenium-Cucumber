package com.sdp.m1.Runner;

import org.junit.runner.RunWith;
import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;

@RunWith(Cucumber.class)
@CucumberOptions(features = "src/test/resources/Features", glue = {
                "com.sdp.m1.Steps", "com.sdp.m1.Hooks" }, tags = "@Login", // Default tags to run from
                                                                                            // IDE. Can be
                // overridden by Maven.
                monochrome = true, plugin = { //
                                "pretty",
                                "html:target/HtmlReports/Report.html",
                                "json:target/JsonReports/Report.json",
                                "junit:target/JunitReports/Report.xml"
                })
public class TestRunner {
        // This class should be empty, it's just a runner
}
