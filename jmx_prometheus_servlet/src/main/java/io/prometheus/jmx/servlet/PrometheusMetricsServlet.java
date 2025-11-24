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

import io.prometheus.jmx.BuildInfoMetrics;
import io.prometheus.jmx.JmxCollector;
import io.prometheus.metrics.exporter.common.PrometheusScrapeHandler;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet for exposing JMX metrics in Prometheus format.
 *
 * <p>This servlet can be deployed in EAR/WAR applications to expose JMX metrics without requiring
 * the Java agent. It uses the platform MBeanServer and allows configuration through init parameters
 * or a configuration file.
 *
 * <p>Configuration options (servlet init-params):
 *
 * <ul>
 *   <li><b>configFile</b>: Path to YAML configuration file (optional)
 *   <li><b>configResource</b>: Classpath resource for configuration (optional)
 *   <li><b>jmxUrl</b>: JMX URL for remote connections (optional, defaults to platform MBeanServer)
 * </ul>
 *
 * <p>Example web.xml configuration:
 *
 * <pre>{@code
 * <servlet>
 *   <servlet-name>prometheus</servlet-name>
 *   <servlet-class>io.prometheus.jmx.servlet.PrometheusMetricsServlet</servlet-class>
 *   <init-param>
 *     <param-name>configFile</param-name>
 *     <param-value>/etc/jmx-exporter/config.yaml</param-value>
 *   </init-param>
 *   <load-on-startup>1</load-on-startup>
 * </servlet>
 * <servlet-mapping>
 *   <servlet-name>prometheus</servlet-name>
 *   <url-pattern>/metrics</url-pattern>
 * </servlet-mapping>
 * }</pre>
 */
public class PrometheusMetricsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private PrometheusRegistry registry;
    private PrometheusScrapeHandler scrapeHandler;
    private JmxCollector jmxCollector;

    /**
     * Initializes the servlet and sets up the JMX collector.
     *
     * @param config servlet configuration
     * @throws ServletException if initialization fails
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        try {
            // Create a dedicated registry for this servlet
            registry = new PrometheusRegistry();

            // Register build info metrics
            new BuildInfoMetrics().register(registry);

            // Initialize JMX collector based on configuration
            String configFile = config.getInitParameter("configFile");
            String configResource = config.getInitParameter("configResource");

            if (configFile != null && !configFile.isEmpty()) {
                // Load configuration from file
                File file = new File(configFile);
                if (!file.exists()) {
                    throw new ServletException("Configuration file not found: " + configFile);
                }
                jmxCollector = new JmxCollector(file);
            } else if (configResource != null && !configResource.isEmpty()) {
                // Load configuration from classpath resource
                InputStream inputStream =
                        getClass().getClassLoader().getResourceAsStream(configResource);
                if (inputStream == null) {
                    throw new ServletException(
                            "Configuration resource not found: " + configResource);
                }
                jmxCollector = new JmxCollector(inputStream);
            } else {
                // Use minimal default configuration (collects all MBeans)
                String defaultConfig = "---\n";
                jmxCollector = new JmxCollector(defaultConfig);
            }

            // Register the JMX collector
            jmxCollector.register(registry);

            // Create the scrape handler
            scrapeHandler = new PrometheusScrapeHandler(registry);

        } catch (Exception e) {
            throw new ServletException("Failed to initialize PrometheusMetricsServlet", e);
        }
    }

    /**
     * Handles GET requests to export metrics in Prometheus format.
     *
     * @param req HTTP request
     * @param resp HTTP response
     * @throws ServletException if request handling fails
     * @throws IOException if I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Delegate to the Prometheus scrape handler
        scrapeHandler.handleRequest(new ServletPrometheusHttpExchange(req, resp));
    }

    /** Cleanup resources when servlet is destroyed. */
    @Override
    public void destroy() {
        super.destroy();
        // Cleanup is handled automatically by registry lifecycle
    }
}
