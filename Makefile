.PHONY: up down clean logs test collector

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

collector: .env
	mkdir -p data/raw
	docker compose --profile collector up -d --build collector

.env:
	cp .env.example .env
	@echo "Created .env"
