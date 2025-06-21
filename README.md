# delight-account-service
The service is developed to manage account signup and account-plan informations.

## Database
This service uses [TiDB](https://tidb.io) for persistence. Because TiDB is
compatible with the MySQL protocol, the application relies on the standard
MySQL JDBC driver (`com.mysql:mysql-connector-j`) to connect. The default JDBC
URL is `jdbc:mysql://localhost:4000/delight-accounts?createDatabaseIfNotExist=true`.

## Configuration
The application reads a few settings from the environment:

- `DB_PASSWORD` sets the database password used for connecting to TiDB.
- `LOG_LEVEL` controls the root logging level (defaults to `INFO`).
- `LOG_DIR` defines where log files are written (defaults to a `logs` folder).

Logging is handled by Log4j2 using the YAML file `log4j2-spring.yml`. Log files
rotate daily and are stored in the folder specified by `LOG_DIR`.

Additional property files provide defaults for specific environments:

- `application-staging.properties`
- `application-prod.properties`

Gradle reads the `ENV` environment variable to determine which profile is active.
Set it before running the application:

```bash
export ENV=staging
./gradlew bootRun
```
