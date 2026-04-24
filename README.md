# fullstack-mock-data-service

Servicio Spring Boot WebFlux para administrar datos mock de usuarios y cargar mocks por scripts SQL.

## Stack

- Java 21
- Spring Boot 3 (WebFlux, JDBC, Security)
- H2
- Gradle Wrapper (`./gradlew`)

## Requisitos

- JDK 21
- Bash o terminal compatible

## Ejecutar local

```bash
./gradlew bootRun
```

El servicio inicia por defecto en `http://localhost:8080`.

## Configuracion clave

Archivo: `src/main/resources/application.yaml`

- `app-security.mock-bearer-token`: token mock esperado en `Authorization: Bearer <token>`
- `user-mocks.auto-load-scripts`: habilita recarga automatica de scripts al iniciar
- `user-mocks.scripts-dir`: carpeta de scripts SQL
- `spring.datasource.url`: base H2

## Seguridad obligatoria

Todas las solicitudes requieren:

- Header `Authorization` con formato `Bearer <token>`
- Header `Source-Bank`
- Header `Application-Id`

Si faltan headers obligatorios o el token no es valido, el servicio responde con `400` o `401`.

## Seed automatico al arranque

Al iniciar, si `user-mocks.auto-load-scripts=true`, el servicio ejecuta scripts `.sql` en `user-mocks.scripts-dir`.

Script seed incluido:

- `third-parties/users-mocks/sql/001_seed_api_mocks.sql`

## Endpoints principales

Base path: `/api`

- `GET /user-mocks`: lista mocks
- `GET /user-mocks/{id}`: obtiene mock por id
- `POST /user-mocks`: crea mock
- `PUT /user-mocks/{id}`: actualiza mock
- `DELETE /user-mocks/{id}`: elimina mock
- `POST /user-mocks/reload`: recarga scripts desde directorio configurado
- `POST /user-mocks/load-sql`: carga SQL enviado en el body

Health:

- `GET /actuator/health`

## Ejemplos de uso (curl)

```bash
curl -i "http://localhost:8080/api/user-mocks" \
  -H "Authorization: Bearer mock-token" \
  -H "Source-Bank: BANK-TEST" \
  -H "Application-Id: APP-TEST"
```

```bash
curl -i -X POST "http://localhost:8080/api/user-mocks" \
  -H "Authorization: Bearer mock-token" \
  -H "Source-Bank: BANK-TEST" \
  -H "Application-Id: APP-TEST" \
  -H "Content-Type: application/json" \
  -d '{"name":"demo-user","description":"creado por curl"}'
```

```bash
curl -i -X POST "http://localhost:8080/api/user-mocks/load-sql" \
  -H "Authorization: Bearer mock-token" \
  -H "Source-Bank: BANK-TEST" \
  -H "Application-Id: APP-TEST" \
  -H "Content-Type: application/json" \
  -d '{"sql":"INSERT INTO user_mocks (name, description, created_at, updated_at) VALUES (''sql-user'', ''from curl'', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);","replaceScripted":true}'
```

## Pruebas

```bash
./gradlew test
```

## Postman

Coleccion incluida:

- `postman/fullstack-mock-data-service.postman_collection.json`

Importa la coleccion y ajusta variables (`baseUrl`, `bearerToken`, `sourceBank`, `applicationId`) segun tu entorno.

## Autor
- [Raul Bolivar](https://linkedin.com/in/rasysbox/)
- [GitHub](https://github.com/raulrobinson/fullstack-mock-data-service)
- [Email](mailto:rasysbox@hotmail.com)

## Licencia
MIT License. Ver LICENSE.txt para detalles.

