# delight-account-service
The service is developed to manage account signup and account-plan informations.

## Database
This service uses [TiDB](https://tidb.io) for persistence. Because TiDB is
compatible with the MySQL protocol, the application relies on the standard
MySQL JDBC driver (`com.mysql:mysql-connector-j`) to connect. The default JDBC
URL is `jdbc:mysql://localhost:4000/delight-accounts?createDatabaseIfNotExist=true&characterEncoding=UTF-8`.
The extra `characterEncoding=UTF-8` option ensures the database stores four byte characters such as emojis correctly.

## Configuration
The application reads its configuration directly from environment variables at
runtime. The key settings are:

- `DB_URL` – the JDBC connection URL used to reach TiDB
- `DB_USER` – the database username
- `DB_PASSWORD` – the database password used for connecting to TiDB
- `LOG_LEVEL` – the root logging level (defaults to `INFO` when unset)
- `LOG_DIR` – folder where log files are written (defaults to `logs` for local runs)
- `PASSWORD_KEY`, `JWT_SECRET`, `JWT_EXPIRATION` – security settings for JWTs

Two helper files, `envVariables_staging` and `envVariables_prod`, provide sample
values. You can source one of them before running the service so the variables
are exported into your shell:

```bash
set -a
source envVariables_staging   # or envVariables_prod
set +a
./gradlew bootRun
```

Gradle also looks at the `ENV` variable when executing `bootRun`. If it is set
to `staging` or `prod` (the default is `staging`) the matching
`envVariables_<env>` file is loaded automatically, without overwriting any
variables that are already present in your environment.

Log4j2 reads the same environment variables when configuring appenders. If you
change `LOG_DIR`, ensure the directory exists; the Docker entrypoint creates it
automatically when running inside a container.

## Running in containers
The application now runs as a standalone container while TiDB and Redis remain
in Kubernetes. Ensure that the container host can reach the Kubernetes network
so the application can access those services.

### Build the container image
Build the application using the provided `Dockerfile` and push it to your
registry:

```bash
docker build -t myregistry/delight-account-service:latest .
docker push myregistry/delight-account-service:latest
```

If you prefer Podman, the commands are identical apart from the executable
name:

```bash
podman build -t myregistry/delight-account-service:latest .
podman push myregistry/delight-account-service:latest
```

You can also use Spring Boot's buildpacks support:

```bash
./gradlew bootBuildImage --imageName=myregistry/delight-account-service:latest
```

`bootBuildImage` talks to a Docker-compatible API; when using Podman ensure the
`podman-docker` shim is installed so the daemon socket is available. The
`bootRun` task simply starts the application locally and does **not** build a
container image.

### Run the container
Start the container and provide the configuration needed to connect to TiDB (and
any other dependencies) via environment variables. For Docker:

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

The same flags work with Podman:

```bash
podman run --rm -p 8080:8080 \
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
