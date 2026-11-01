.PHONY: up down clean logs test collector collector-pull pack upload download analyse

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

download: .env
	test -n "$(DAY)"
	mkdir -p data/download
	docker compose --profile upload run --rm --build -v ./data/download:/data/download --entrypoint sh upload \
		-c 'hf download "$$HF_DATASET" --repo-type dataset --include "*/*-$(DAY).*" --local-dir /data/download'

analyse: .env
	test -n "$(ARCHIVE)"
	ARCHIVE=$(ARCHIVE) docker compose --profile analyse run --rm analyse

.env:
	cp .env.example .env
	@echo "Created .env"
