.PHONY: all build test clean docker-build docker-server docker-dev run-server

all: build

build:
	./gradlew build

test:
	./gradlew test

clean:
	./gradlew clean

docker-build:
	docker compose build

docker-server:
	docker compose up -d server && docker compose logs -f server

docker-dev:
	docker compose run --rm dev

run-server:
	./gradlew runServer
