.PHONY: up down clean logs test

# Start everything and wait until all services are healthy.
up: .env
	docker compose up -d --wait

down:
	docker compose down

# Also deletes the database and Kafka volumes.
clean:
	docker compose down -v

logs:
	docker compose logs -f

test:
	./gradlew build

.env:
	cp .env.example .env
	@echo "Created .env from .env.example - edit it before relying on it."
