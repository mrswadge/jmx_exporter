# JMX Exporter Servlet Examples

This directory contains examples for deploying and using the JMX Exporter Servlet in various scenarios.

## Example 1: Standalone WAR Deployment

### web.xml with File-based Configuration

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee
         http://xmlns.jcp.org/xml/ns/javaee/web-app_3_1.xsd"
         version="3.1">

    <display-name>Prometheus JMX Exporter</display-name>

    <servlet>
        <servlet-name>prometheus</servlet-name>
        <servlet-class>io.prometheus.jmx.servlet.PrometheusMetricsServlet</servlet-class>
        <init-param>
            <param-name>configFile</param-name>
            <param-value>/opt/jmx-exporter/config.yaml</param-value>
        </init-param>
        <load-on-startup>1</load-on-startup>
    </servlet>

    <servlet-mapping>
        <servlet-name>prometheus</servlet-name>
        <url-pattern>/metrics</url-pattern>
    </servlet-mapping>

</web-app>
```

### Deployment Steps

1. Build or download the WAR file
2. Create configuration at `/opt/jmx-exporter/config.yaml`
3. Deploy to your application server
4. Access metrics at `http://your-server:port/jmx_prometheus_servlet/metrics`

## Example 2: Embedded in Existing Application

### web.xml Fragment

Add this to your existing application's web.xml:

```xml
<servlet>
    <servlet-name>jmx-metrics</servlet-name>
    <servlet-class>io.prometheus.jmx.servlet.PrometheusMetricsServlet</servlet-class>
    <init-param>
        <param-name>configResource</param-name>
        <param-value>jmx-exporter-config.yaml</param-value>
    </init-param>
    <load-on-startup>1</load-on-startup>
</servlet>

<servlet-mapping>
    <servlet-name>jmx-metrics</servlet-name>
    <url-pattern>/internal/metrics</url-pattern>
</servlet-mapping>
```

### Maven Dependency

```xml
<dependency>
    <groupId>io.prometheus.jmx</groupId>
    <artifactId>jmx_prometheus_servlet</artifactId>
    <version>1.5.0-post</version>
    <classifier>classes</classifier>
</dependency>
```

Place your configuration file `jmx-exporter-config.yaml` in `src/main/resources/`.

## Example 3: WebLogic Server Deployment

### weblogic.xml (optional - for security constraints)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<weblogic-web-app xmlns="http://xmlns.oracle.com/weblogic/weblogic-web-app">
    <context-root>/monitoring</context-root>
    
    <security-role-assignment>
        <role-name>metrics-reader</role-name>
        <principal-name>MonitoringGroup</principal-name>
    </security-role-assignment>
</weblogic-web-app>
```

### web.xml with Security

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee
         http://xmlns.jcp.org/xml/ns/javaee/web-app_3_1.xsd"
         version="3.1">

    <servlet>
        <servlet-name>prometheus</servlet-name>
        <servlet-class>io.prometheus.jmx.servlet.PrometheusMetricsServlet</servlet-class>
        <init-param>
            <param-name>configFile</param-name>
            <param-value>/u01/app/config/jmx-exporter.yaml</param-value>
        </init-param>
        <load-on-startup>1</load-on-startup>
    </servlet>

    <servlet-mapping>
        <servlet-name>prometheus</servlet-name>
        <url-pattern>/metrics</url-pattern>
    </servlet-mapping>

    <!-- Optional: Secure the metrics endpoint -->
    <security-constraint>
        <web-resource-collection>
            <web-resource-name>Metrics</web-resource-name>
            <url-pattern>/metrics</url-pattern>
        </web-resource-collection>
        <auth-constraint>
            <role-name>metrics-reader</role-name>
        </auth-constraint>
    </security-constraint>

    <security-role>
        <role-name>metrics-reader</role-name>
    </security-role>

</web-app>
```

### WebLogic-Specific Configuration

Example configuration for WebLogic MBeans:

```yaml
---
lowercaseOutputName: false
lowercaseOutputLabelNames: false

# WebLogic-specific rules
rules:
  # WebLogic Server metrics
  - pattern: 'com.bea<Type=ServerRuntime, Name=(\w+)><>(\w+)'
    name: weblogic_server_$2
    type: GAUGE
    labels:
      server: $1

  # WebLogic datasource metrics
  - pattern: 'com.bea<Type=JDBCDataSourceRuntime, Name=(\w+), ServerRuntime=(\w+)><>(\w+)'
    name: weblogic_datasource_$3
    type: GAUGE
    labels:
      datasource: $1
      server: $2

  # WebLogic JMS metrics
  - pattern: 'com.bea<Type=JMSServerRuntime, Name=(\w+), ServerRuntime=(\w+)><>(\w+)'
    name: weblogic_jms_$3
    type: GAUGE
    labels:
      jms_server: $1
      server: $2

  # Application-specific metrics
  - pattern: 'java.lang<type=(\w+)><>(\w+)'
    name: java_$1_$2
    type: GAUGE

  # Default: collect everything else
  - pattern: ".*"
```

## Example 4: Default Configuration (No Config File)

If no configuration is provided, the servlet uses a minimal default that exports all available MBeans:

```xml
<servlet>
    <servlet-name>prometheus</servlet-name>
    <servlet-class>io.prometheus.jmx.servlet.PrometheusMetricsServlet</servlet-class>
    <!-- No init-param - uses default configuration -->
    <load-on-startup>1</load-on-startup>
</servlet>

<servlet-mapping>
    <servlet-name>prometheus</servlet-name>
    <url-pattern>/metrics</url-pattern>
</servlet-mapping>
```

This is useful for:
- Quick testing
- Development environments
- When you want to export all MBeans without filtering

## Testing Your Deployment

After deployment, verify the servlet is working:

```bash
# Check if metrics endpoint is accessible
curl http://localhost:7001/your-app/metrics

# Look for expected metrics
curl http://localhost:7001/your-app/metrics | grep java_lang_Memory

# Check for WebLogic-specific metrics (if on WebLogic)
curl http://localhost:7001/your-app/metrics | grep weblogic_
```

## Performance Considerations

1. **Metric Cardinality**: Be careful with label values that have high cardinality (e.g., request IDs, timestamps)
2. **Collection Frequency**: MBean collection happens on each scrape. Default Prometheus scrape interval is 15s.
3. **File Monitoring**: Hot-reload checks file modification time on each scrape (low overhead)
4. **Registry Isolation**: Each servlet instance has its own registry, avoiding conflicts in multi-app deployments

## Troubleshooting

### Servlet fails to initialize

Check application server logs:
- Configuration file path is correct and readable
- YAML syntax is valid
- Required dependencies are in classpath

### No metrics appear

- Verify MBeans exist: Use JConsole or VisualVM to browse available MBeans
- Check configuration rules match your MBean patterns
- Review servlet logs for collection errors

### Hot reload not working

- Ensure using `configFile` (not `configResource`)
- Verify file permissions allow reading
- Check file system supports modification timestamps
