package br.com.fateczs.seazs.unit.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.repository.UsuarioRepository;
import br.com.fateczs.seazs.security.AutenticacaoConfig;
import br.com.fateczs.seazs.security.Autoridades;
import br.com.fateczs.seazs.security.JwtConfig;
import br.com.fateczs.seazs.security.JwtProperties;
import br.com.fateczs.seazs.security.JwtTokenService;
import br.com.fateczs.seazs.security.ProblemDetailSecurityHandlers;
import br.com.fateczs.seazs.security.RefreshTokens;
import br.com.fateczs.seazs.security.SecurityConfig;
import br.com.fateczs.seazs.security.SeazsUserDetailsService;
import br.com.fateczs.seazs.security.UsuarioAutenticado;
import br.com.fateczs.seazs.utils.TipoUsuario;
import tools.jackson.databind.json.JsonMapper;

/** Pecas de seguranca testadas em isolamento (sem Spring): encoder, hash legado, tokens, handlers e configuracao. */
@ExtendWith(MockitoExtension.class)
public class SegurancaUnitTest {

	private static final String SEGREDO = "segredo-de-teste-com-32-bytes-ou-mais!!";

	@Mock
	private UsuarioRepository repository;
	@InjectMocks
	private SeazsUserDetailsService userDetails;

	private static JwtProperties props(String segredo) {
		return new JwtProperties(segredo, "seazs", Duration.ofMinutes(15), Duration.ofDays(7));
	}

	private static Usuario usuario(String senha) {
		Usuario u = new Usuario();
		u.setId(3);
		u.setEmail("ana@teste.com");
		u.setSenha(senha);
		u.setTipoUsuario(TipoUsuario.PROFESSOR);
		u.setAdm(false);
		return u;
	}

	// ---- PasswordEncoder / migracao de senhas

	private final PasswordEncoder encoder = new AutenticacaoConfig().passwordEncoder();

	@Test
	public void encoderGravaBcryptComPrefixoEVerifica() {
		String hash = encoder.encode("Segredo123");

		assertTrue(hash.startsWith("{bcrypt}$2a$"));
		assertTrue(encoder.matches("Segredo123", hash));
		assertFalse(encoder.matches("SEGREDO123", hash)); // diferente do collation case-insensitive do MySQL
		assertFalse(encoder.upgradeEncoding(hash));
	}

	@Test
	public void senhaLegadaSemPrefixoELidaComoNoopEPedeUpgrade() {
		UserDetails legado = new UsuarioAutenticado(usuario("Segredo123"));

		assertEquals("{noop}Segredo123", legado.getPassword());
		assertTrue(encoder.matches("Segredo123", legado.getPassword()));
		assertFalse(encoder.matches("segredo123", legado.getPassword())); // case-sensitive
		assertTrue(encoder.upgradeEncoding(legado.getPassword()));
	}

	@Test
	public void senhaLegadaQueComecaComChavesNaoEConfundidaComPrefixoDeHash() {
		UserDetails legado = new UsuarioAutenticado(usuario("{noop}abc"));

		assertEquals("{noop}{noop}abc", legado.getPassword());
		assertTrue(encoder.matches("{noop}abc", legado.getPassword()));
	}

	@Test
	public void senhaJaEmBcryptNaoRecebePrefixoNovo() {
		String hash = encoder.encode("x");

		assertEquals(hash, new UsuarioAutenticado(usuario(hash)).getPassword());
	}

	@Test
	public void bcryptRejeitaSenhaAcimaDe72Bytes() {
		assertThrows(IllegalArgumentException.class, () -> new BCryptPasswordEncoder().encode("a".repeat(73)));
		assertThrows(IllegalArgumentException.class, () -> encoder.encode("a".repeat(73)));
		assertTrue(encoder.encode("a".repeat(72)).startsWith("{bcrypt}"));
	}

	@Test
	public void encoderNaoReconheceAlgoritmosFracosLegados() {
		assertThrows(IllegalArgumentException.class, () -> encoder.matches("x", "{MD5}abcdef"));
	}

