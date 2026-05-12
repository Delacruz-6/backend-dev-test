# Backend Dev Test — Similar Products API

REST API que expone productos similares para un producto dado, construida con Spring Boot y WebFlux.

## Tech Stack

- Java 17
- Spring Boot 3.x
- Spring WebFlux (reactive, non-blocking)
- Lombok

## Architecture

La app orquesta dos APIs existentes (puerto 3001) para construir la respuesta:

1. `GET /product/{id}/similarids` → obtiene los IDs de productos similares
2. `GET /product/{id}` → obtiene el detalle de cada producto (en paralelo)

Y expone:

3. `GET /product/{id}/similar` → **el endpoint que implementa esta app** (puerto 5000)

## API Contract

`GET /product/{productId}/similar`

**Response 200:**
```json
[
  {
    "id": "2",
    "name": "Dress",
    "price": 19.99,
    "availability": true
  }
]
```

**Response 404:** si el producto raíz no existe.

Ver contrato completo en [similarProducts.yaml](./similarProducts.yaml).

## Run locally

```bash
./mvnw spring-boot:run
```

La app arranca en el **puerto 5000**.

Requiere el mock corriendo en el puerto 3001. Ver sección de tests.

## Test con k6 (requiere Docker)

```bash
# Levantar mocks e infraestructura
docker-compose up -d simulado influxdb grafana

# Ejecutar test de carga
docker-compose run --rm k6 run scripts/test.js
```

Ver resultados en: http://localhost:3000/d/Le2Ku9NMk/k6-performance-test

## Decisiones técnicas

- **WebFlux + flatMapSequential** para paralelizar las llamadas de detalle por cada producto similar preservando el orden de similitud
- Si un producto individual falla (404, 500 o timeout), se omite sin romper el flujo principal
- URL base del mock externalizada en `application.properties`
- Manejo global de errores con `@ControllerAdvice`

## Comportamiento ante errores

| Escenario | Respuesta |
|---|---|
| Producto principal no existe (`/similarids` → 404) | `404 Not Found` |
| Producto individual falla (404 o 500) | Se omite del resultado, el resto se devuelve |
| Producto individual lento (timeout) | Se omite del resultado, el resto se devuelve |
| Timeout de conexión con el mock | `504 Gateway Timeout` |

## Tests

```bash
./mvnw test
```

Cubre con `@WebFluxTest` + `MockWebServer` todos los escenarios de error.

## Notas de configuración Maven

El proyecto usa **Spring Boot 3.3.6** (la versión más reciente estable disponible en el momento del desarrollo).

Maven Central tenía la IP bloqueada en el entorno de desarrollo, por lo que se configuró un mirror alternativo en `.mvn/settings.xml` apuntando a `repo.huaweicloud.com`, que replica el repositorio central. Para compilar o ejecutar el proyecto en un entorno sin esta restricción, el `settings.xml` puede ignorarse:

```bash
# Con mirror (si Maven Central está bloqueado)
mvn spring-boot:run -s .mvn/settings.xml

# Sin mirror (entorno normal)
mvn spring-boot:run
```
