.PHONY: up down clean logs test

up: .env
	docker compose up -d --wait

down:
	docker compose down

clean:
	docker compose down -v

logs:
	docker compose logs -f

test:
	./gradlew build

.env:
	cp .env.example .env
	@echo "Created .env"
