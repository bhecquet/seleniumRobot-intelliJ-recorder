package io.github.bhecquet.seleniumRobot.recorder;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PageClassManagerTest {

    private PageClassManager manager;

    @Before
    public void setUp() {
        manager = new PageClassManager(null);
    }

    @Test
    public void shouldBuildClassNameFromPageMethodName() {
        assertEquals("LocatorsPage", manager.buildClassName("locatorsPage"));
    }

    @Test
    public void shouldAddPageSuffixWhenMissing() {
        assertEquals("CustomerDetailsPage", manager.buildClassName("customerDetails"));
    }

    @Test
    public void shouldNotDuplicatePageSuffix() {
        assertEquals("LocatorsPage", manager.buildClassName("LocatorsPage"));
    }

    @Test
    public void shouldBuildGenericClassNameForNullValue() {
        assertEquals("NewPage", manager.buildClassName(null));
    }

    @Test
    public void shouldBuildGenericClassNameForEmptyValue() {
        assertEquals("NewPage", manager.buildClassName(""));
    }

    @Test
    public void shouldBuildJavaFileName() {
        assertEquals("LocatorsPage.java", manager.buildFileName("locatorsPage"));
    }

    @Test
    public void shouldBuildPageMethodCall() {
        assertEquals("LocatorsPage.execute();", manager.buildMethodCall("locatorsPage"));
    }

    @Test
    public void shouldGenerateClassWithPackage() {
        String result = manager.buildClassContent("covea.pic.jenkins.tests.selenium", "locatorsPage");

        assertTrue(result.contains("package covea.pic.jenkins.tests.selenium;"));
        assertTrue(result.contains("public class LocatorsPage"));
        assertTrue(result.contains("public static void execute()"));
    }

    @Test
    public void shouldGenerateClassWithoutPackage() {
        String result = manager.buildClassContent("", "locatorsPage");

        assertTrue(result.startsWith("public class LocatorsPage"));
        assertTrue(result.contains("public static void execute()"));
    }
}