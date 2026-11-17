# 9장 Eureka 서비스 디스커버리

기존 7장 메시징 기능에 Eureka 서버와 서비스 이름 기반 HTTP 조회를 병합했다.

- 서버: `spring-cloud/eureka-server`, 포트 8761, `@EnableEurekaServer`.
- 버전: Spring Boot 4.0.9, Java 21, Spring Cloud 2025.1.3.
- 등록 이름: `product`, `recommendation`, `review`, `product-composite`.
- 각 서비스의 `application.yml`: 로컬은 `http://localhost:8761/eureka/`, docker 프로필은 `http://eureka:8761/eureka/`.
- composite: `@LoadBalanced WebClient.Builder` 하나를 주입하며 `http://product`, `http://recommendation`, `http://review`를 사용한다.
- 기존 `app.*-service.host/port` 설정은 제거했다. 실제 API 경로(`/product/{id}` 등)는 유지한다.
- 생성·삭제는 기존 RabbitMQ/Kafka 이벤트를 그대로 사용한다.
- 세 Compose 파일에 Eureka를 추가했다. 파티션 인스턴스도 동일한 서비스 이름으로 등록한다.

## 실행

Docker 엔진을 실행하고 WSL Docker 통합을 활성화한 뒤 WSL에서 실행한다.

```bash
cd /home/user/projects/some-temp-folder
export JAVA_HOME=/home/user/.sdkman/candidates/java/21-tem
./gradlew build
docker compose up -d --build
./test-em-all.bash
```

Eureka 대시보드: http://localhost:8761
등록 목록(JSON): `curl -H 'Accept: application/json' http://localhost:8761/eureka/apps`
Swagger UI: http://localhost:8080/openapi/swagger-ui.html

서비스 시작 직후에는 등록과 클라이언트 캐시 갱신에 시간이 걸릴 수 있다.
`depends_on`은 서버 컨테이너 시작 순서만 보장하며 등록 완료까지 보장하지 않는다.
테스트 스크립트는 composite의 health가 준비될 때까지 기다린다.

파티션/Kafka 구성을 사용할 때는 이전 구성을 종료하고 다음 중 하나를 선택한다.
DB 볼륨을 유지하려면 `down`에 `-v`를 붙이지 않는다.

```bash
export COMPOSE_FILE=docker-compose-partitions.yml
# 또는 export COMPOSE_FILE=docker-compose-kafka.yml
docker compose up -d --build
./test-em-all.bash
```

## 테스트

마이크로서비스의 `@SpringBootTest`에는 `eureka.client.enabled=false`를 추가했다.
서버 자체 테스트는 Eureka를 켜고 실제 HTTP 등록 목록과 health 응답을 검사한다.
`ServiceDiscoveryTests`는 Eureka 대신 고정된 테스트 레지스트리를 사용해 실제
LoadBalanced WebClient가 `product` 이름을 임시 HTTP 서버 주소로 해석하는지 검사한다.

Docker 없이 실행 가능한 검증:

```bash
./gradlew testClasses :spring-cloud:eureka-server:test \
  :microservices:product-composite-service:test :util:test \
  :microservices:product-service:test --tests '*MapperTests' \
  :microservices:recommendation-service:test --tests '*MapperTests' \
  :microservices:review-service:test --tests '*MapperTests' bootJar
```

DB Testcontainers 및 실제 DB·브로커·Eureka 전체 환경 검증은 실행 중인 Docker가 필요하다.
