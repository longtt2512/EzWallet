.PHONY: help dev dev-perf up down restart logs ps clean be-run be-run-perf be-test be-build fe-install fe-run fe-test fe-build

help: ## Show available commands
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

# ----- One-command local dev -----
dev: ## Start everything: infra + backend + frontend (Ctrl+C stops all)
	@[ -f .env ] || (echo "Creating .env from .env.example..." && cp .env.example .env)
	@lsof -ti :8080 | xargs kill -9 2>/dev/null && echo "Stopped existing process on :8080" || true
	@lsof -ti :4200 | xargs kill -9 2>/dev/null && echo "Stopped existing process on :4200" || true
	docker compose up -d
	@echo ""
	@echo "Services:  http://localhost:8080/api/v1  |  http://localhost:4200"
	@echo "Press Ctrl+C to stop backend and frontend."
	@echo ""
	@if [ ! -d frontend/node_modules ]; then cd frontend && npm install --silent; fi
	@trap 'kill 0' EXIT; \
	 (cd backend && ./gradlew bootRun --args='--spring.profiles.active=dev' 2>&1 | sed 's/^/[be] /') & \
	 (cd frontend && npm start 2>&1 | sed 's/^/[fe] /') & \
	 wait

dev-perf: ## Start with PERF_MODE=true (fixed OTP=123456 for load testing)
	@[ -f .env ] || (echo "Creating .env from .env.example..." && cp .env.example .env)
	@lsof -ti :8080 | xargs kill -9 2>/dev/null && echo "Stopped existing process on :8080" || true
	@lsof -ti :4200 | xargs kill -9 2>/dev/null && echo "Stopped existing process on :4200" || true
	docker compose up -d
	@echo ""
	@echo "⚠️  PERF_MODE=true - All OTPs will be 123456"
	@echo "Services:  http://localhost:8080/api/v1  |  http://localhost:4200"
	@echo "Press Ctrl+C to stop backend and frontend."
	@echo ""
	@if [ ! -d frontend/node_modules ]; then cd frontend && npm install --silent; fi
	@trap 'kill 0' EXIT; \
	 (export PERF_MODE=true && cd backend && ./gradlew bootRun --args='--spring.profiles.active=dev' 2>&1 | sed 's/^/[be] /') & \
	 (cd frontend && npm start 2>&1 | sed 's/^/[fe] /') & \
	 wait

# ----- Infrastructure (Docker Compose) -----
up: ## Start postgres, minio, redis, pgadmin, mailhog
	docker compose up -d
	@echo ""
	@echo "Infrastructure ready:"
	@echo "  PostgreSQL  : localhost:5432"
	@echo "  pgAdmin     : http://localhost:5050"
	@echo "  MinIO API   : http://localhost:9000"
	@echo "  MinIO UI    : http://localhost:9001"
	@echo "  Redis       : localhost:6379"
	@echo "  MailHog UI  : http://localhost:8025"

down: ## Stop all containers
	docker compose down

restart: down up ## Restart all containers

logs: ## Stream container logs
	docker compose logs -f

ps: ## Show container status
	docker compose ps

clean: ## Remove all volumes (destroys DB data!)
	docker compose down -v

# ----- Backend -----
be-run: ## Run Spring Boot in dev mode
	cd backend && ./gradlew bootRun --args='--spring.profiles.active=dev'

be-run-perf: ## Run Spring Boot in performance mode (fixed OTP=123456)
	@echo "⚠️  PERF_MODE=true - All OTPs will be 123456"
	export PERF_MODE=true && cd backend && ./gradlew bootRun --args='--spring.profiles.active=dev'

be-test: ## Run backend unit tests
	cd backend && ./gradlew test

be-build: ## Build backend jar
	cd backend && ./gradlew clean bootJar

# ----- Frontend -----
fe-install: ## Install frontend dependencies
	cd frontend && npm install

fe-run: ## Run Angular dev server
	cd frontend && npm start

fe-test: ## Run frontend unit tests
	cd frontend && npm test

fe-build: ## Build frontend for production
	cd frontend && npm run build
