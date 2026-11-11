# =============================================================================
# Makefile - Auto Center FIAP | Microsservico de Orcamento e Pagamento (Billing)
# =============================================================================
COMPOSE_FILE := docker/docker-compose.yml

.PHONY: help build test verify run infra-up infra-down tunnel docker-build

help: ## Mostra esta ajuda
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-14s\033[0m %s\n", $$1, $$2}'

build: ## Compila o projeto (sem testes)
	mvn clean package -DskipTests

test: ## Executa os testes unitarios + cobertura (JaCoCo)
	mvn clean test

verify: ## Roda testes + gate de cobertura (minimo 80%)
	mvn clean verify

run: ## Sobe a aplicacao localmente (profile dev, lendo .env)
	mvn spring-boot:run -Dspring-boot.run.profiles=dev

tunnel: ## Abre tunel publico ngrok na porta 8099 para receber Webhooks do Mercado Pago
	ngrok http 8099

infra-up: ## Sobe MySQL + Kafka locais (docker compose)
	docker compose -f $(COMPOSE_FILE) up -d

infra-down: ## Derruba a infraestrutura local
	docker compose -f $(COMPOSE_FILE) down

docker-build: ## Constroi a imagem Docker do servico
	docker build -f docker/Dockerfile -t autocenter-billing-service:latest .
