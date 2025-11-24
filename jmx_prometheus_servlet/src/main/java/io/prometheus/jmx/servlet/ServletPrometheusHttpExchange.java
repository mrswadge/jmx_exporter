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

import io.prometheus.metrics.exporter.common.PrometheusHttpExchange;
import io.prometheus.metrics.exporter.common.PrometheusHttpRequest;
import io.prometheus.metrics.exporter.common.PrometheusHttpResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Adapter class that bridges between the Prometheus HTTP exchange API and the Servlet API.
 *
 * <p>This class implements the {@link PrometheusHttpExchange} interface to allow the Prometheus
 * scrape handler to work with standard Java Servlet requests and responses.
 */
class ServletPrometheusHttpExchange implements PrometheusHttpExchange {

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final PrometheusHttpRequest prometheusRequest;

    /**
     * Constructor.
     *
     * @param request the servlet request
     * @param response the servlet response
     */
    public ServletPrometheusHttpExchange(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
        this.prometheusRequest = new ServletPrometheusHttpRequest();
    }

    @Override
    public PrometheusHttpRequest getRequest() {
        return prometheusRequest;
    }

    @Override
    public PrometheusHttpResponse getResponse() {
        return new ServletPrometheusHttpResponse();
    }

    @Override
    public void handleException(IOException e) throws IOException {
        throw e;
    }

    @Override
    public void handleException(RuntimeException e) {
        throw e;
    }

    @Override
    public void close() {
        // Nothing to close for servlet API
    }

    /** Implementation of PrometheusHttpRequest for Servlet API. */
    private class ServletPrometheusHttpRequest implements PrometheusHttpRequest {

        @Override
        public String getQueryString() {
            return request.getQueryString();
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return request.getHeaders(name);
        }

        @Override
        public String getMethod() {
            return request.getMethod();
        }

        @Override
        public String getRequestPath() {
            return request.getRequestURI();
        }
    }

    /** Implementation of PrometheusHttpResponse for Servlet API. */
    private class ServletPrometheusHttpResponse implements PrometheusHttpResponse {

        @Override
        public void setHeader(String name, String value) {
            response.setHeader(name, value);
        }

        @Override
        public OutputStream sendHeadersAndGetBody(int statusCode, int contentLength)
                throws IOException {
            response.setStatus(statusCode);
            if (contentLength > 0) {
                response.setContentLength(contentLength);
            }
            return response.getOutputStream();
        }
    }
}
