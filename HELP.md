# Getting Started

### Reference Documentation
For further reference, please consider the following sections:

* [Official Gradle documentation](https://docs.gradle.org)
* [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/3.4.6/gradle-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/3.4.6/gradle-plugin/packaging-oci-image.html)
* [Spring Boot DevTools](https://docs.spring.io/spring-boot/3.4.6/reference/using/devtools.html)
* [Spring Configuration Processor](https://docs.spring.io/spring-boot/3.4.6/specification/configuration-metadata/annotation-processor.html)
* [Docker Compose Support](https://docs.spring.io/spring-boot/3.4.6/reference/features/dev-services.html#features.dev-services.docker-compose)
* [Spring Web](https://docs.spring.io/spring-boot/3.4.6/reference/web/servlet.html)
* [OAuth2 Client](https://docs.spring.io/spring-boot/3.4.6/reference/web/spring-security.html#web.security.oauth2.client)
* [JDBC API](https://docs.spring.io/spring-boot/3.4.6/reference/data/sql.html)
* [Spring Data JPA](https://docs.spring.io/spring-boot/3.4.6/reference/data/sql.html#data.sql.jpa-and-spring-data)
* [Liquibase Migration](https://docs.spring.io/spring-boot/3.4.6/how-to/data-initialization.html#howto.data-initialization.migration-tool.liquibase)
* [Spring Data Redis (Access+Driver)](https://docs.spring.io/spring-boot/3.4.6/reference/data/nosql.html#data.nosql.redis)
* [Spring for Apache Kafka](https://docs.spring.io/spring-boot/3.4.6/reference/messaging/kafka.html)
* [Apache Kafka Streams Support](https://docs.spring.io/spring-kafka/reference/streams.html)
* [Apache Kafka Streams Binding Capabilities of Spring Cloud Stream](https://docs.spring.io/spring-cloud-stream/reference/kafka/kafka-streams-binder/usage.html)
* [Spring Batch](https://docs.spring.io/spring-boot/3.4.6/how-to/batch.html)

### Guides
The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)
* [Accessing Relational Data using JDBC with Spring](https://spring.io/guides/gs/relational-data-access/)
* [Managing Transactions](https://spring.io/guides/gs/managing-transactions/)
* [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)
* [Accessing data with MySQL](https://spring.io/guides/gs/accessing-data-mysql/)
  (TiDB is MySQL compatible)
* [Messaging with Redis](https://spring.io/guides/gs/messaging-redis/)
* [Samples for using Apache Kafka Streams with Spring Cloud stream](https://github.com/spring-cloud/spring-cloud-stream-samples/tree/master/kafka-streams-samples)
* [Creating a Batch Service](https://spring.io/guides/gs/batch-processing/)

### Additional Links
These additional references should also help you:

* [Gradle Build Scans – insights for your project's build](https://scans.gradle.com#gradle)

### Docker Compose support
This project contains a Docker Compose file named `compose.yaml`.
In this file, the following services have been defined:

* tidb: [`pingcap/tidb:latest`](https://hub.docker.com/r/pingcap/tidb)
* redis: [`redis:latest`](https://hub.docker.com/_/redis)

Please review the tags of the used images and set them to the same as you're running in production.

