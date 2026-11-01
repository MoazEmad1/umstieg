.PHONY: up down clean logs test collector collector-pull pack sync

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

collector-pull: .env
	mkdir -p data/raw
	docker compose --profile collector pull collector
	docker compose --profile collector up -d --no-build collector

pack: .env
	mkdir -p data/archive
	docker compose --profile pack run --rm pack

sync: .env
	docker compose --profile sync run --rm sync

.env:
	cp .env.example .env
	@echo "Created .env"
