# Prometheus JMX Exporter - Servlet

This module provides servlet-based support for exposing JMX metrics in Prometheus format, suitable for deployment in EAR/WAR applications on application servers like WebLogic, WildFly, Tomcat, etc.

## Features

- Deploy as a WAR file or embed in existing EAR/WAR applications
- Uses the platform MBeanServer (compatible with WebLogic and other Java EE servers)
- Configurable via YAML configuration files or servlet init parameters
- Supports hot-reload of configuration without server restart
- No Java agent required
- Dedicated PrometheusRegistry per servlet instance

## Usage

### Standalone WAR Deployment

1. Build the WAR file:
   ```bash
   mvn clean package
   ```

2. Deploy `jmx_prometheus_servlet-<version>.war` to your application server

3. Create a configuration file (e.g., `/etc/jmx-exporter/config.yaml`) - see `example-config.yaml` for a template

4. Configure the servlet via `web.xml` or server-specific deployment descriptors

5. Access metrics at `http://localhost:8080/jmx_prometheus_servlet/metrics`

### Embedded in Existing Application

#### Option 1: Add as Dependency (Maven)

```xml
<dependency>
    <groupId>io.prometheus.jmx</groupId>
    <artifactId>jmx_prometheus_servlet</artifactId>
    <version>1.5.0-post</version>
    <classifier>classes</classifier>
</dependency>
```

#### Option 2: Configure in web.xml

```xml
<servlet>
    <servlet-name>prometheus</servlet-name>
    <servlet-class>io.prometheus.jmx.servlet.PrometheusMetricsServlet</servlet-class>
    <init-param>
        <param-name>configFile</param-name>
        <param-value>/etc/jmx-exporter/config.yaml</param-value>
    </init-param>
    <!-- Or use a classpath resource -->
    <!--
    <init-param>
        <param-name>configResource</param-name>
        <param-value>jmx-exporter-config.yaml</param-value>
    </init-param>
    -->
    <load-on-startup>1</load-on-startup>
</servlet>

<servlet-mapping>
    <servlet-name>prometheus</servlet-name>
    <url-pattern>/metrics</url-pattern>
</servlet-mapping>
```

## Configuration

The servlet supports the following init parameters:

- **configFile**: Path to a YAML configuration file on the filesystem (e.g., `/etc/jmx-exporter/config.yaml`)
- **configResource**: Path to a YAML configuration file on the classpath (e.g., `config.yaml` or `META-INF/config.yaml`)

If neither parameter is provided, the servlet will use a minimal default configuration that exports all available MBeans.

### Configuration File Format

The configuration file uses the same YAML format as the JMX Exporter Java agent. See the [main documentation](https://prometheus.github.io/jmx_exporter/) for details.

Example configuration:

```yaml
---
lowercaseOutputName: false
lowercaseOutputLabelNames: false

rules:
  # Export specific MBean attributes with custom names
  - pattern: 'java.lang<type=Memory><HeapMemoryUsage>(\w+)'
    name: java_memory_heap_$1
    type: GAUGE
    
  # Default rule - export all remaining MBeans
  - pattern: ".*"
```

### Hot Reload

When using the `configFile` init parameter (file path), the servlet automatically reloads the configuration file when it detects changes. This is handled by the underlying JmxCollector which checks the file's last modified timestamp on each scrape and reloads if necessary.

**Note**: Hot reload only works with `configFile` (filesystem path), not with `configResource` (classpath resource), since classpath resources cannot be monitored for changes.

To update the configuration without restarting:
1. Edit your configuration file (e.g., `/etc/jmx-exporter/config.yaml`)
2. Save the changes
3. The next metrics scrape will automatically use the updated configuration

This allows you to:
- Add or remove MBean collection rules
- Modify metric names and labels
- Adjust include/exclude patterns
- All without requiring a server restart

## WebLogic Specific Notes

When deploying to WebLogic Server:

1. The servlet uses `ManagementFactory.getPlatformMBeanServer()` which provides access to both JVM and WebLogic-specific MBeans
2. You can access WebLogic domain runtime MBeans through the platform MBeanServer
3. Make sure the WAR is deployed at the application level, not as a shared library
4. Consider security implications - restrict access to the `/metrics` endpoint using WebLogic security roles

## Example: Deploying to WebLogic

1. Create a configuration file at `/opt/jmx-exporter/config.yaml`
2. Deploy the WAR file through WebLogic Console or using WLST
3. Access metrics at `http://your-server:7001/jmx_prometheus_servlet/metrics`

## Comparison with Java Agent

| Feature | Servlet | Java Agent |
|---------|---------|------------|
| Deployment | WAR/EAR | JVM argument |
| Server Restart | Not required | Required |
| Configuration | Per application | Per JVM |
| Registry | Dedicated | Shared |
| Use Case | Application-specific metrics | JVM-wide metrics |

## Security Considerations

- The `/metrics` endpoint exposes detailed information about your application and JVM
- Use WebLogic security constraints or servlet filters to restrict access
- Consider deploying on an internal network or using authentication
- Review the exposed MBeans and use include/exclude patterns to limit sensitive data

## Troubleshooting

### Servlet fails to initialize

Check the application server logs for detailed error messages. Common issues:
- Configuration file not found
- Invalid YAML syntax in configuration
- Missing dependencies

### No metrics appear

- Verify the configuration file contains valid rules
- Check that MBeans you're trying to export actually exist using JConsole or VisualVM
- Review application server logs for errors during metric collection

### Configuration changes not reflected

- Ensure you're using `configFile` (not `configResource`) for hot reload support
- Check file modification timestamps
- Verify the application has read access to the configuration file

## License

Apache License 2.0
