#!/bin/bash

# Script para executar todos os testes e gerar relatório de cobertura
# Uso: ./run-tests.sh [tipo]
# Tipos: unit, integration, e2e, all (padrão)

set -e

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Função para log
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Verificar se Maven está instalado
if ! command -v mvn &> /dev/null; then
    error "Maven não está instalado ou não está no PATH"
    exit 1
fi

# Verificar se estamos no diretório correto
if [ ! -f "pom.xml" ]; then
    error "Arquivo pom.xml não encontrado. Execute este script no diretório raiz do projeto."
    exit 1
fi

# Tipo de teste (padrão: all)
TEST_TYPE=${1:-all}

log "Iniciando execução de testes - Tipo: $TEST_TYPE"

# Limpar builds anteriores
log "Limpando builds anteriores..."
mvn clean -q

# Verificar se a porta 8080 está livre
check_port() {
    if lsof -Pi :8080 -sTCP:LISTEN -t >/dev/null 2>&1; then
        warning "Porta 8080 está em uso. Tentando liberar..."
        
        # Encontrar e matar processo na porta 8080
        PID=$(lsof -ti:8080)
        if [ ! -z "$PID" ]; then
            kill -9 $PID 2>/dev/null || true
            sleep 2
            log "Processo na porta 8080 finalizado"
        fi
    fi
}

# Executar testes unitários
run_unit_tests() {
    log "Executando testes unitários..."
    mvn test -Dspring.profiles.active=dev-test -q
    
    if [ $? -eq 0 ]; then
        success "Testes unitários executados com sucesso"
    else
        error "Falha nos testes unitários"
        return 1
    fi
}

# Executar testes de integração
run_integration_tests() {
    log "Executando testes de integração..."
    check_port
    
    mvn test -Dtest="**/*IntegrationTest" -Dspring.profiles.active=dev-test -q
    
    if [ $? -eq 0 ]; then
        success "Testes de integração executados com sucesso"
    else
        error "Falha nos testes de integração"
        return 1
    fi
}

# Executar testes E2E
run_e2e_tests() {
    log "Executando testes E2E..."
    check_port
    
    mvn failsafe:integration-test -Dspring.profiles.active=dev-test -q
    
    if [ $? -eq 0 ]; then
        success "Testes E2E executados com sucesso"
    else
        error "Falha nos testes E2E"
        return 1
    fi
}

# Gerar relatório de cobertura
generate_coverage_report() {
    log "Gerando relatório de cobertura..."
    
    mvn jacoco:report -q
    
    if [ $? -eq 0 ]; then
        success "Relatório de cobertura gerado"
        
        # Verificar se o relatório foi gerado
        if [ -f "target/site/jacoco/index.html" ]; then
            log "Relatório disponível em: target/site/jacoco/index.html"
            
            # Extrair estatísticas de cobertura
            if command -v grep &> /dev/null && [ -f "target/site/jacoco/index.html" ]; then
                log "Estatísticas de cobertura:"
                echo "----------------------------------------"
                
                # Tentar extrair dados do relatório HTML
                if grep -q "Instructions" target/site/jacoco/index.html; then
                    INSTRUCTION_COVERAGE=$(grep -A1 "Instructions" target/site/jacoco/index.html | grep -o '[0-9]*%' | head -1)
                    BRANCH_COVERAGE=$(grep -A1 "Branches" target/site/jacoco/index.html | grep -o '[0-9]*%' | head -1)
                    
                    echo "Cobertura de Instruções: ${INSTRUCTION_COVERAGE:-N/A}"
                    echo "Cobertura de Branches: ${BRANCH_COVERAGE:-N/A}"
                fi
                
                echo "----------------------------------------"
            fi
        else
            warning "Arquivo de relatório não encontrado"
        fi
    else
        error "Falha ao gerar relatório de cobertura"
        return 1
    fi
}

# Verificar cobertura mínima
check_coverage() {
    log "Verificando cobertura mínima..."
    
    mvn jacoco:check -q
    
    if [ $? -eq 0 ]; then
        success "Cobertura atende aos critérios mínimos (80% instruções, 70% branches)"
    else
        warning "Cobertura não atende aos critérios mínimos"
        log "Critérios: 80% instruções, 70% branches"
        return 1
    fi
}

# Executar testes baseado no tipo
case $TEST_TYPE in
    "unit")
        run_unit_tests
        generate_coverage_report
        ;;
    "integration")
        run_integration_tests
        generate_coverage_report
        ;;
    "e2e")
        run_e2e_tests
        generate_coverage_report
        ;;
    "all")
        log "Executando todos os tipos de teste..."
        
        # Executar em sequência
        run_unit_tests || exit 1
        run_integration_tests || exit 1
        run_e2e_tests || exit 1
        
        # Gerar relatório final
        generate_coverage_report
        check_coverage
        ;;
    *)
        error "Tipo de teste inválido: $TEST_TYPE"
        echo "Tipos válidos: unit, integration, e2e, all"
        exit 1
        ;;
esac

# Resumo final
log "Execução de testes concluída!"

if [ "$TEST_TYPE" = "all" ]; then
    echo ""
    echo "========================================="
    echo "           RESUMO DOS TESTES"
    echo "========================================="
    echo "✓ Testes Unitários"
    echo "✓ Testes de Integração"
    echo "✓ Testes E2E"
    echo "✓ Relatório de Cobertura"
    echo "========================================="
    echo ""
    
    if [ -f "target/site/jacoco/index.html" ]; then
        log "Para visualizar o relatório de cobertura:"
        echo "  - Abra o arquivo: target/site/jacoco/index.html"
        echo "  - Ou execute: xdg-open target/site/jacoco/index.html"
    fi
fi

success "Script executado com sucesso!"