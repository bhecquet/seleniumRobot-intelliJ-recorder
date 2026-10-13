package io.github.bhecquet.seleniumRobot.recorder;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class SeleniumServletTest {

    private SeleniumServlet servlet;

    @Before
    public void setUp() {
        servlet = new SeleniumServlet(null);
    }

    @Test
    public void shouldBuildMethodNameFromUrlFragment() throws Exception {
        String result = invokeStringMethod(
                "buildPageMethodName",
                new Class<?>[]{String.class, String.class},
                "http://localhost/page.html#locators",
                "Page de test"
        );

        assertEquals("locatorsPage", result);
    }

    @Test
    public void shouldBuildMethodNameFromLastPathSegment() throws Exception {
        String result = invokeStringMethod(
                "buildPageMethodName",
                new Class<?>[]{String.class, String.class},
                "https://application.example/customer/details",
                "Customer details"
        );

        assertEquals("detailsPage", result);
    }

    @Test
    public void shouldRemoveHtmlExtensionFromMethodName() throws Exception {
        String result = invokeStringMethod(
                "buildPageMethodName",
                new Class<?>[]{String.class, String.class},
                "https://application.example/customer-details.html",
                "Customer details"
        );

        assertEquals("customerDetailsPage", result);
    }

    @Test
    public void shouldUseTitleWhenUrlHasNoFragmentOrPath() throws Exception {
        String result = invokeStringMethod(
                "buildPageMethodName",
                new Class<?>[]{String.class, String.class},
                "https://application.example/",
                "Customer details"
        );

        assertEquals("customerDetailsPage", result);
    }

    @Test
    public void shouldUseGenericNameWhenUrlAndTitleAreEmpty() throws Exception {
        String result = invokeStringMethod(
                "buildPageMethodName",
                new Class<?>[]{String.class, String.class},
                "",
                ""
        );

        assertEquals("newPage", result);
    }

    @Test
    public void shouldPrefixMethodNameStartingWithNumber() throws Exception {
        String result = invokeStringMethod(
                "buildPageMethodName",
                new Class<?>[]{String.class, String.class},
                "https://application.example/#123-results",
                "Results"
        );

        assertEquals("page123ResultsPage", result);
    }

    @Test
    public void shouldDetectExistingMethod() throws Exception {
        String javaCode = "public class Main {\n"
                + "    private static void locatorsPage() {\n"
                + "    }\n"
                + "}";

        boolean result = invokeBooleanMethod(
                "containsMethod",
                new Class<?>[]{String.class, String.class},
                javaCode,
                "locatorsPage"
        );

        assertTrue(result);
    }

    @Test
    public void shouldNotDetectMissingMethod() throws Exception {
        String javaCode = "public class Main {\n"
                + "    public static void main(String[] args) {\n"
                + "    }\n"
                + "}";

        boolean result = invokeBooleanMethod(
                "containsMethod",
                new Class<?>[]{String.class, String.class},
                javaCode,
                "locatorsPage"
        );

        assertFalse(result);
    }

    @Test
    public void shouldKeepBaseNameWhenMethodDoesNotExist() throws Exception {
        String javaCode = "public class Main {\n"
                + "    public static void main(String[] args) {\n"
                + "    }\n"
                + "}";

        String result = invokeStringMethod(
                "findAvailableMethodName",
                new Class<?>[]{String.class, String.class},
                javaCode,
                "locatorsPage"
        );

        assertEquals("locatorsPage", result);
    }

    @Test
    public void shouldAddSuffixWhenMethodAlreadyExists() throws Exception {
        String javaCode = "public class Main {\n"
                + "    private static void locatorsPage() {\n"
                + "    }\n"
                + "}";

        String result = invokeStringMethod(
                "findAvailableMethodName",
                new Class<?>[]{String.class, String.class},
                javaCode,
                "locatorsPage"
        );

        assertEquals("locatorsPage2", result);
    }

    @Test
    public void shouldIncrementSuffixUntilNameIsAvailable() throws Exception {
        String javaCode = "public class Main {\n"
                + "    private static void locatorsPage() {\n"
                + "    }\n"
                + "    private static void locatorsPage2() {\n"
                + "    }\n"
                + "    private static void locatorsPage3() {\n"
                + "    }\n"
                + "}";

        String result = invokeStringMethod(
                "findAvailableMethodName",
                new Class<?>[]{String.class, String.class},
                javaCode,
                "locatorsPage"
        );

        assertEquals("locatorsPage4", result);
    }

    private String invokeStringMethod(String name, Class<?>[] parameterTypes, Object... arguments) throws Exception {
        Method method = SeleniumServlet.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return (String) method.invoke(servlet, arguments);
    }

    private boolean invokeBooleanMethod(String name, Class<?>[] parameterTypes, Object... arguments) throws Exception {
        Method method = SeleniumServlet.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return (boolean) method.invoke(servlet, arguments);
    }

}