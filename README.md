<h1 align="center">Toyota-32bit-Forex-data-Integration-Project</h1>

[![Kafka](https://img.shields.io/badge/Kafka-Message%20Queue-blue.svg)](https://kafka.apache.org/)
[![Redis](https://img.shields.io/badge/Redis-Data%20Store-red.svg)](https://redis.io/)
[![Java](https://img.shields.io/badge/Java-Programming%20Language-green.svg)](https://www.oracle.com/java/)
[![Docker](https://img.shields.io/badge/Docker-Supported-blue.svg)](https://www.docker.com/)

This project aims to develop a comprehensive Java-based software solution for collecting, integrating, evaluating, and calculating financial data from multiple real-time data providers in global financial markets, with a focus on Forex (foreign exchange) data.

## **Project Scope**
The project includes the following components:

### **Forex Data Simulation platforms**
*  `Tcp-data-provider`: TCP socket-based streaming data. [See detailed info](./tcp-data-provider/README.md)
*  `Rest-data-provider`: REST API-based data on request.

### **Forex-data-collector**
* A central application to collect, process, calculate, and publish financial data, built with a Spring Boot infrastructure.

### **Kafka-db-consumer**
*  Consuming valid raw data from data provider platforms, as well as derived data resulting from computations, and persisting them into the database.

### **Kafka-opensearch-consumer**
* Not available right now.

### **Redis**
* Cache used to temporarily store raw and calculated exchange rates in `forex-data-collector` project.
* Also used for exchange rate repository in `rest-data-provider` project.

## **Project Architecture**

<div align="center">
  <img alt="Project-Architecture2" src="./assets/Project-Architecture-2.png" width="800"/>
</div>


## **Installation**

* **COMING SOON**