/*
 * Copyright 2012 Harald Wellmann
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.ops4j.pax.exam.testng.servlet;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.testng.ITestResult;
import org.testng.TestListenerAdapter;
import org.testng.TestNG;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlInclude;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

/**
 * Base class for test runner servlets, providing the communication link between the embedded test
 * container and the test driver.
 * <p>
 * Derived classes shall provide a method of dependency injection.
 * 
 * @author Harald Wellmann
 * 
 */
@WebServlet(urlPatterns = "/testrunner")
public class TestRunnerServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        String className = request.getParameter("class");
        String methodName = request.getParameter("method");
        try {
            Class<?> clazz = getClass().getClassLoader().loadClass(className);
            response.setContentType("application/octet-stream");
            ServletOutputStream os = response.getOutputStream();
            runSuite(os, clazz, methodName);
            os.flush();
        }
        catch (ClassNotFoundException exc) {
            throw new ServletException("cannot load test class " + className, exc);
        }
    }

    private void runSuite(OutputStream os, Class<?> clazz, String methodName) throws IOException {

        TestNG testNG = new TestNG();
        testNG.setUseDefaultListeners(false);
        TestListenerAdapter listener = new TestListenerAdapter();
        XmlSuite suite = new XmlSuite();
        suite.setName("PaxExamInternal");
        XmlTest xmlTest = new XmlTest(suite);
        XmlClass xmlClass = new XmlClass(clazz);
        xmlTest.getClasses().add(xmlClass);
        XmlInclude xmlInclude = new XmlInclude(methodName);
        xmlClass.getIncludedMethods().add(xmlInclude);

        testNG.setXmlSuites(Arrays.asList(suite));
        testNG.run();

        ObjectOutputStream oos = new ObjectOutputStream(os);
        for (ITestResult result : listener.getFailedTests()) {
            oos.writeObject(result.getThrowable());
        }
        if (listener.getFailedTests().isEmpty()) {
            oos.writeObject("ok");
        }
    }
}
