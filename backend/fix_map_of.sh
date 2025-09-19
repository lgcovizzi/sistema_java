#!/bin/bash

# Script para corrigir Map.of() em arquivos Java de forma robusta

# Função para corrigir Map.of() complexos
fix_map_of_in_file() {
    local file="$1"
    echo "Processando $file"
    
    # Backup do arquivo
    cp "$file" "$file.bak"
    
    # Usar Python para fazer a substituição mais precisa
    python3 -c "
import re
import sys

def fix_map_of(content):
    # Padrão para Map.of(...) com múltiplos argumentos
    pattern = r'Map\.of\s*\(\s*([^)]+)\s*\)'
    
    def replace_map_of(match):
        args = match.group(1)
        # Dividir argumentos por vírgula, mas cuidado com vírgulas dentro de strings
        args_list = []
        current_arg = ''
        paren_count = 0
        in_string = False
        escape_next = False
        
        for char in args:
            if escape_next:
                current_arg += char
                escape_next = False
                continue
                
            if char == '\\\\':
                escape_next = True
                current_arg += char
                continue
                
            if char == '\"' and not escape_next:
                in_string = not in_string
                current_arg += char
                continue
                
            if not in_string:
                if char in '([{':
                    paren_count += 1
                elif char in ')]}':
                    paren_count -= 1
                elif char == ',' and paren_count == 0:
                    args_list.append(current_arg.strip())
                    current_arg = ''
                    continue
                    
            current_arg += char
            
        if current_arg.strip():
            args_list.append(current_arg.strip())
        
        # Criar HashMap com puts
        if len(args_list) % 2 != 0:
            return match.group(0)  # Retorna original se número ímpar de argumentos
            
        puts = []
        for i in range(0, len(args_list), 2):
            key = args_list[i]
            value = args_list[i+1] if i+1 < len(args_list) else '\"\"'
            puts.append(f'put({key}, {value});')
            
        return f'new HashMap<>() {{ {\" \".join(puts)} }}'
    
    return re.sub(pattern, replace_map_of, content)

with open('$file', 'r') as f:
    content = f.read()

fixed_content = fix_map_of(content)

with open('$file', 'w') as f:
    f.write(fixed_content)
"
    
    if [ $? -eq 0 ]; then
        echo "✓ Corrigido: $file"
        rm "$file.bak"
    else
        echo "✗ Erro ao corrigir: $file"
        mv "$file.bak" "$file"
    fi
}

# Corrigir todos os arquivos Java
find src/main/java -name "*.java" | while read file; do
    if grep -q "Map\.of(" "$file"; then
        fix_map_of_in_file "$file"
    fi
done

echo "Correção concluída!"