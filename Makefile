.PHONY: up down clean logs test collector collector-pull pack upload

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
	docker build -q -t umstieg-upload services/upload
	docker run --rm --entrypoint sh -v "$(CURDIR)/services/upload:/t:ro" umstieg-upload /t/test.sh

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

upload: .env
	docker compose --profile upload run --rm --build upload

.env:
	cp .env.example .env
	@echo "Created .env"
