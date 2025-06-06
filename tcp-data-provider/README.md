<div align="center" class="text-center">
<h1>TCP FOREX DATA PROVIDER</h1>

<img alt="Java" src="https://img.shields.io/badge/Java-007396.svg?style=flat&logo=java&logoColor=white" class="inline-block mx-1" style="margin: 0px 2px;">
<img alt="Maven" src="https://img.shields.io/badge/Maven-C71A36.svg?style=flat&logo=apachemaven&logoColor=white" class="inline-block mx-1" style="margin: 0px 2px;">
<img alt="Docker" src="https://img.shields.io/badge/Docker-2496ED.svg?style=flat&logo=Docker&logoColor=white" class="inline-block mx-1" style="margin: 0px 2px;">
<br>

> A Java-based TCP server application that simulates and broadcasts real-time Foreign Exchange (FX) rate data to subscribed clients.

This project provides a TCP server built using *Java NIO* for handling client connections efficiently. It simulates FX rate fluctuations, including configurable volatility and periodic spikes, based on settings in
an `application.properties` file or environment variables. Clients can connect, authenticate, and subscribe to specific currency pairs to receive real-time updates.
</div>


##  Table of Contents

* [Core Components](#core-components)
* [Features](#features)
* [Installation](#installation)

    * [Quick Start (With Default Settings)](#quick-start-with-default-settings)
    * [Custom Setup](#custom-setup)
* [Usage (Testing with Telnet)](#usage-testing-with-telnet)
* [Contact](#contact)


## Core Components

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

## Installation

### Quick Start (With Default Settings)

This section guides you through launching the Application quickly using **Docker** (recommended). You’ll also learn how to override configuration and load custom initial FX rates using a Json file.

---

### Run with Docker

If Docker is installed on your system, you can get the application up and running with minimal setup.

#### 🔹 Pull the Docker Image

```bash
docker pull yusufokr0/tcp-data-provider
```

#### 🔹 Start the Container

```bash
docker run -d --name fx-provider -p 8090:8090 yusufokr0/tcp-data-provider
```

> These two step will start the application with the default settings by pulling the image from DockerHub.

---

## Custom setup

The application supports dynamic configuration through environment variables.

| Property Key               | Environment Variable                 | Description                                         |         |
| -------------------------- | ------------------------------------ | --------------------------------------------------- | ------- |
| `minimum.rate.change`      | `MINIMUM_RATE_CHANGE`                | Minimum bid/ask change per tick (decimal)           |         |
| `maximum.rate.change`      | `MAXIMUM_RATE_CHANGE`                | Maximum bid/ask change per tick (decimal)           |         |
| `publish.frequency`        | `PUBLISH_FREQUENCY`                  | Frequency of updates (in milliseconds)              |         |
| `spike.percentage`         | `SPIKE_PERCENTAGE`                   | Percent change during a spike (decimal)             |         |
| `spike.interval`           | `SPIKE_INTERVAL`                     | Interval (in ticks) between simulated spikes        |         |
| `user.credentials`         | `USER_CREDENTIALS`                   | Comma-separated list of valid users                 |         | 

### Load Initial FX Rates from Custom JSON (Optional)

By default, the application starts with a predefined set of currency rate data from an embedded `initial-rates.json` file. However, you can override this by providing your own JSON file inside the container at:

```
/conf/initial-rates.json
```

If the file exists at that location, it will be loaded at startup. Otherwise, the default configuration bundled inside the JAR will be used.

---

####  JSON Format Example

Here's an example of a valid `initial-rates.json` file:

```json
[
  {
    "rateName": "TCP_USDTRY",
    "bid": 38.4426,
    "ask": 38.8584,
    "minLimit": 37.9134,
    "maxLimit": 39.1324
  },
  {
    "rateName": "TCP_EURUSD",
    "bid": 1.1266,
    "ask": 1.1369,
    "minLimit": 1.10654,
    "maxLimit": 1.14654
  },
  {
    "rateName": "TCP_GBPUSD",
    "bid": 1.3273,
    "ask": 1.3573,
    "minLimit": 1.30245,
    "maxLimit": 1.34142
  }
]
```


###  Run with Docker + Custom configurations

To run the container using your own rates file:

If you want a more maintainable way to run the application with custom configurations and mounted files, you can use **Docker Compose**.

Here’s a sample `docker-compose.yml` file:

```yaml
version: '3.8'

services:
  fx-provider:
    image: yusufokr0/tcp-data-provider
    container_name: fx-provider
    ports:
      - "8090:8090"
    volumes:
      - ./path/to/your/own/config/file/initial-rates.json:/conf/initial-rates.json
    environment:
      SERVER_PORT: 8090
      PUBLISH_FREQUENCY: 7000
      MINIMUM_RATE_CHANGE: 0.001
      MAXIMUM_RATE_CHANGE: 0.002
      SPIKE_INTERVAL: 20
      SPIKE_PERCENTAGE: 0.011
      USER_CREDENTIALS: user|pass,admin|admin,custom|custom
```

#### ➤ Run it:

```bash
docker-compose up -d
```



## Usage (Testing with Telnet)

Interact with the server using `telnet`. Commands are pipe-delimited (`|`) and sent by pressing **Enter**.

**Important:** Ensure your `telnet` client is in **line mode** (Requires setting `mode line` after connecting).

```bash
# 1. Connect to the server
telnet server_ip_address server_port
```

Once connected, send commands by typing them and pressing **Enter**:

*   **Authenticate:**
    ```
    connect|your_username|your_password
    ```
*   **Subscribe:**
    ```
    subscribe|CURRENCY_PAIR
    ```
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

*   **Disconnect:**
    ```
    disconnect
    ```


### Server Responses

The server responds to client commands with status messages:

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

