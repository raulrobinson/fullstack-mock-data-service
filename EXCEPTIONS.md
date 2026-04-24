# 🧠 PRINCIPIO CLAVE ARQUITECTURA HEXAGONAL: Excepciones

En arquitectura hexagonal:

***❗ Las excepciones nacen en el dominio, se adaptan en infraestructura y se presentan en la entrada (API).***

---

### 🏗️ CAPAS Y EXCEPCIONES

---

🟢 DOMINIO (Core / UseCases)

👉 Aquí defines tus propias `excepciones de negocio`

**✔ Qué excepciones van aquí**
- Reglas de negocio
- Validaciones
- Casos funcionales

Ejemplos:
```java
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
```

```java
public class ParameterNotFoundException extends BusinessException {
    public ParameterNotFoundException(String key) {
        super("Parameter not found: " + key);
    }
}
```

**🚫 Qué NO va aquí**
- ❌ HTTP (400, 404, etc.)
- ❌ WebFlux
- ❌ DynamoDB
- ❌ AWS

---

🔵 PUERTOS (Interfaces)

👉 NO lanzan excepciones propias

Pero sí definen contratos que pueden fallar:

```java
public interface ParameterRepository {
    Mono<Parameter> findById(String id);
}
```
Aquí no lanzamos `ParameterNotFoundException`, sino que devolvemos un `Mono.empty()` o un error genérico.

👉 Las excepciones se manejan en implementaciones (adaptadores)

---

🟡 ADAPTADORES DE SALIDA (Driven)

👉 Aquí traduces `errores técnicos` → `errores de dominio`

Ejemplo con DynamoDB:
```java
@Override
public Mono<Parameter> findById(String id) {
    return dynamoClient.getItem(...)
        .map(this::toDomain)
        .switchIfEmpty(Mono.error(new ParameterNotFoundException(id)))
        .onErrorMap(e -> new TechnicalException("Dynamo error", e));
}
```

**✔ Qué excepciones usar aquí**
- TechnicalException
- Adaptación de `errores externos`

Ejemplo:
```java
public class TechnicalException extends RuntimeException {
    public TechnicalException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

---

🟠 CASOS DE USO (Application)

👉 Orquestan lógica y pueden propagar excepciones

Ejemplo:
```java
public Mono<Parameter> execute(String id) {
    return repository.findById(id)
        .switchIfEmpty(Mono.error(new ParameterNotFoundException(id)));
}
```

**👉 No transforman a HTTP aún**

---

🔴 ADAPTADOR DE ENTRADA (Controller / Handler WebFlux)

Aquí es donde entra:

👉 Spring WebFlux Handler / Router Function

**Opcion A: Manejo Local**
```java
return useCase.execute(id)
    .map(ResponseEntity::ok)
    .onErrorResume(ParameterNotFoundException.class,
        e -> Mono.just(ResponseEntity.notFound().build()))
    .onErrorResume(TechnicalException.class,
        e -> Mono.just(ResponseEntity.status(500).build()));
```

**Opcion B: Manejo Global con WebFlux**
```java
@Component
@Order(-2)
public class GlobalErrorHandler extends AbstractErrorWebExceptionHandler {

    public GlobalErrorHandler(...) {
        super(...);
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {

        Throwable error = getError(request);

        if (error instanceof ParameterNotFoundException) {
            return ServerResponse.status(404)
                .bodyValue(Map.of("error", error.getMessage()));
        }

        if (error instanceof BusinessException) {
            return ServerResponse.status(400)
                .bodyValue(Map.of("error", error.getMessage()));
        }

        return ServerResponse.status(500)
            .bodyValue(Map.of("error", "Internal Server Error"));
    }
}
```

---

### 🧠 RESUMEN

| Capa              | Tipo de excepción  | Ejemplo           |
|-------------------|--------------------|-------------------|
| Dominio           | BusinessException  | ParameterNotFound |
| Aplicación        | Propaga            | Mono.error(...)   |
| Adaptador salida  | TechnicalException | Dynamo error      |
| Adaptador entrada | HTTP mapping       | 404 / 400 / 500   |

---

### Patron Correcto
```text
Dynamo error → TechnicalException
    No existe → ParameterNotFoundException
        Validación → BusinessException
                        ↓
                  Global Handler
                        ↓
                  HTTP Response
```

---

**⚠️ ERRORES COMUNES (EVÍTALOS)**

- ❌ Lanzar ResponseStatusException en dominio
- ❌ Mezclar HTTP en casos de uso
- ❌ Dejar errores técnicos sin mapear
- ❌ Usar try/catch en reactive (usa onErrorResume)

---

**🧠 REGLA DE ORO**

- 🟢 Dominio NO conoce infraestructura
- 🔴 Infraestructura adapta errores
- 🟣 Entrada decide respuesta HTTP