	// ---- UserDetailsService / upgrade

	@Test
	public void carregaUsuarioPorEmail() {
		when(repository.findByEmail("ana@teste.com")).thenReturn(Optional.of(usuario("{bcrypt}h")));

		UserDetails detalhes = userDetails.loadUserByUsername("ana@teste.com");

		assertEquals("ana@teste.com", detalhes.getUsername());
		assertEquals(Arrays.asList("ROLE_PROFESSOR"),
				detalhes.getAuthorities().stream().map(Object::toString).collect(java.util.stream.Collectors.toList()));
	}

	@Test
	public void emailInexistenteLancaUsernameNotFound() {
		when(repository.findByEmail("x@x.com")).thenReturn(Optional.empty());

		assertThrows(UsernameNotFoundException.class, () -> userDetails.loadUserByUsername("x@x.com"));
	}

	@Test
	public void updatePasswordPersisteOHashNovo() {
		Usuario u = usuario("Segredo123");
		when(repository.save(u)).thenReturn(u);

		UserDetails atualizado = userDetails.updatePassword(new UsuarioAutenticado(u), "{bcrypt}NOVO");

		verify(repository).save(u);
		assertEquals("{bcrypt}NOVO", u.getSenha());
		assertEquals("{bcrypt}NOVO", atualizado.getPassword());
	}

	// ---- Autoridades

	@Test
	public void autoridadesCombinamTipoEAdm() {
		Usuario u = usuario("x");
		u.setAdm(true);
		assertEquals(Arrays.asList("ROLE_PROFESSOR", "ROLE_ADMIN"), Autoridades.de(u));

		u.setTipoUsuario(null);
		u.setAdm(null);
		assertEquals(Collections.emptyList(), Autoridades.de(u));
	}

	// ---- RefreshTokens

	@Test
	public void refreshTokenOpacoEUnicoEOHashEHexSha256() {
		String a = RefreshTokens.gerar();
		String b = RefreshTokens.gerar();

		assertNotEquals(a, b);
		assertTrue(a.length() >= 43);
		assertEquals(64, RefreshTokens.hash(a).length());
		assertEquals(RefreshTokens.hash(a), RefreshTokens.hash(a));
		assertNotEquals(RefreshTokens.hash(a), RefreshTokens.hash(b));
		// vetor conhecido de SHA-256("abc")
		assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", RefreshTokens.hash("abc"));
	}

	// ---- JwtConfig / JwtTokenService

	@Test
	public void segredoAusenteOuCurtoFalhaNaPartida() {
		IllegalStateException vazio = assertThrows(IllegalStateException.class, () -> new JwtConfig().jwtEncoder(props("")));
		assertTrue(vazio.getMessage().contains("JWT_SECRET"));
		assertThrows(IllegalStateException.class, () -> new JwtConfig().jwtDecoder(props("curto-demais")));
		assertThrows(IllegalStateException.class, () -> new JwtConfig().jwtDecoder(props("a".repeat(31))));
		assertThrows(IllegalStateException.class, () -> new JwtConfig().jwtDecoder(props(null)));
		new JwtConfig().jwtDecoder(props("a".repeat(32)));
	}

	@Test
	public void ttlNaoPositivoFalhaNaPartida() {
		JwtProperties zero = new JwtProperties(SEGREDO, "seazs", Duration.ZERO, Duration.ofDays(1));
		assertThrows(IllegalStateException.class, () -> new JwtConfig().jwtEncoder(zero));
		JwtProperties negativo = new JwtProperties(SEGREDO, "seazs", Duration.ofMinutes(1), Duration.ofDays(-1));
		assertThrows(IllegalStateException.class, () -> new JwtConfig().jwtEncoder(negativo));
	}

	@Test
	public void propertiesNaoImprimeOSegredo() {
		assertFalse(props(SEGREDO).toString().contains(SEGREDO));
	}

	private JwtTokenService servico(Instant agora, JwtProperties p) {
		JwtEncoder enc = new JwtConfig().jwtEncoder(p);
		return new JwtTokenService(enc, p, Clock.fixed(agora, ZoneOffset.UTC));
	}

