# JMX Exporter Servlet - Quick Start Guide

This guide will help you get started with the JMX Exporter Servlet in 5 minutes.

## Prerequisites

- Java 8 or later
- Servlet 3.1+ compatible application server (WebLogic, Tomcat, WildFly, etc.)
- Maven (for building)

## Option 1: Standalone WAR Deployment (Fastest)

### Step 1: Build the WAR

```bash
cd jmx_prometheus_servlet
mvn clean package
```

### Step 2: Deploy

Copy the WAR file to your application server's deployment directory:

**WebLogic:**
```bash
cp target/jmx_prometheus_servlet-*.war $DOMAIN_HOME/autodeploy/
```

**Tomcat:**
```bash
cp target/jmx_prometheus_servlet-*.war $CATALINA_HOME/webapps/
```

**WildFly/JBoss:**
```bash
cp target/jmx_prometheus_servlet-*.war $JBOSS_HOME/standalone/deployments/
```

### Step 3: Access Metrics

Wait for deployment to complete, then access:
```bash
curl http://localhost:8080/jmx_prometheus_servlet-1.5.0-post/metrics
```

You should see Prometheus-format metrics output.

## Option 2: Custom Configuration

### Step 1: Create Configuration File

Create `/etc/jmx-exporter/config.yaml`:

```yaml
---
lowercaseOutputName: false
rules:
  # Memory metrics
  - pattern: 'java.lang<type=Memory><HeapMemoryUsage>(\w+)'
    name: jvm_memory_heap_$1_bytes
    type: GAUGE
    
  # Thread metrics
  - pattern: 'java.lang<type=Threading><>(\w+)'
    name: jvm_threads_$1
    type: GAUGE
    
  # Export everything else
  - pattern: ".*"
```

### Step 2: Update web.xml

Edit `src/main/webapp/WEB-INF/web.xml`:

```xml
<servlet>
    <servlet-name>prometheus</servlet-name>
    <servlet-class>io.prometheus.jmx.servlet.PrometheusMetricsServlet</servlet-class>
    <init-param>
        <param-name>configFile</param-name>
        <param-value>/etc/jmx-exporter/config.yaml</param-value>
    </init-param>
    <load-on-startup>1</load-on-startup>
</servlet>
```

### Step 3: Rebuild and Deploy

```bash
mvn clean package
# Deploy as in Option 1
```

## Option 3: Embed in Your Application

### Step 1: Add Dependency

Add to your application's `pom.xml`:

```xml
<dependency>
    <groupId>io.prometheus.jmx</groupId>
    <artifactId>jmx_prometheus_servlet</artifactId>
    <version>1.5.0-post</version>
    <classifier>classes</classifier>
</dependency>
```

### Step 2: Configure in Your web.xml

Add to your application's `src/main/webapp/WEB-INF/web.xml`:

```xml
<servlet>
    <servlet-name>jmx-metrics</servlet-name>
    <servlet-class>io.prometheus.jmx.servlet.PrometheusMetricsServlet</servlet-class>
    <init-param>
        <param-name>configResource</param-name>
        <param-value>jmx-config.yaml</param-value>
    </init-param>
    <load-on-startup>1</load-on-startup>
</servlet>

<servlet-mapping>
    <servlet-name>jmx-metrics</servlet-name>
    <url-pattern>/internal/metrics</url-pattern>
</servlet-mapping>
```

### Step 3: Add Configuration Resource

Create `src/main/resources/jmx-config.yaml`:

```yaml
---
rules:
  - pattern: ".*"
```

### Step 4: Build and Deploy Your Application

```bash
mvn clean package
# Deploy your application's WAR/EAR
```

Access metrics at: `http://localhost:8080/your-app/internal/metrics`

## Testing Your Deployment

### 1. Check if endpoint is accessible

```bash
curl http://localhost:8080/your-app/metrics
```

### 2. Look for specific metrics

```bash
# Memory metrics
curl http://localhost:8080/your-app/metrics | grep heap

# Thread metrics
curl http://localhost:8080/your-app/metrics | grep thread

# All java.lang metrics
curl http://localhost:8080/your-app/metrics | grep java_lang
```

### 3. Verify hot-reload (if using configFile)

```bash
# Edit your config file
vi /etc/jmx-exporter/config.yaml

# Add a new rule, save the file

# Wait 15 seconds (default Prometheus scrape interval)
# Fetch metrics again - new metrics should appear
curl http://localhost:8080/your-app/metrics
```

## Common Patterns

### WebLogic Server Metrics

```yaml
rules:
  # Server runtime
  - pattern: 'com.bea<Type=ServerRuntime, Name=(\w+)><>State'
    name: weblogic_server_state
    type: GAUGE
    labels:
      server: $1
      
  # DataSource connections
  - pattern: 'com.bea<Type=JDBCDataSourceRuntime, Name=(\w+)><>ActiveConnectionsCurrentCount'
    name: weblogic_datasource_active_connections
    type: GAUGE
    labels:
      datasource: $1
```

### Application-Specific MBeans

```yaml
rules:
  # Your custom MBeans
  - pattern: 'com.mycompany<type=(\w+), name=(\w+)><>(\w+)'
    name: myapp_$1_$3
    type: GAUGE
    labels:
      component: $2
```

### Filter Out Unwanted Metrics

```yaml
# Exclude all JMX internal metrics
excludeObjectNames:
  - "JMImplementation:*"
  - "com.sun.management:*"
```

## Troubleshooting

### Servlet Fails to Initialize

Check application server logs:
```bash
# WebLogic
tail -f $DOMAIN_HOME/servers/*/logs/*.log

# Tomcat
tail -f $CATALINA_HOME/logs/catalina.out
```

Common issues:
- Configuration file not found or not readable
- Invalid YAML syntax
- Missing dependencies

### No Metrics Appear

1. Verify MBeans exist:
   ```bash
   # Use jconsole or jvisualvm to browse available MBeans
   jconsole localhost:7091
   ```

2. Check configuration rules match MBean names

3. Enable debug logging (if available)

### Hot-Reload Not Working

- Ensure using `configFile` parameter (not `configResource`)
- Verify file permissions allow reading
- Check file modification timestamp is updating:
  ```bash
  ls -l /etc/jmx-exporter/config.yaml
  ```

## Next Steps

- Read the full [README](../README.md) for detailed information
- Check [examples](examples/README.md) for more deployment scenarios
- Review [example-config.yaml](../src/main/resources/example-config.yaml) for common patterns
- Consult [JMX Exporter documentation](https://prometheus.github.io/jmx_exporter/) for advanced configuration

## Getting Help

If you encounter issues:
1. Check application server logs
2. Verify configuration file syntax
3. Test with minimal configuration (no configFile parameter)
4. Review examples in the `examples/` directory
5. Open an issue on GitHub with logs and configuration
