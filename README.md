# delight-account-service
The service is developed to manage account signup and account-plan informations.

## Database
This service uses [TiDB](https://tidb.io) for persistence. Because TiDB is
compatible with the MySQL protocol, the application relies on the standard
MySQL JDBC driver (`com.mysql:mysql-connector-j`) to connect. The default JDBC
URL is `jdbc:mysql://localhost:4000/delight-accounts?createDatabaseIfNotExist=true&characterEncoding=UTF-8`.
The extra `characterEncoding=UTF-8` option ensures the database stores four byte characters such as emojis correctly.

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

Logging is handled by Log4j2 using the YAML file `log4j2-spring.xml`. Log files
rotate daily and are stored in the folder specified by `LOG_DIR`.

Values from the selected `envVariables_*` file are substituted into
`application.properties` **and** `log4j2-spring.xml` when Gradle processes
resources. Ensure the `ENV` variable is set before running any build task so the
resulting jar contains the resolved `LOG_DIR` value. After running
`processResources` you should see the `fileName` and `filePattern` entries in
`build/resources/main/log4j2-spring.xml` expanded with that directory instead of
`${LOG_DIR}`. The `LOG_DIR` property itself is also resolved in the
`Properties` section. For example:

```bash
ENV=prod ./gradlew bootJar
```

To run the application:

```bash
./gradlew bootRun
```

## Running with Docker
The application service now runs as a standalone Docker container while TiDB and
Redis continue to be hosted in Kubernetes. Ensure that the Docker host can reach
the Kubernetes cluster network so the application can access those services.

### Build the container image
Build the application using the provided `Dockerfile` and push it to your
registry:

```bash
docker build -t myregistry/delight-account-service:latest .
docker push myregistry/delight-account-service:latest
```

You can also build an image with buildpacks using Gradle:

```bash
./gradlew bootBuildImage --imageName=myregistry/delight-account-service:latest
```

### Run the container
Start the container and provide the configuration needed to connect to TiDB (and
any other dependencies) via environment variables. For example:

```bash
docker run --rm -p 8080:8080 \
  -e DB_URL="jdbc:mysql://<tidb-host>:4000/delight-accounts?createDatabaseIfNotExist=true&characterEncoding=UTF-8" \
  -e DB_USER=root \
  -e DB_PASSWORD=secret \
  -e LOG_LEVEL=INFO \
  -e LOG_DIR=/var/log/app \
  -e PASSWORD_KEY=sampleSecretKey123 \
  -e JWT_SECRET=sampleJwtSecret123sampleJwtSecret123 \
  -e JWT_EXPIRATION=86400000 \
  myregistry/delight-account-service:latest
```

Adjust the values to match the endpoints exposed by your Kubernetes-hosted
TiDB and Redis instances. Mount a volume at `LOG_DIR` if you need to persist log
files outside of the container.
