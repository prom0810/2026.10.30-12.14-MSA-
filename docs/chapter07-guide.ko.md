# 교재 7장 적용 안내

`spring-microservice/Chapter07`의 예제를 현재 실습 프로젝트에 맞춰 적용했다.
Spring Boot 4.0.9, Java 21, Gradle Wrapper와 기존 DTO의 Jackson 생성자,
불변 객체 매핑, DB 볼륨 및 로컬 바인딩 설정을 유지했다.
Spring Cloud는 Boot 4.0과 호환되는 2025.1.3을 사용한다.
호환성 표: https://spring.io/projects/spring-cloud/

## 변경 사항

- 조회: WebClient와 Mono/Flux를 사용한 리액티브 호출. 상품 조회 오류는 전달하며 추천·리뷰 조회 실패 시 부분 응답을 반환한다.
- 생성·삭제: composite가 CREATE/DELETE 이벤트를 발행하며 HTTP 202 Accepted를 반환한다.
- 코어 서비스: `messageProcessor`가 이벤트를 소비한다. 생성·삭제는 더 이상 코어 HTTP 엔드포인트가 아니다.
- MongoDB는 리액티브 저장소를 사용하고, JDBC 작업과 메시지 발행은 별도 bounded-elastic 스케줄러에서 실행한다.
- RabbitMQ/Kafka 바인더, 재시도, DLQ와 2개 파티션/소비자 인스턴스 프로필을 추가했다.
- `/actuator/health`에서 코어 서비스 상태를 확인한다.
- 삭제 완료를 기다리는 조합은 `Mono.when`으로 수정하여 빈 Mono 완료에 의해 다른 삭제 이벤트가 취소되지 않게 했다.
- 메시징 테스트, 이벤트 JSON 왕복 테스트, 리액티브 API 회귀 테스트와 매퍼 테스트를 반영했다.

202는 DB 반영 완료를 뜻하지 않는다. 생성 후 GET을 반복해서 반영을 확인해야 한다.
중복 생성 같은 소비자 오류는 재시도/DLQ로 처리되며 원래 POST 요청에 422로 반환되지 않는다.
`test-em-all.bash`는 이 동작에 맞춰 202와 비동기 반영 완료를 검사한다.

## 실행 (WSL)

Java 21, Docker Desktop의 WSL 통합, 실행 중인 Docker 엔진, curl, jq가 필요하다.

```bash
cd /home/user/projects/some-temp-folder
# Java가 PATH에 없다면 현재 설치된 JDK 사용
export JAVA_HOME=/home/user/.sdkman/candidates/java/21-tem
./gradlew build

# 기본 RabbitMQ
# 기존 환경이 있다면 동일한 구성으로 먼저 down한다. -v는 사용하지 않는다.
docker compose up -d --build
./test-em-all.bash
```

파티션과 Kafka 예제는 기본 파일에 덧붙이는 override가 아닌 독립 구성이다.
구성을 전환할 때는 이전 구성의 컨테이너를 먼저 내린다. DB 볼륨은 유지된다.

```bash
# RabbitMQ 두 인스턴스/파티션
export COMPOSE_FILE=docker-compose-partitions.yml
docker compose up -d --build
./test-em-all.bash

# 위 환경 종료 후 Kafka 예제
# docker compose down
# export COMPOSE_FILE=docker-compose-kafka.yml
# docker compose up -d --build
# ./test-em-all.bash
```

테스트 스크립트는 상품 ID 1, 13, 113, 213을 삭제·재생성한다.
Swagger UI: http://localhost:8080/openapi/swagger-ui.html
RabbitMQ 관리 UI: http://localhost:15672 (guest / guest)
Kafka 예제의 브로커 주소는 컨테이너 네트워크 내부 `kafka:9092` 기준이다.

## 검증 결과

- 전체 `testClasses` 컴파일 성공.
- 4개 서비스 `bootJar` 생성 성공.
- composite 전체 테스트, util 테스트, 3개 코어 서비스 매퍼 테스트: 23개 통과.
- 세 Compose 파일 `docker compose -f <파일> config --quiet` 통과.
- `bash -n test-em-all.bash` 통과.
- Docker 엔진이 실행되지 않았고 WSL Docker 통합도 사용할 수 없어 DB Testcontainers 테스트와 실제 RabbitMQ/Kafka 종단 테스트는 실행하지 못했다.

Docker 없이 실행한 검증 명령:

```bash
./gradlew testClasses :microservices:product-composite-service:test :util:test \
  :microservices:product-service:test --tests '*MapperTests' \
  :microservices:recommendation-service:test --tests '*MapperTests' \
  :microservices:review-service:test --tests '*MapperTests' bootJar
```

기존 `msa-persistence-guide.ko.md`는 이전 장의 동기 CRUD 설명이다.
7장 적용 후 생성·삭제 동작과 실행 방법은 이 문서를 기준으로 한다.
