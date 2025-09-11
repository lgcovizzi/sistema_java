package com.sistema.java.service;

import com.sistema.java.model.dto.UsuarioDTO;
import com.sistema.java.model.entity.Usuario;
import com.sistema.java.model.enums.PapelUsuario;
import com.sistema.java.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service para gerenciamento de usuários
 * 
 * @author Sistema Java
 * @version 1.0
 */
@Service
@Transactional
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Padrões de validação
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    
    private static final Pattern CPF_PATTERN = Pattern.compile(
        "^\\d{11}$"
    );
    
    private static final Pattern TELEFONE_PATTERN = Pattern.compile(
        "^\\(\\d{2}\\)\\s\\d{4,5}-\\d{4}$"
    );

    /**
     * Busca todos os usuários com paginação
     * 
     * @param pageable Configuração de paginação
     * @return Página de usuários
     */
    @Transactional(readOnly = true)
    public Page<UsuarioDTO> findAll(Pageable pageable) {
        return usuarioRepository.findAll(pageable)
                .map(this::convertToDTO);
    }

    /**
     * Busca usuário por ID
     * 
     * @param id ID do usuário
     * @return Optional com o usuário encontrado
     */
    @Transactional(readOnly = true)
    public Optional<UsuarioDTO> findById(Long id) {
        return usuarioRepository.findById(id)
                .map(this::convertToDTO);
    }

    /**
     * Busca usuário por email
     * 
     * @param email Email do usuário
     * @return Optional com o usuário encontrado
     */
    @Transactional(readOnly = true)
    public Optional<UsuarioDTO> findByEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .map(this::convertToDTO);
    }

    /**
     * Busca usuário por CPF
     * 
     * @param cpf CPF do usuário
     * @return Optional com o usuário encontrado
     */
    @Transactional(readOnly = true)
    public Optional<UsuarioDTO> findByCpf(String cpf) {
        return usuarioRepository.findByCpf(cpf)
                .map(this::convertToDTO);
    }

    /**
     * Busca usuário por token de verificação
     * 
     * @param token Token de verificação
     * @return Optional com o usuário encontrado
     */
    @Transactional(readOnly = true)
    public Optional<UsuarioDTO> findByTokenVerificacao(String token) {
        return usuarioRepository.findByTokenVerificacao(token)
                .map(this::convertToDTO);
    }

    /**
     * Busca usuário por token de reset de senha
     * 
     * @param token Token de reset de senha
     * @return Optional com o usuário encontrado
     */
    @Transactional(readOnly = true)
    public Optional<UsuarioDTO> findByTokenResetSenha(String token) {
        return usuarioRepository.findByTokenResetSenha(token)
                .map(this::convertToDTO);
    }

    /**
     * Busca usuários ativos
     * 
     * @param pageable Configuração de paginação
     * @return Página de usuários ativos
     */
    @Transactional(readOnly = true)
    public Page<UsuarioDTO> findAtivos(Pageable pageable) {
        return usuarioRepository.findByAtivo(true, pageable)
                .map(this::convertToDTO);
    }

    /**
     * Busca usuários por nome
     * 
     * @param nome Nome ou parte do nome
     * @param pageable Configuração de paginação
     * @return Página de usuários encontrados
     */
    @Transactional(readOnly = true)
    public Page<UsuarioDTO> findByNome(String nome, Pageable pageable) {
        return usuarioRepository.findByNomeContainingIgnoreCase(nome, pageable)
                .map(this::convertToDTO);
    }

    /**
     * Busca usuários por papel
     * 
     * @param papel Papel do usuário
     * @param pageable Configuração de paginação
     * @return Página de usuários encontrados
     */
    @Transactional(readOnly = true)
    public Page<UsuarioDTO> findByPapel(PapelUsuario papel, Pageable pageable) {
        return usuarioRepository.findByPapel(papel, pageable)
                .map(this::convertToDTO);
    }

    /**
     * Busca usuários ativos por papel
     * 
     * @param papel Papel do usuário
     * @param pageable Configuração de paginação
     * @return Página de usuários encontrados
     */
    @Transactional(readOnly = true)
    public Page<UsuarioDTO> findAtivosByPapel(PapelUsuario papel, Pageable pageable) {
        return usuarioRepository.findByPapelAndAtivo(papel, true, pageable)
                .map(this::convertToDTO);
    }

    /**
     * Busca usuários por email ou nome
     * 
     * @param termo Termo de busca
     * @param pageable Configuração de paginação
     * @return Página de usuários encontrados
     */
    @Transactional(readOnly = true)
    public Page<UsuarioDTO> buscar(String termo, Pageable pageable) {
        return usuarioRepository.buscarPorEmailOuNome(termo, pageable)
                .map(this::convertToDTO);
    }

    /**
     * Cria um novo usuário
     * 
     * @param usuarioDTO Dados do usuário
     * @return Usuário criado
     */
    public UsuarioDTO create(UsuarioDTO usuarioDTO) {
        // Validações
        validarDadosUsuario(usuarioDTO, null);
        
        if (usuarioRepository.existsByEmail(usuarioDTO.getEmail())) {
            throw new IllegalArgumentException("Email já está em uso");
        }
        
        if (StringUtils.hasText(usuarioDTO.getCpf()) && usuarioRepository.existsByCpf(usuarioDTO.getCpf())) {
            throw new IllegalArgumentException("CPF já está em uso");
        }

        Usuario usuario = convertToEntity(usuarioDTO);
        usuario.setSenha(passwordEncoder.encode(usuarioDTO.getSenha()));
        usuario.setDataCriacao(LocalDateTime.now());
        usuario.setDataAtualizacao(LocalDateTime.now());
        usuario.setAtivo(true);
        usuario.setEmailVerificado(false);
        
        // Gerar token de verificação
        usuario.setTokenVerificacao(UUID.randomUUID().toString());
        usuario.setDataExpiracaoToken(LocalDateTime.now().plusDays(7));
        
        // Definir papel padrão se não especificado
        if (usuario.getPapel() == null) {
            usuario.setPapel(PapelUsuario.USUARIO);
        }

        Usuario savedUsuario = usuarioRepository.save(usuario);
        return convertToDTO(savedUsuario);
    }

    /**
     * Atualiza um usuário
     * 
     * @param id ID do usuário
     * @param usuarioDTO Dados atualizados
     * @return Usuário atualizado
     */
    public UsuarioDTO update(Long id, UsuarioDTO usuarioDTO) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        // Validações
        validarDadosUsuario(usuarioDTO, id);
        
        if (!usuario.getEmail().equals(usuarioDTO.getEmail()) && 
            usuarioRepository.existsByEmailAndIdNot(usuarioDTO.getEmail(), id)) {
            throw new IllegalArgumentException("Email já está em uso");
        }
        
        if (StringUtils.hasText(usuarioDTO.getCpf()) && 
            !usuario.getCpf().equals(usuarioDTO.getCpf()) &&
            usuarioRepository.existsByCpfAndIdNot(usuarioDTO.getCpf(), id)) {
            throw new IllegalArgumentException("CPF já está em uso");
        }

        usuario.setNome(usuarioDTO.getNome());
        usuario.setSobrenome(usuarioDTO.getSobrenome());
        usuario.setEmail(usuarioDTO.getEmail());
        usuario.setCpf(usuarioDTO.getCpf());
        usuario.setTelefone(usuarioDTO.getTelefone());
        usuario.setDataNascimento(usuarioDTO.getDataNascimento());
        usuario.setDataAtualizacao(LocalDateTime.now());
        
        // Só atualizar papel se for fornecido
        if (usuarioDTO.getPapel() != null) {
            usuario.setPapel(usuarioDTO.getPapel());
        }

        Usuario savedUsuario = usuarioRepository.save(usuario);
        return convertToDTO(savedUsuario);
    }

    /**
     * Ativa ou desativa usuário
     * 
     * @param id ID do usuário
     * @param ativo Novo status
     * @return Usuário atualizado
     */
    public UsuarioDTO updateStatus(Long id, boolean ativo) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + id));

        usuario.setAtivo(ativo);
        usuario.setDataAtualizacao(LocalDateTime.now());

        Usuario savedUsuario = usuarioRepository.save(usuario);
        return convertToDTO(savedUsuario);
    }

    /**
     * Altera senha do usuário
     * 
     * @param id ID do usuário
     * @param novaSenha Nova senha
     * @return Usuário atualizado
     */
    public UsuarioDTO alterarSenha(Long id, String novaSenha) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + id));

        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuario.setDataAtualizacao(LocalDateTime.now());

        Usuario savedUsuario = usuarioRepository.save(usuario);
        return convertToDTO(savedUsuario);
    }

    /**
     * Remove usuário
     * 
     * @param id ID do usuário
     * @throws IllegalArgumentException se usuário não existir
     */
    public void delete(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new IllegalArgumentException("Usuário não encontrado: " + id);
        }
        usuarioRepository.deleteById(id);
    }

    /**
     * Conta usuários ativos
     * 
     * @return Número de usuários ativos
     */
    @Transactional(readOnly = true)
    public long countAtivos() {
        return usuarioRepository.countByAtivo(true);
    }

    /**
     * Conta usuários por papel
     * 
     * @param papel Papel do usuário
     * @return Número de usuários com o papel
     */
    @Transactional(readOnly = true)
    public long countByPapel(PapelUsuario papel) {
        return usuarioRepository.countByPapel(papel);
    }

    /**
     * Conta usuários verificados
     * 
     * @return Número de usuários verificados
     */
    @Transactional(readOnly = true)
    public long countVerificados() {
        return usuarioRepository.countByEmailVerificado(true);
    }

    /**
     * Conta total de usuários
     * 
     * @return Número total de usuários
     */
    @Transactional(readOnly = true)
    public long countTotal() {
        return usuarioRepository.count();
    }

    /**
     * Busca usuários mais ativos (com mais notícias)
     * 
     * @param limite Número máximo de usuários
     * @return Lista de usuários mais ativos
     */
    @Transactional(readOnly = true)
    public List<UsuarioDTO> findMaisAtivos(int limite) {
        Pageable pageable = Pageable.ofSize(limite);
        return usuarioRepository.findUsuariosMaisAtivos(pageable)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Busca usuários com notícias publicadas
     * 
     * @return Lista de usuários autores
     */
    @Transactional(readOnly = true)
    public List<UsuarioDTO> findAutores() {
        return usuarioRepository.findUsuariosComNoticiasPublicadas()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Verifica email do usuário
     * 
     * @param token Token de verificação
     * @return true se verificado com sucesso
     */
    public boolean verificarEmail(String token) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByTokenVerificacao(token);
        
        if (usuarioOpt.isEmpty()) {
            return false;
        }
        
        Usuario usuario = usuarioOpt.get();
        
        if (usuario.getDataExpiracaoToken().isBefore(LocalDateTime.now())) {
            return false;
        }
        
        usuarioRepository.updateEmailVerificadoById(usuario.getId(), true);
        return true;
    }

    /**
     * Atualiza último acesso do usuário
     * 
     * @param id ID do usuário
     */
    public void atualizarUltimoAcesso(Long id) {
        usuarioRepository.updateUltimoAcessoById(id, LocalDateTime.now());
    }

    /**
     * Limpa tokens expirados
     */
    @Transactional
    public void limparTokensExpirados() {
        usuarioRepository.limparTokensExpirados(LocalDateTime.now());
    }

    /**
     * Valida dados do usuário
     * 
     * @param usuarioDTO Dados do usuário
     * @param id ID do usuário (para updates)
     */
    private void validarDadosUsuario(UsuarioDTO usuarioDTO, Long id) {
        // Validar nome
        if (!StringUtils.hasText(usuarioDTO.getNome()) || usuarioDTO.getNome().trim().length() < 2) {
            throw new IllegalArgumentException("Nome deve ter pelo menos 2 caracteres");
        }
        
        // Validar email
        if (!StringUtils.hasText(usuarioDTO.getEmail()) || !EMAIL_PATTERN.matcher(usuarioDTO.getEmail()).matches()) {
            throw new IllegalArgumentException("Email inválido");
        }
        
        // Validar CPF se fornecido
        if (StringUtils.hasText(usuarioDTO.getCpf())) {
            String cpfLimpo = usuarioDTO.getCpf().replaceAll("[^0-9]", "");
            if (!CPF_PATTERN.matcher(cpfLimpo).matches() || !isValidCPF(cpfLimpo)) {
                throw new IllegalArgumentException("CPF inválido");
            }
            usuarioDTO.setCpf(cpfLimpo); // Salvar apenas números
        }
        
        // Validar telefone se fornecido
        if (StringUtils.hasText(usuarioDTO.getTelefone()) && 
            !TELEFONE_PATTERN.matcher(usuarioDTO.getTelefone()).matches()) {
            throw new IllegalArgumentException("Telefone deve estar no formato (XX) XXXXX-XXXX");
        }
        
        // Validar data de nascimento se fornecida
        if (usuarioDTO.getDataNascimento() != null) {
            LocalDate hoje = LocalDate.now();
            int idade = Period.between(usuarioDTO.getDataNascimento(), hoje).getYears();
            
            if (idade < 13) {
                throw new IllegalArgumentException("Usuário deve ter pelo menos 13 anos");
            }
            
            if (idade > 120) {
                throw new IllegalArgumentException("Data de nascimento inválida");
            }
        }
    }

    /**
     * Valida CPF usando algoritmo oficial
     * 
     * @param cpf CPF apenas com números
     * @return true se válido
     */
    private boolean isValidCPF(String cpf) {
        // Verifica se todos os dígitos são iguais
        if (cpf.matches("(\\d)\\1{10}")) {
            return false;
        }
        
        // Calcula primeiro dígito verificador
        int soma = 0;
        for (int i = 0; i < 9; i++) {
            soma += Character.getNumericValue(cpf.charAt(i)) * (10 - i);
        }
        int primeiroDigito = 11 - (soma % 11);
        if (primeiroDigito >= 10) primeiroDigito = 0;
        
        // Calcula segundo dígito verificador
        soma = 0;
        for (int i = 0; i < 10; i++) {
            soma += Character.getNumericValue(cpf.charAt(i)) * (11 - i);
        }
        int segundoDigito = 11 - (soma % 11);
        if (segundoDigito >= 10) segundoDigito = 0;
        
        // Verifica se os dígitos calculados conferem
        return Character.getNumericValue(cpf.charAt(9)) == primeiroDigito &&
               Character.getNumericValue(cpf.charAt(10)) == segundoDigito;
    }

    /**
     * Converte entidade para DTO
     * 
     * @param usuario Entidade
     * @return DTO
     */
    private UsuarioDTO convertToDTO(Usuario usuario) {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(usuario.getId());
        dto.setNome(usuario.getNome());
        dto.setSobrenome(usuario.getSobrenome());
        dto.setEmail(usuario.getEmail());
        dto.setCpf(usuario.getCpf());
        dto.setTelefone(usuario.getTelefone());
        dto.setDataNascimento(usuario.getDataNascimento());
        dto.setAvatar(usuario.getAvatar());
        dto.setPapel(usuario.getPapel());
        dto.setAtivo(usuario.isAtivo());
        dto.setEmailVerificado(usuario.isEmailVerificado());
        dto.setUltimoAcesso(usuario.getUltimoAcesso());
        dto.setDataCriacao(usuario.getDataCriacao());
        dto.setDataAtualizacao(usuario.getDataAtualizacao());
        return dto;
    }

    /**
     * Converte DTO para entidade
     * 
     * @param usuarioDTO DTO
     * @return Entidade
     */
    private Usuario convertToEntity(UsuarioDTO usuarioDTO) {
        Usuario usuario = new Usuario();
        usuario.setNome(usuarioDTO.getNome());
        usuario.setSobrenome(usuarioDTO.getSobrenome());
        usuario.setEmail(usuarioDTO.getEmail());
        usuario.setCpf(usuarioDTO.getCpf());
        usuario.setTelefone(usuarioDTO.getTelefone());
        usuario.setDataNascimento(usuarioDTO.getDataNascimento());
        usuario.setAvatar(usuarioDTO.getAvatar());
        usuario.setPapel(usuarioDTO.getPapel());
        return usuario;
    }
}