# TCP FX Data Provider

[![Java Version](https://img.shields.io/badge/Java-17-blue.svg)](https://openjdk.java.net/projects/jdk/17/)
[![Build Tool](https://img.shields.io/badge/Build-Maven-orange.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE) <!-- Add your LICENSE file -->
[![Docker Support](https://img.shields.io/badge/Docker-Supported-blue.svg?logo=docker)](https://www.docker.com/)

> A Java-based TCP server application that simulates and broadcasts real-time Foreign Exchange (FX) rate data to subscribed clients.

This project provides a robust TCP server built using Java NIO for handling client connections efficiently. It simulates FX rate fluctuations, including configurable volatility and periodic spikes, based on settings in an `application.properties` file or environment variables. Clients can connect, authenticate, and subscribe to specific currency pairs to receive real-time updates.

## Table of Contents
*   [Core Components](#core-components)
*   [Features](#features)
*   [Getting Started](#getting-started)
    *   [Configuration](#configuration)
    *   [Running the Application](#running-the-application)
        *   [Directly (Executable JAR)](#directly-executable-jar)
        *   [Using Docker](#using-docker)
*   [Usage (Testing with Telnet)](#usage-testing-with-telnet)
*   [Contact](#contact)

### Core Components

1.  **`TcpDataProvider`**: The main entry point. Loads configuration, initializes, and starts the core services (`AuthService`, `FxDataServer`, `FxDataPublisher`).
2.  **`FxDataServer`**: Handles TCP connections using Java NIO. Accepts clients, processes commands (`connect`, `subscribe`, etc.), and manages authentication via `AuthService`.
3.  **`FxDataPublisher`**: Periodically updates FX rates (`Rate` objects) based on configured parameters (min/max changes, spike intervals/percentages). Broadcasts updated data to subscribed clients for relevant currency pairs.
4.  **`AuthService`**: Manages client authentication (username/password) and session state. It ensures that a client channel is authenticated and prevents the same *username* from having multiple active sessions simultaneously across different connections.
5.  **`ConfigUtil`**: Loads configuration settings from `application.properties` or environment variables (environment variables take precedence).
6.  **`Rate`**: An entity class representing the instantaneous data for a currency pair.

## Features

*    **`Real-time FX Simulation:`** Simulates bid/ask price changes for configured currency pairs.
*    **`High-Performance Networking:`** Uses Java NIO for efficient, non-blocking handling of multiple client connections.
*    **`Secure Authentication:`** Requires clients to authenticate using username/password credentials.
*    **`Session Management:`** Prevents the same user account from being logged in from multiple different connections simultaneously.
*    **`Subscription Model:`** Clients can subscribe/unsubscribe to specific currency pairs they are interested in.
*    **`Flexible Configuration:`** Control broadcast frequency, normal rate volatility (min/max change), and periodic price spikes (interval, percentage).Settings managed via `application.properties` file or overridden by environment variables.
*    **`Docker Support:`** Includes a `Dockerfile` for easy containerization and deployment.

## Getting Started

### Configuration

Configuration is managed via the `src/main/resources/application.properties` file. Values can be overridden by setting corresponding environment variables (e.g., `SERVER_PORT` overrides `server.port`).

**Example `src/main/resources/application.properties`:**

```properties
# Server Settings
server.port=8090

# Supported Currency Pairs (Comma-separated)
currency.pairs=TCP_USDTRY,TCP_EURUSD,TCP_GBPUSD

# Initial Rates and Limits (Define for each pair in currency.pairs)
tcp_usdtry.bid=37.9973
tcp_usdtry.ask=38.0333
tcp_usdtry.min.limit=35.2134
tcp_usdtry.max.limit=40.1324

tcp_eurusd.bid=1.0848
tcp_eurusd.ask=1.0949
tcp_eurusd.min.limit=1.01254
tcp_eurusd.max.limit=1.21423

tcp_gbpusd.bid=1.295
tcp_gbpusd.ask=1.395
tcp_gbpusd.min.limit=1.11245
tcp_gbpusd.max.limit=1.42142

# Rate Simulation Settings
# Minimum change percentage per update cycle (decimal format, e.g., 0.001 = 0.1%)
minimum.rate.change=0.001
# Maximum change percentage per update cycle (decimal format)
maximum.rate.change=0.004

# Broadcast frequency in milliseconds
publish.frequency=10000

# Spike Simulation Settings
# Spike change percentage (decimal format, e.g., 0.011 = 1.1%)
spike.percentage=0.011
# Spike occurs every N update cycles (e.g., 20 means spike every 20 publishes)
spike.interval=20

# User Credentials (Format: username|password, Comma-separated)
user.credentials=yusuf|pass,admin|admin

```

### Running the Application

#### Directly (Executable JAR)

After building the project (`mvn clean package`), run the JAR file:

```bash
java -jar target/tcp-data-provider.jar
```

The server will start using the configuration baked into the JAR or overridden by environment variables.

#### Using Docker

The included `Dockerfile` allows you to containerize the application.

1.  **Build the Docker image:** (Ensure you have built the JAR first with `mvn clean package`)
    ```bash
    docker build -t tcp-fx-provider .
    ```
    *(You can replace `tcp-fx-provider` with your preferred image name)*

#### Using Docker

The included `Dockerfile` allows you to containerize the application.

1.  **Build the Docker image:** (Ensure you have built the JAR first with `mvn clean package`)
    ```bash
    docker build -t tcp-fx-provider .
    ```

2.  **Run the Docker container:**
    ```bash
    # Run with default settings baked into the JAR
    docker run -d -p 8090:8090 --name fx-provider tcp-fx-provider
    ```

    **Note:** The Docker container uses the `application.properties` baked into the JAR by default. To override configuration for Docker, use environment variables (`-e` flag). This is the recommended method for containers.

    *   **Example: Overriding Port and Credentials:**
        ```bash
        docker run -d -p 9999:9999 \
          -e SERVER_PORT=9999 \
          -e USER_CREDENTIALS="guest|guest" \
          --name fx-provider-custom tcp-fx-provider
        ```

    *   **Example: Overriding Currency Pairs (Important!):**
        If you override `CURRENCY_PAIRS`, you **must** also provide the initial `bid`, `ask`, `min.limit`, and `max.limit` values for **each** new pair as environment variables. The variable names follow the pattern `PAIR_NAME_PROPERTY_NAME` (uppercase, dots replaced with underscores).

        ```bash
        docker run -d -p 8090:8090 \
          -e CURRENCY_PAIRS="BTCUSD,ETHUSD" \
          -e BTCUSD_BID="60000.50" \
          -e BTCUSD_ASK="60001.75" \
          -e BTCUSD_MIN_LIMIT="50000.00" \
          -e BTCUSD_MAX_LIMIT="70000.00" \
          -e ETHUSD_BID="3000.10" \
          -e ETHUSD_ASK="3000.90" \
          -e ETHUSD_MIN_LIMIT="2500.00" \
          -e ETHUSD_MAX_LIMIT="3500.00" \
          -e USER_CREDENTIALS="trader1|secret" \
          --name fx-provider-crypto tcp-fx-provider
        ```
        *(Remember to provide all four required initial values for every pair listed in `CURRENCY_PAIRS`)*


## Usage (Testing with Telnet)

Interact with the server using `telnet`. Commands are pipe-delimited (`|`) and sent by pressing **Enter**.

**Important:** Ensure your `telnet` client is in **line mode** (often the default, but sometimes requires setting `mode line` after connecting).

```bash
# 1. Connect to the server
telnet localhost 8090
```

Once connected, send commands by typing them and pressing **Enter**:

*   **Authenticate:**
    ```
    connect|your_username|your_password
    ```
    *(Expect `SUCCESS|CONNECTED`)*

*   **Subscribe:**
    ```
    subscribe|CURRENCY_PAIR
    ```
    *(Expect `SUCCESS|Subscribed to currency pair: CURRENCY_PAIR` and start receiving data)*

*   **View Data Stream:** (Pushed by server after successful subscription)
    ```
    CURRENCY_PAIR|B:BID_PRICE|A:ASK_PRICE|T:TIMESTAMP
    CURRENCY_PAIR|B:BID_PRICE|A:ASK_PRICE|T:TIMESTAMP
    ...
    ```

*   **Unsubscribe:**
    ```
    unsubscribe|CURRENCY_PAIR
    ```
    *(Expect `SUCCESS|Unsubscribed from currency pair: CURRENCY_PAIR`)*

*   **Disconnect:**
    ```
    disconnect
    ```
    *(Server closes the connection)*


### Server Responses

The server responds to client commands with status messages (`\r\n` terminated):

*   **`SUCCESS|CONNECTED`**: Authentication successful.
*   **`SUCCESS|Subscribed to currency pair: <PAIR>`**: Subscription successful.
*   **`SUCCESS|Unsubscribed from currency pair: <PAIR>`**: Unsubscription successful.
*   **`ERROR|Invalid command. Please enter one of these: connect,disconnect,subscribe,unsubscribe`**: Unknown command received.
*   **`ERROR|Not Authenticated`**: Command requires authentication, but client is not authenticated.
*   **`ERROR|Invalid credentials`**: Incorrect username or password during `connect`.
*   **`ERROR|Invalid currency pair: <PAIR>`**: Attempted to subscribe/unsubscribe from a pair not supported by the server.
*   **`ERROR|Invalid message format`**: Command structure (e.g., number of parts) is incorrect.
*   **`ERROR|User already logged in from another session`**: Attempted to `connect` with credentials already in use by another active connection.
*   **`INFO|Already subscribed to currency pair: <PAIR>`**: Tried to subscribe to an already subscribed pair.
*   **`INFO|Not subscribed to currency pair: <PAIR>`**: Tried to unsubscribe from a pair the client wasn't subscribed to.
*   **`INFO|User already connected`**: Tried to `connect` when the current connection is already authenticated.

## Contact

Yusuf okur - YusufOkr0@gmail.com

