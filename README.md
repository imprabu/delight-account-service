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

## Postman collection

- Collection: `postman/Delight Account Service.postman_collection.json`
- Environment: `postman/Delight Local.postman_environment.json`

Usage:
- Import both files into Postman, select the "Delight Local" environment, and verify `{{baseUrl}}` points to your running instance (default `http://localhost:8080`).
- Set `companyName` to your company (e.g., "Acme Corp"). The collection auto-derives `companyDomain` by lowercasing and removing spaces (e.g., `acmecorp`).
- Signup uses localhost: send `POST {{baseUrl}}/account/signup`.
- All other APIs require the Host header to be the company domain. The collection sends `Host: {{companyDomain}}` automatically for those requests while still targeting `{{baseUrl}}`.
- Use the "Signin" request (with Host header) to obtain a JWT and set it in the environment variable `jwt`. Several endpoints also accept the `hrsession` cookie which is set by the signin response.
- The OpenAPI spec is also served at `http://localhost:8080/v3/api-docs` while the app is running and can be imported directly into Postman if you prefer Postman’s auto-generated collections.

## Channel APIs

Channel endpoints under `/channel/**` are authenticated using a ChannelJWT and secured with `ROLE_CHANNEL`. These endpoints operate at the account level without a user session.

### Generate a ChannelJWT

Use the helper script and copy the token output:

```bash
python3 create_channel_jwt.py
# Copy the value printed after "Token:" and use it as the Bearer token
```

### Quick curl tests

Set the token and base URL once, then call channel endpoints:

```bash
TOKEN="<paste-token-from-script>"
BASE="http://127.0.0.1:8080"

# Account info (Channel-safe: no user context)
curl -s -o - -w '\nHTTP %{http_code}\n' \
  -H "Authorization: Bearer $TOKEN" \
  "$BASE/channel/account/info"

# Membership list (Channel-safe)
curl -s -o - -w '\nHTTP %{http_code}\n' \
  -H "Authorization: Bearer $TOKEN" \
  "$BASE/channel/account/members"

# Session validation (Channel)
curl -s -o - -w '\nHTTP %{http_code}\n' \
  -H "Authorization: Bearer $TOKEN" \
  "$BASE/channel/validateSession"
```

Expected: HTTP 200 with account-level data when the token is valid.

### Postman collection for Channel APIs

- Collection: `postman/Delight Channel.postman_collection.json`
- Environment: `postman/Delight Channel Local.postman_environment.json`

Usage:
- Import the collection and environment.
- Select the "Delight Channel Local" environment.
- Set `channel_token` to the value printed by `create_channel_jwt.py`.
- Ensure `baseUrl` points to your running instance (default `http://127.0.0.1:8080`).
- Run requests under the Channel folders; each uses collection-level Bearer auth with `{{channel_token}}` and includes simple tests for HTTP 200.

### Troubleshooting

- 401 Unauthorized: Token missing/expired/invalid. Regenerate with `create_channel_jwt.py` and retry.
- 403 Forbidden: Hitting `/channel/**` without `ROLE_CHANNEL`. Ensure you send `Authorization: Bearer <ChannelJWT>`.
- Port in use: If the app fails to start because `8080` is busy, run `lsof -nP -iTCP:8080 -sTCP:LISTEN` and kill the reported PID, then `./gradlew bootRun`.

## Rate limiting: how to test

Global, public endpoints (signup, signin, activation) are rate limited. Defaults are configured in `src/main/resources/application.properties`:

- Activation: `rate.limit.activation.max.requests=5` within `rate.limit.activation.window.seconds=300`
- Signup: `rate.limit.signup.max.requests=10` within `rate.limit.signup.window.seconds=3600`
- Signin: `rate.limit.signin.max.requests=20` within `rate.limit.signin.window.seconds=3600`

These can be overridden via environment variables (e.g., `RATE_LIMIT_ACTIVATION_MAX_REQUESTS`, `RATE_LIMIT_ACTIVATION_WINDOW_SECONDS`).

Postman Runner (easy path)
- Use the collection group “Auth” → request “Activate” (or “Activate (by accountId)”).
- Open Postman Collection Runner, set iterations to 6–10, and run quickly.
- The built-in tests on activation requests assert HTTP 200 or 429 and verify `code=error.rate.limit.exceeded` for 429.

Curl loop (manual path)

```
# Set once per shell session
BASE=http://localhost:8080
HOST=acmecorp       # company primary domain (lowercased, no spaces)

for i in {1..6}; do
  curl -s -o /dev/null -w "Iteration $i => HTTP %{http_code}\n" \
    -H "Host: $HOST" \
    -H "Content-Type: application/json" \
    -d '{
          "emailAddress":"owner@acme.com",
          "firstName":"Alice",
          "lastName":"Smith",
          "phoneNumber":"+1-555-123-4567",
          "password":"SecureTest123!",
          "retypePassword":"SecureTest123!"
        }' \
    "$BASE/account/activate"
done
```

Expected: first requests return 200; subsequent ones within the window return 429 with JSON body containing `code: "error.rate.limit.exceeded"`.
