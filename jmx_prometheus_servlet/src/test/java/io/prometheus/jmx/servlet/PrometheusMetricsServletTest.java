/*
 * Copyright (C) The Prometheus jmx_exporter Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.prometheus.jmx.servlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Enumeration;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for PrometheusMetricsServlet */
class PrometheusMetricsServletTest {

    @Test
    void testInitWithNoConfig() throws Exception {
        PrometheusMetricsServlet servlet = new PrometheusMetricsServlet();
        TestServletConfig config = new TestServletConfig();

        servlet.init(config);

        // Verify servlet initialized successfully
        assertThat(servlet).isNotNull();
    }

    @Test
    void testInitWithConfigFile(@TempDir File tempDir) throws Exception {
        // Create a temporary config file
        File configFile = new File(tempDir, "config.yaml");
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("---\n");
            writer.write("lowercaseOutputName: true\n");
        }

        PrometheusMetricsServlet servlet = new PrometheusMetricsServlet();
        TestServletConfig config = new TestServletConfig();
        config.setInitParameter("configFile", configFile.getAbsolutePath());

        servlet.init(config);

        // Verify servlet initialized successfully
        assertThat(servlet).isNotNull();
    }

    @Test
    void testInitWithNonExistentConfigFile() {
        PrometheusMetricsServlet servlet = new PrometheusMetricsServlet();
        TestServletConfig config = new TestServletConfig();
        config.setInitParameter("configFile", "/nonexistent/config.yaml");

        assertThatThrownBy(() -> servlet.init(config))
                .isInstanceOf(ServletException.class)
                .hasMessageContaining("Failed to initialize");
    }

    @Test
    void testInitWithConfigResource() throws Exception {
        PrometheusMetricsServlet servlet = new PrometheusMetricsServlet();
        TestServletConfig config = new TestServletConfig();
        config.setInitParameter("configResource", "example-config.yaml");

        servlet.init(config);

        // Verify servlet initialized successfully
        assertThat(servlet).isNotNull();
    }

    @Test
    void testDoGet(@TempDir File tempDir) throws Exception {
        // Create a simple config
        File configFile = new File(tempDir, "config.yaml");
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("---\n");
        }

        // Initialize servlet
        PrometheusMetricsServlet servlet = new PrometheusMetricsServlet();
        TestServletConfig config = new TestServletConfig();
        config.setInitParameter("configFile", configFile.getAbsolutePath());
        servlet.init(config);

        // Create mock request and response
        TestHttpServletRequest request = new TestHttpServletRequest();
        TestHttpServletResponse response = new TestHttpServletResponse();

        // Execute request
        servlet.doGet(request, response);

        // Verify response
        String output = response.getOutputAsString();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(output).contains("# HELP");
        assertThat(output).contains("# TYPE");
    }

    /** Test implementation of ServletConfig */
    private static class TestServletConfig implements ServletConfig {
        private final java.util.Map<String, String> initParameters = new java.util.HashMap<>();

        public void setInitParameter(String name, String value) {
            initParameters.put(name, value);
        }

        @Override
        public String getServletName() {
            return "TestServlet";
        }

        @Override
        public ServletContext getServletContext() {
            return null;
        }

        @Override
        public String getInitParameter(String name) {
            return initParameters.get(name);
        }

        @Override
        public Enumeration<String> getInitParameterNames() {
            return Collections.enumeration(initParameters.keySet());
        }
    }

    /** Test implementation of HttpServletRequest */
    private static class TestHttpServletRequest implements HttpServletRequest {
        @Override
        public String getQueryString() {
            return null;
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return Collections.emptyEnumeration();
        }

        @Override
        public String getMethod() {
            return "GET";
        }

        @Override
        public String getRequestURI() {
            return "/metrics";
        }

        // Minimal implementation - other methods throw UnsupportedOperationException
        @Override
        public String getAuthType() {
            throw new UnsupportedOperationException();
        }

        @Override
        public javax.servlet.http.Cookie[] getCookies() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getDateHeader(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getHeader(String name) {
            return null;
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            return Collections.emptyEnumeration();
        }

        @Override
        public int getIntHeader(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getPathInfo() {
            return null;
        }

        @Override
        public String getPathTranslated() {
            return null;
        }

        @Override
        public String getContextPath() {
            return "";
        }

        @Override
        public String getRemoteUser() {
            return null;
        }

        @Override
        public boolean isUserInRole(String role) {
            return false;
        }

        @Override
        public java.security.Principal getUserPrincipal() {
            return null;
        }

        @Override
        public String getRequestedSessionId() {
            return null;
        }

        @Override
        public StringBuffer getRequestURL() {
            return new StringBuffer("http://localhost/metrics");
        }

        @Override
        public String getServletPath() {
            return "/metrics";
        }

        @Override
        public javax.servlet.http.HttpSession getSession(boolean create) {
            return null;
        }

        @Override
        public javax.servlet.http.HttpSession getSession() {
            return null;
        }

        @Override
        public String changeSessionId() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isRequestedSessionIdValid() {
            return false;
        }

        @Override
        public boolean isRequestedSessionIdFromCookie() {
            return false;
        }

        @Override
        public boolean isRequestedSessionIdFromURL() {
            return false;
        }

        @Override
        public boolean isRequestedSessionIdFromUrl() {
            return false;
        }

        @Override
        public boolean authenticate(HttpServletResponse response) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void login(String username, String password) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void logout() {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.Collection<javax.servlet.http.Part> getParts() {
            throw new UnsupportedOperationException();
        }

        @Override
        public javax.servlet.http.Part getPart(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T extends javax.servlet.http.HttpUpgradeHandler> T upgrade(Class<T> handlerClass) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object getAttribute(String name) {
            return null;
        }

        @Override
        public Enumeration<String> getAttributeNames() {
            return Collections.emptyEnumeration();
        }

        @Override
        public String getCharacterEncoding() {
            return "UTF-8";
        }

        @Override
        public void setCharacterEncoding(String env) {}

        @Override
        public int getContentLength() {
            return -1;
        }

        @Override
        public long getContentLengthLong() {
            return -1L;
        }

        @Override
        public String getContentType() {
            return null;
        }

        @Override
        public javax.servlet.ServletInputStream getInputStream() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getParameter(String name) {
            return null;
        }

        @Override
        public Enumeration<String> getParameterNames() {
            return Collections.emptyEnumeration();
        }

        @Override
        public String[] getParameterValues(String name) {
            return null;
        }

        @Override
        public java.util.Map<String, String[]> getParameterMap() {
            return Collections.emptyMap();
        }

        @Override
        public String getProtocol() {
            return "HTTP/1.1";
        }

        @Override
        public String getScheme() {
            return "http";
        }

        @Override
        public String getServerName() {
            return "localhost";
        }

        @Override
        public int getServerPort() {
            return 80;
        }

        @Override
        public java.io.BufferedReader getReader() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getRemoteAddr() {
            return "127.0.0.1";
        }

        @Override
        public String getRemoteHost() {
            return "localhost";
        }

        @Override
        public void setAttribute(String name, Object o) {}

        @Override
        public void removeAttribute(String name) {}

        @Override
        public java.util.Locale getLocale() {
            return java.util.Locale.getDefault();
        }

        @Override
        public Enumeration<java.util.Locale> getLocales() {
            return Collections.enumeration(
                    Collections.singletonList(java.util.Locale.getDefault()));
        }

        @Override
        public boolean isSecure() {
            return false;
        }

        @Override
        public javax.servlet.RequestDispatcher getRequestDispatcher(String path) {
            return null;
        }

        @Override
        public String getRealPath(String path) {
            return null;
        }

        @Override
        public int getRemotePort() {
            return 0;
        }

        @Override
        public String getLocalName() {
            return "localhost";
        }

        @Override
        public String getLocalAddr() {
            return "127.0.0.1";
        }

        @Override
        public int getLocalPort() {
            return 80;
        }

        @Override
        public ServletContext getServletContext() {
            return null;
        }

        @Override
        public javax.servlet.AsyncContext startAsync() {
            throw new UnsupportedOperationException();
        }

        @Override
        public javax.servlet.AsyncContext startAsync(
                javax.servlet.ServletRequest servletRequest,
                javax.servlet.ServletResponse servletResponse) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isAsyncStarted() {
            return false;
        }

        @Override
        public boolean isAsyncSupported() {
            return false;
        }

        @Override
        public javax.servlet.AsyncContext getAsyncContext() {
            throw new UnsupportedOperationException();
        }

        @Override
        public javax.servlet.DispatcherType getDispatcherType() {
            return javax.servlet.DispatcherType.REQUEST;
        }
    }

    /** Test implementation of HttpServletResponse */
    private static class TestHttpServletResponse implements HttpServletResponse {
        private int status = 200;
        private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        private final java.util.Map<String, String> headers = new java.util.HashMap<>();

        public String getOutputAsString() {
            return outputStream.toString();
        }

        @Override
        public void setStatus(int sc) {
            this.status = sc;
        }

        public int getStatus() {
            return status;
        }

        @Override
        public void setHeader(String name, String value) {
            headers.put(name, value);
        }

        @Override
        public ServletOutputStream getOutputStream() {
            return new ServletOutputStream() {
                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setWriteListener(WriteListener writeListener) {}

                @Override
                public void write(int b) {
                    outputStream.write(b);
                }
            };
        }

        @Override
        public void setContentLength(int len) {}

        // Minimal implementation
        @Override
        public void addCookie(javax.servlet.http.Cookie cookie) {}

        @Override
        public boolean containsHeader(String name) {
            return headers.containsKey(name);
        }

        @Override
        public String encodeURL(String url) {
            return url;
        }

        @Override
        public String encodeRedirectURL(String url) {
            return url;
        }

        @Override
        public String encodeUrl(String url) {
            return url;
        }

        @Override
        public String encodeRedirectUrl(String url) {
            return url;
        }

        @Override
        public void sendError(int sc, String msg) {}

        @Override
        public void sendError(int sc) {}

        @Override
        public void sendRedirect(String location) {}

        @Override
        public void setDateHeader(String name, long date) {}

        @Override
        public void addDateHeader(String name, long date) {}

        @Override
        public void addHeader(String name, String value) {
            headers.put(name, value);
        }

        @Override
        public void setIntHeader(String name, int value) {}

        @Override
        public void addIntHeader(String name, int value) {}

        @Override
        public void setStatus(int sc, String sm) {
            this.status = sc;
        }

        @Override
        public String getHeader(String name) {
            return headers.get(name);
        }

        @Override
        public java.util.Collection<String> getHeaders(String name) {
            return Collections.singletonList(headers.get(name));
        }

        @Override
        public java.util.Collection<String> getHeaderNames() {
            return headers.keySet();
        }

        @Override
        public String getCharacterEncoding() {
            return "UTF-8";
        }

        @Override
        public String getContentType() {
            return headers.get("Content-Type");
        }

        @Override
        public PrintWriter getWriter() {
            return new PrintWriter(outputStream);
        }

        @Override
        public void setCharacterEncoding(String charset) {}

        @Override
        public void setContentLengthLong(long len) {}

        @Override
        public void setContentType(String type) {
            headers.put("Content-Type", type);
        }

        @Override
        public void setBufferSize(int size) {}

        @Override
        public int getBufferSize() {
            return 8192;
        }

        @Override
        public void flushBuffer() {}

        @Override
        public void resetBuffer() {}

        @Override
        public boolean isCommitted() {
            return false;
        }

        @Override
        public void reset() {}

        @Override
        public void setLocale(java.util.Locale loc) {}

        @Override
        public java.util.Locale getLocale() {
            return java.util.Locale.getDefault();
        }
    }
}
