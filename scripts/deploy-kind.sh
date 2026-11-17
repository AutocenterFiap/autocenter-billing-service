#!/usr/bin/env bash
set -euo pipefail

CLUSTER="${1:?Uso: $0 <cluster-kind> <tag-da-imagem>}"
TAG="${2:?Uso: $0 <cluster-kind> <tag-da-imagem>}"
CTX="kind-${CLUSTER}"
IMAGEM=autocenter-billing-service
NAMESPACE=auto-center-billing
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
K="kubectl --context ${CTX}"

for VAR in MYSQL_ROOT_PASSWORD MYSQL_USER MYSQL_PASSWORD SPRING_DATASOURCE_URL \
    MERCADOPAGO_ACCESS_TOKEN MERCADOPAGO_PUBLIC_KEY SISTEMA_SEGURANCA_CHAVE_SECRETA; do
  if [ -z "${!VAR:-}" ]; then
    echo "::error::Variavel ${VAR} nao definida para o deploy em ${CLUSTER}."
    exit 1
  fi
done

if ! kubectl config get-contexts -o name | grep -qx "${CTX}"; then
  echo "::error::Contexto ${CTX} nao existe nesta maquina. Abortando antes de qualquer acao."
  exit 1
fi

docker build -f "${RAIZ}/docker/Dockerfile" -t "${IMAGEM}:${TAG}" "${RAIZ}"
kind load docker-image "${IMAGEM}:${TAG}" --name "${CLUSTER}"

${K} apply -f "${RAIZ}/k8s/namespace.yaml"
${K} create secret generic billing-secrets \
  --namespace="${NAMESPACE}" \
  --from-literal=MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD}" \
  --from-literal=MYSQL_USER="${MYSQL_USER}" \
  --from-literal=MYSQL_PASSWORD="${MYSQL_PASSWORD}" \
  --from-literal=SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL}" \
  --from-literal=SPRING_DATASOURCE_USERNAME="${MYSQL_USER}" \
  --from-literal=SPRING_DATASOURCE_PASSWORD="${MYSQL_PASSWORD}" \
  --from-literal=MERCADOPAGO_ACCESS_TOKEN="${MERCADOPAGO_ACCESS_TOKEN}" \
  --from-literal=MERCADOPAGO_PUBLIC_KEY="${MERCADOPAGO_PUBLIC_KEY}" \
  --from-literal=SISTEMA_SEGURANCA_CHAVE_SECRETA="${SISTEMA_SEGURANCA_CHAVE_SECRETA}" \
  --save-config --dry-run=client -o yaml | ${K} apply -f -

TMP="$(mktemp -d)"
trap 'rm -rf "${TMP}"' EXIT
cp -r "${RAIZ}/k8s" "${TMP}/k8s"
printf '\nimages:\n  - name: %s\n    newTag: "%s"\n' "${IMAGEM}" "${TAG}" >> "${TMP}/k8s/kustomization.yaml"
${K} apply -k "${TMP}/k8s"
${K} -n "${NAMESPACE}" rollout status "deployment/${IMAGEM}" --timeout=300s