	@Test
	public void accessTokenTemClaimsMinimosAssinaturaHs256EVencimentoCurto() {
		JwtProperties p = props(SEGREDO);
		JwtDecoder decoder = new JwtConfig().jwtDecoder(p);
		Instant agora = Instant.now();

		JwtTokenService.AccessToken token = servico(agora, p).emitir(usuario("x"));
		Jwt jwt = decoder.decode(token.valor());

		assertEquals(900, token.expiraEmSegundos());
		assertEquals("3", jwt.getSubject());
		assertEquals("seazs", jwt.getClaimAsString("iss"));
		assertEquals(List.of("ROLE_PROFESSOR"), jwt.getClaimAsStringList("roles"));
		assertEquals("HS256", jwt.getHeaders().get("alg"));
		assertEquals(Duration.ofMinutes(15), Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()));
		assertFalse(jwt.getClaims().containsKey("email"));
		assertFalse(jwt.getClaims().containsKey("senha"));
	}

	@Test
	public void decoderRejeitaTokenExpiradoOutroSegredoEOutroEmissor() {
		JwtProperties p = props(SEGREDO);
		JwtDecoder decoder = new JwtConfig().jwtDecoder(p);

		String expirado = servico(Instant.now().minus(Duration.ofHours(1)), p).emitir(usuario("x")).valor();
		assertThrows(JwtValidationException.class, () -> decoder.decode(expirado));

		String outroSegredo = servico(Instant.now(), props("outro-segredo-com-mais-de-32-bytes-ok!!")).emitir(usuario("x")).valor();
		assertThrows(org.springframework.security.oauth2.jwt.BadJwtException.class, () -> decoder.decode(outroSegredo));

		JwtProperties outroEmissor = new JwtProperties(SEGREDO, "outro", Duration.ofMinutes(15), Duration.ofDays(7));
		String emissorErrado = servico(Instant.now(), outroEmissor).emitir(usuario("x")).valor();
		assertThrows(JwtValidationException.class, () -> decoder.decode(emissorErrado));

		assertThrows(org.springframework.security.oauth2.jwt.BadJwtException.class, () -> decoder.decode("nao.e.jwt"));
	}

	// ---- Handlers 401/403 em ProblemDetail

	@Test
	public void entryPointResponde401ProblemJsonSemVazarOMotivo() throws Exception {
		ProblemDetailSecurityHandlers handlers = new ProblemDetailSecurityHandlers(JsonMapper.builder().build());
		MockHttpServletResponse resposta = new MockHttpServletResponse();

		handlers.commence(new MockHttpServletRequest(), resposta, new BadCredentialsException("segredo interno"));

		assertEquals(401, resposta.getStatus());
		assertEquals("Bearer", resposta.getHeader("WWW-Authenticate"));
		assertTrue(resposta.getContentType().startsWith("application/problem+json"));
		String corpo = resposta.getContentAsString();
		assertTrue(corpo.contains("\"status\":401"), corpo);
		assertFalse(corpo.contains("segredo interno"));
	}

	@Test
	public void accessDeniedResponde403ProblemJson() throws Exception {
		ProblemDetailSecurityHandlers handlers = new ProblemDetailSecurityHandlers(JsonMapper.builder().build());
		MockHttpServletResponse resposta = new MockHttpServletResponse();

		handlers.handle(new MockHttpServletRequest(), resposta, new AccessDeniedException("x"));

		assertEquals(403, resposta.getStatus());
		assertTrue(resposta.getContentType().startsWith("application/problem+json"));
		assertTrue(resposta.getContentAsString().contains("\"status\":403"));
	}

	// ---- CORS

	@Test
	public void corsRecusaCuringa() {
		assertThrows(IllegalStateException.class,
				() -> new SecurityConfig().corsConfigurationSource(Arrays.asList("http://a.com", "*")));
		assertThrows(IllegalStateException.class,
				() -> new SecurityConfig().corsConfigurationSource(Arrays.asList("https://*.fatec.sp.gov.br")));
	}
}
