# delight-account-service
The service is developed to manage account signup and account-plan informations.

## Database
This service uses [TiDB](https://tidb.io) for persistence. Because TiDB is
compatible with the MySQL protocol, the application relies on the standard
MySQL JDBC driver (`com.mysql:mysql-connector-j`) to connect. The default JDBC
URL is `jdbc:mysql://localhost:4000/delight-accounts?createDatabaseIfNotExist=true&characterEncoding=UTF-8`.
The extra `characterEncoding=UTF-8` option ensures TiDB uses the standard UTF-8 charset.

## Configuration
The application uses a single `application.properties` file. During the build
process Gradle copies values from an environment specific file into this
properties file. Two sample files are provided:

- `envVariables_staging`
- `envVariables_prod`

Select which one to use by setting the `ENV` environment variable before
running the build:

```bash
export ENV=staging   # or "prod"
./gradlew bootRun
```

Each `envVariables_*` file defines values for:

- `DB_URL` – the JDBC connection URL
- `DB_USER` – the database username
- `DB_PASSWORD` – the database password used for connecting to TiDB
- `LOG_LEVEL` – the root logging level
- `LOG_DIR` – folder where log files are written

Logging is handled by Log4j2 using the YAML file `log4j2-spring.yml`. Log files
rotate daily and are stored in the folder specified by `LOG_DIR`.

To run the application:

```bash
./gradlew bootRun
```
