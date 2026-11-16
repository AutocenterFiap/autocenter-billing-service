# Kubernetes — Billing (Orçamento e Pagamento)

Manifestos para o deploy do microsserviço `autocenter-billing-service` no Kubernetes.

## Componentes

| Arquivo | Recurso |
|---|---|
| `namespace.yaml` | Namespace `auto-center-billing` |
| `configmap.yaml` | Config não-sensível (perfil, porta `8099`, Kafka, Mercado Pago modo/URL, logging) |
| `secret.example.yaml` | **Template** do Secret (credenciais MySQL, `SPRING_DATASOURCE_URL` e tokens do Mercado Pago) — não versione o real |
| `mysql-statefulset.yaml` + `mysql-service.yaml` | MySQL 8 (SQL) com PVC e service headless |
| `app-deployment.yaml` | Deployment da aplicação (init container aguarda o MySQL, anotações Datadog, probes) |
| `app-service.yaml` | Service NodePort (`30099` → `8099`) |
| `hpa.yaml` | HorizontalPodAutoscaler (CPU 70% / memória 80%, 2–10 réplicas) |
| `kustomization.yaml` | Orquestra a ordem de aplicação |

> O **Kafka** é a mensageria compartilhada do ecossistema; aponte `KAFKA_BOOTSTRAP_SERVERS`
> no `configmap.yaml` para o cluster Kafka existente (não é provisionado aqui).

## Pré-requisitos

1. Imagem publicada no registry e referenciada em `app-deployment.yaml`.
2. Criar o Secret (não versionado) antes do deploy:

```bash
kubectl create namespace auto-center-billing
kubectl create secret generic billing-secrets -n auto-center-billing \
  --from-literal=MYSQL_ROOT_PASSWORD='<senha-root>' \
  --from-literal=MYSQL_USER=billing \
  --from-literal=MYSQL_PASSWORD='<senha>' \
  --from-literal=SPRING_DATASOURCE_URL="jdbc:mysql://mysql-service:3306/billing?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true" \
  --from-literal=SPRING_DATASOURCE_USERNAME=billing \
  --from-literal=SPRING_DATASOURCE_PASSWORD='<senha>' \
  --from-literal=MERCADOPAGO_ACCESS_TOKEN='<token>' \
  --from-literal=MERCADOPAGO_PUBLIC_KEY='<public-key>'
```

## Deploy

```bash
kubectl apply -k k8s/
```

## Verificação

```bash
kubectl get pods -n auto-center-billing
kubectl get svc  -n auto-center-billing
# Health:  http://localhost:30099/actuator/health
# Swagger: http://localhost:30099/swagger-ui.html
```

