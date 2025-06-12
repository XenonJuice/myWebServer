# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Java-based web server implementation (similar to Tomcat) called Livonia. It implements the Servlet API and
provides a container for web applications.

## Architecture

The project follows a hierarchical container architecture:

1. **Server** - Top-level container that manages the entire server instance
2. **Service** - Groups connectors with an engine for request processing
3. **Engine** - Processes requests and routes them to appropriate hosts
4. **Host** - Virtual host that manages web applications
5. **Context** - Represents a single web application
6. **Endpoint** - Manages servlet lifecycle within a context

Key components:

- **Connector** - Handles network connections (HTTP protocol)
- **Processor** - Processes individual HTTP requests/responses
- **Loader** - Custom classloader for web applications
- **Mapper** - Routes requests through the container hierarchy
- **Filter** - Implements servlet filter chain functionality

## Build Commands

```bash
# Compile the project
mvn compile

# Run tests
mvn test

# Clean build artifacts
mvn clean

# Package the project
mvn package

# Run a specific test class
mvn test -Dtest=TestClassName

# Run tests with specific pattern
mvn test -Dtest=TestClassName#testMethodName
```

## Running the Server

The server is started through the Bootstrap class which sets up the classloader hierarchy and launches Livonia:

```bash
# Start server (requires proper classpath setup)
java livonia.startup.Bootstrap -start -c path/to/server.xml

# Stop server
java livonia.startup.Bootstrap -stop -c path/to/server.xml
```

Server configuration is defined in `server.xml` with the following structure:

- Server (shutdownPort, shutdownCommand)
    - Service
        - Connector (port, protocol)
        - Engine (defaultHost)
            - Host (name, appBase)
                - Context (path, docBase)

## Development Notes

- **Java Version**: Configured for Java 23 with preview features enabled
- **Main Entry Points**:
    - `Bootstrap.java` - JVM entry point, sets up classloaders
    - `Livonia.java` - Server startup logic
- **XML Parsing**: Uses custom MiniDigester for configuration parsing
- **Logging**: Uses SLF4J with Logback
- **Testing**: JUnit 4/5, Mockito, and TestNG are available

## Directory Structure

- `/src/main/java/livonia/` - Core server implementation
    - `base/` - Core interfaces and constants
    - `core/` - Default implementations
    - `connector/` - HTTP connector and request/response handling
    - `XMLParse/` - Configuration file parsing
    - `lifecycle/` - Lifecycle management
    - `filter/` - Servlet filter implementation
    - `loader/` - Custom classloader for web apps
- `/src/test/` - Test classes
- `/src/note/` - Documentation and configuration examples

## Key Implementation Details

- HTTP request processing uses chunked transfer encoding support
- Custom classloader isolation for web applications
- Filter chain implementation for servlet filters
- Resource management for static files and JARs
- Hot-reload support for development (when reloadable=true)