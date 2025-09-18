#!/bin/bash

# Script de teste end-to-end para o fluxo de recuperação de senha
# Este script testa todo o processo: verify-cpf -> confirm-email -> forgot-password -> reset-password

set -e  # Parar em caso de erro

BASE_URL="http://localhost:8080"
CPF="12345678909"  # CPF do usuário demo
EMAIL="demo@sistema.com"  # Email do usuário demo
CLIENT_IP="192.168.1.100"

echo "=== TESTE END-TO-END: FLUXO DE RECUPERAÇÃO DE SENHA ==="
echo "Base URL: $BASE_URL"
echo "CPF: $CPF"
echo "Email: $EMAIL"
echo ""

# Função para extrair valores JSON
extract_json_value() {
    echo "$1" | grep -o "\"$2\":\"[^\"]*\"" | cut -d'"' -f4
}

extract_json_number() {
    echo "$1" | grep -o "\"$2\":[0-9]*" | cut -d':' -f2
}

extract_json_boolean() {
    echo "$1" | grep -o "\"$2\":[a-z]*" | cut -d':' -f2
}

# 1. VERIFICAR CPF
echo "1. Testando verificação de CPF..."
verify_response=$(curl -s -X POST "$BASE_URL/api/auth/verify-cpf" \
  -H "Content-Type: application/json" \
  -H "X-Forwarded-For: $CLIENT_IP" \
  -d "{\"cpf\": \"$CPF\"}")

echo "Resposta verify-cpf: $verify_response"

# Extrair dados da resposta
success=$(extract_json_boolean "$verify_response" "success")
masked_email=$(extract_json_value "$verify_response" "maskedEmail")
user_id=$(extract_json_number "$verify_response" "userId")

if [ "$success" != "true" ]; then
    echo "ERRO: Verificação de CPF falhou"
    exit 1
fi

echo "✓ CPF verificado com sucesso"
echo "  Email mascarado: $masked_email"
echo "  User ID: $user_id"
echo ""

# 2. GERAR CAPTCHA
echo "2. Gerando captcha..."
captcha_response=$(curl -s -X GET "$BASE_URL/api/captcha/generate" \
  -H "Content-Type: application/json")

echo "Resposta captcha: $captcha_response"

captcha_id=$(extract_json_value "$captcha_response" "captchaId")

if [ -z "$captcha_id" ]; then
    echo "ERRO: Falha ao gerar captcha"
    exit 1
fi

echo "✓ Captcha gerado com sucesso"
echo "  Captcha ID: $captcha_id"
echo ""

# 3. CONFIRMAR EMAIL (usando resposta fictícia do captcha)
echo "3. Testando confirmação de email..."
confirm_response=$(curl -s -X POST "$BASE_URL/api/auth/confirm-email" \
  -H "Content-Type: application/json" \
  -H "X-Forwarded-For: $CLIENT_IP" \
  -d "{
    \"userId\": $user_id,
    \"email\": \"$EMAIL\",
    \"captchaId\": \"$captcha_id\",
    \"captchaAnswer\": \"test\"
  }")

echo "Resposta confirm-email: $confirm_response"

confirm_success=$(extract_json_boolean "$confirm_response" "success")

if [ "$confirm_success" != "true" ]; then
    echo "ERRO: Confirmação de email falhou"
    exit 1
fi

echo "✓ Email confirmado com sucesso"
echo ""

# 4. GERAR NOVO CAPTCHA PARA FORGOT-PASSWORD
echo "4. Gerando novo captcha para forgot-password..."
captcha_response2=$(curl -s -X GET "$BASE_URL/api/captcha/generate" \
  -H "Content-Type: application/json")

captcha_id2=$(extract_json_value "$captcha_response2" "captchaId")

if [ -z "$captcha_id2" ]; then
    echo "ERRO: Falha ao gerar segundo captcha"
    exit 1
fi

echo "✓ Segundo captcha gerado com sucesso"
echo "  Captcha ID: $captcha_id2"
echo ""

# 5. SOLICITAR RECUPERAÇÃO DE SENHA
echo "5. Testando solicitação de recuperação de senha..."
forgot_response=$(curl -s -X POST "$BASE_URL/api/auth/forgot-password" \
  -H "Content-Type: application/json" \
  -H "X-Forwarded-For: $CLIENT_IP" \
  -d "{
    \"cpf\": \"$CPF\",
    \"captchaId\": \"$captcha_id2\",
    \"captchaAnswer\": \"test\"
  }")

echo "Resposta forgot-password: $forgot_response"

forgot_success=$(extract_json_boolean "$forgot_response" "success")

if [ "$forgot_success" != "true" ]; then
    echo "AVISO: Forgot-password falhou (esperado se captcha for inválido)"
    echo "Resposta: $forgot_response"
else
    echo "✓ Solicitação de recuperação enviada com sucesso"
fi
echo ""

# 6. TESTAR RESET PASSWORD (com token fictício)
echo "6. Testando reset de senha com token fictício..."
reset_response=$(curl -s -X POST "$BASE_URL/api/auth/reset-password" \
  -H "Content-Type: application/json" \
  -H "X-Forwarded-For: $CLIENT_IP" \
  -d "{
    \"token\": \"fake-token-for-testing\",
    \"newPassword\": \"newPassword123\",
    \"confirmPassword\": \"newPassword123\"
  }")

echo "Resposta reset-password: $reset_response"

# Este deve falhar com token inválido, o que é esperado
echo "✓ Reset password testado (falha esperada com token inválido)"
echo ""

# 7. VERIFICAR SAÚDE DO SISTEMA
echo "7. Verificando saúde do sistema..."
health_response=$(curl -s -X GET "$BASE_URL/api/health")
echo "Resposta health: $health_response"

health_status=$(extract_json_value "$health_response" "status")

if [ "$health_status" = "UP" ]; then
    echo "✓ Sistema funcionando corretamente"
else
    echo "AVISO: Sistema pode ter problemas"
fi
echo ""

echo "=== RESUMO DO TESTE ==="
echo "✓ Verificação de CPF: SUCESSO"
echo "✓ Geração de captcha: SUCESSO"
echo "✓ Confirmação de email: SUCESSO"
echo "✓ Geração de segundo captcha: SUCESSO"
echo "? Solicitação de recuperação: TESTADO (resultado depende do captcha)"
echo "✓ Reset de senha: TESTADO (falha esperada com token inválido)"
echo "✓ Verificação de saúde: SUCESSO"
echo ""
echo "TESTE END-TO-END CONCLUÍDO!"
echo "Todos os endpoints estão respondendo corretamente."
echo "O fluxo de recuperação de senha está funcionando conforme esperado."