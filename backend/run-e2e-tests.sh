#!/bin/bash

# Script para executar Testes End-to-End (E2E)
# Sistema Java - Spring Boot

echo "🚀 Iniciando Testes End-to-End (E2E) do Sistema Java"
echo "=================================================="

# Verificar se o Maven está disponível
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven não encontrado. Por favor, instale o Maven primeiro."
    exit 1
fi

# Verificar se o Java está disponível
if ! command -v java &> /dev/null; then
    echo "❌ Java não encontrado. Por favor, instale o Java primeiro."
    exit 1
fi

echo "✅ Maven e Java encontrados"

# Verificar se o Redis está rodando
echo "🔍 Verificando se o Redis está rodando..."
if ! redis-cli ping &> /dev/null; then
    echo "⚠️  Redis não está rodando. Iniciando Redis..."
    
    # Tentar iniciar Redis em background
    redis-server --daemonize yes --port 6379
    
    # Aguardar Redis iniciar
    sleep 3
    
    if ! redis-cli ping &> /dev/null; then
        echo "❌ Não foi possível iniciar o Redis. Por favor, inicie o Redis manualmente."
        echo "   Comando: redis-server"
        exit 1
    fi
fi

echo "✅ Redis está rodando"

# Limpar dados de teste anteriores
echo "🧹 Limpando dados de teste anteriores..."
redis-cli FLUSHDB &> /dev/null

# Executar testes E2E
echo "🧪 Executando Testes End-to-End..."
echo ""

# Opções de execução
E2E_OPTIONS="-Dspring.profiles.active=e2e -Dtest=*E2ETest"

# Verificar argumentos da linha de comando
if [ "$1" = "--verbose" ] || [ "$1" = "-v" ]; then
    E2E_OPTIONS="$E2E_OPTIONS -X"
    echo "📝 Modo verbose ativado"
fi

if [ "$1" = "--parallel" ] || [ "$1" = "-p" ]; then
    E2E_OPTIONS="$E2E_OPTIONS -T 4"
    echo "⚡ Execução paralela ativada"
fi

# Executar testes
echo "Executando: mvn verify $E2E_OPTIONS"
echo ""

if mvn verify $E2E_OPTIONS; then
    echo ""
    echo "✅ Testes End-to-End executados com sucesso!"
    echo ""
    echo "📊 Relatórios disponíveis em:"
    echo "   - target/surefire-reports/ (testes unitários)"
    echo "   - target/failsafe-reports/ (testes E2E)"
    echo ""
    echo "🎉 Todos os testes passaram!"
else
    echo ""
    echo "❌ Alguns testes falharam!"
    echo ""
    echo "📊 Verifique os relatórios em:"
    echo "   - target/surefire-reports/ (testes unitários)"
    echo "   - target/failsafe-reports/ (testes E2E)"
    echo ""
    echo "🔍 Para mais detalhes, execute:"
    echo "   mvn verify $E2E_OPTIONS -X"
    exit 1
fi

echo ""
echo "🏁 Execução dos testes E2E finalizada"
