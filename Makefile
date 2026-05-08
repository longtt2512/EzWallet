# ============================================================================
# EzWallet - Lệnh tiện ích
# ============================================================================

.PHONY: help up down restart logs ps clean be-run be-test be-build fe-run fe-test fe-build

help: ## Hiển thị danh sách lệnh
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

# ----- Hạ tầng (Docker Compose) -----
up: ## Khởi động postgres, minio, redis, pgadmin, mailhog
	docker compose up -d
	@echo ""
	@echo "Hạ tầng đã sẵn sàng:"
	@echo "  PostgreSQL  : localhost:5432"
	@echo "  pgAdmin     : http://localhost:5050"
	@echo "  MinIO API   : http://localhost:9000"
	@echo "  MinIO UI    : http://localhost:9001"
	@echo "  Redis       : localhost:6379"
	@echo "  MailHog UI  : http://localhost:8025"

down: ## Dừng tất cả container
	docker compose down

restart: down up ## Khởi động lại

logs: ## Xem log realtime
	docker compose logs -f

ps: ## Trạng thái container
	docker compose ps

clean: ## Xoá toàn bộ volume (mất dữ liệu DB!)
	docker compose down -v

# ----- Backend -----
be-run: ## Chạy Spring Boot ở chế độ dev
	cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

be-test: ## Chạy unit test backend
	cd backend && ./mvnw test

be-build: ## Build jar backend
	cd backend && ./mvnw clean package -DskipTests

# ----- Frontend -----
fe-install: ## Cài deps frontend
	cd frontend && npm install

fe-run: ## Chạy Angular dev server
	cd frontend && npm start

fe-test: ## Chạy unit test frontend
	cd frontend && npm test

fe-build: ## Build production frontend
	cd frontend && npm run build
