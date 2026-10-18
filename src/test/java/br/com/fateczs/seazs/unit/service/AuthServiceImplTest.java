package br.com.fateczs.seazs.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.fateczs.seazs.model.RefreshToken;
import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.model.dto.LoginLegadoResponse;
import br.com.fateczs.seazs.model.dto.LoginRequest;
import br.com.fateczs.seazs.model.dto.TokenResponse;
import br.com.fateczs.seazs.repository.RefreshTokenRepository;
import br.com.fateczs.seazs.repository.UsuarioRepository;
import br.com.fateczs.seazs.security.JwtProperties;
import br.com.fateczs.seazs.security.JwtTokenService;
import br.com.fateczs.seazs.security.RefreshTokens;
import br.com.fateczs.seazs.security.UsuarioAutenticado;
import br.com.fateczs.seazs.service.serviceImpl.AuthServiceImpl;
import br.com.fateczs.seazs.utils.TipoUsuario;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

	private static final Instant AGORA = Instant.parse("2026-10-17T12:00:00Z");

	@Mock
	private AuthenticationManager authenticationManager;
	@Mock
	private JwtTokenService tokenService;
	@Mock
	private RefreshTokenRepository refreshTokens;
	@Mock
	private UsuarioRepository usuarios;

	private AuthServiceImpl service;
	private Usuario ana;

	@BeforeEach
	public void montar() {
		service = new AuthServiceImpl();
		ReflectionTestUtils.setField(service, "authenticationManager", authenticationManager);
		ReflectionTestUtils.setField(service, "tokenService", tokenService);
		ReflectionTestUtils.setField(service, "props",
				new JwtProperties("segredo-de-teste-com-32-bytes-ou-mais!!", "seazs", Duration.ofMinutes(15), Duration.ofDays(7)));
		ReflectionTestUtils.setField(service, "refreshTokens", refreshTokens);
		ReflectionTestUtils.setField(service, "usuarios", usuarios);
		ReflectionTestUtils.setField(service, "clock", Clock.fixed(AGORA, ZoneOffset.UTC));
		ana = new Usuario();
		ana.setId(5);
		ana.setNome("Ana");
		ana.setEmail("ana@teste.com");
		ana.setSenha("{bcrypt}x");
		ana.setTipoUsuario(TipoUsuario.ALUNO);
		ana.setAdm(true);
	}

	private void autenticaComSucesso() {
		when(authenticationManager.authenticate(any()))
				.thenReturn(UsernamePasswordAuthenticationToken.authenticated(new UsuarioAutenticado(ana), null, null));
		when(tokenService.emitir(ana)).thenReturn(new JwtTokenService.AccessToken("jwt", 900));
	}

	// ---- login

	@Test
	public void loginValidoDevolveAccessEPersisteApenasOHashDoRefresh() {
		autenticaComSucesso();

		TokenResponse resposta = service.login(new LoginRequest("ana@teste.com", "Segredo123"));

		assertEquals("Bearer", resposta.tokenType());
		assertEquals("jwt", resposta.accessToken());
		assertEquals(900, resposta.expiresIn());
		assertEquals(5, resposta.usuario().id());
		assertEquals("ALUNO", resposta.usuario().tipoUsuario());
		assertTrue(resposta.usuario().authorities().contains("ROLE_ADMIN"));
		ArgumentCaptor<RefreshToken> salvo = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokens).save(salvo.capture());
		assertEquals(RefreshTokens.hash(resposta.refreshToken()), salvo.getValue().getTokenHash());
		assertNotEquals(resposta.refreshToken(), salvo.getValue().getTokenHash());
		assertEquals(AGORA.plus(Duration.ofDays(7)), salvo.getValue().getExpiraEm());
		assertEquals(5, salvo.getValue().getIdUsuario());
	}

	@Test
	public void loginComCredenciaisInvalidasPropagaBadCredentialsSemEmitirNada() {
		when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("x"));

		assertThrows(BadCredentialsException.class, () -> service.login(new LoginRequest("a@b.com", "errada")));

		verifyNoInteractions(tokenService, refreshTokens);
	}

	// ---- refresh

	private RefreshToken tokenArmazenado(Instant expira, Instant revogado) {
		RefreshToken t = new RefreshToken(RefreshTokens.hash("opaco"), "fam-1", 5, AGORA.minusSeconds(60), expira);
		t.setRevogadoEm(revogado);
		return t;
	}

	@Test
	public void refreshValidoRotacionaMantendoAFamilia() {
		RefreshToken atual = tokenArmazenado(AGORA.plusSeconds(60), null);
		when(refreshTokens.findByTokenHash(RefreshTokens.hash("opaco"))).thenReturn(Optional.of(atual));
		when(usuarios.findById(5)).thenReturn(Optional.of(ana));
		when(tokenService.emitir(ana)).thenReturn(new JwtTokenService.AccessToken("jwt2", 900));

		Optional<TokenResponse> resposta = service.refresh("opaco");

		assertTrue(resposta.isPresent());
		assertEquals(AGORA, atual.getRevogadoEm());
		assertNotEquals("opaco", resposta.get().refreshToken());
		ArgumentCaptor<RefreshToken> salvos = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokens, org.mockito.Mockito.times(2)).save(salvos.capture());
		assertEquals("fam-1", salvos.getAllValues().get(1).getFamilia());
	}

	@Test
	public void refreshDesconhecidoDevolveVazio() {
		when(refreshTokens.findByTokenHash(anyString())).thenReturn(Optional.empty());

		assertFalse(service.refresh("qualquer").isPresent());

		verify(refreshTokens, never()).save(any());
	}

	@Test
	public void refreshExpiradoDevolveVazioSemRotacionar() {
		when(refreshTokens.findByTokenHash(anyString())).thenReturn(Optional.of(tokenArmazenado(AGORA, null)));

		assertFalse(service.refresh("opaco").isPresent());

		verify(refreshTokens, never()).save(any());
	}

	@Test
	public void refreshJaUsadoRevogaAFamiliaInteira() {
		when(refreshTokens.findByTokenHash(anyString()))
				.thenReturn(Optional.of(tokenArmazenado(AGORA.plusSeconds(60), AGORA.minusSeconds(5))));

		assertFalse(service.refresh("opaco").isPresent());

		verify(refreshTokens).revogarFamilia("fam-1", AGORA);
		verify(refreshTokens, never()).save(any());
	}

	@Test
	public void refreshDeUsuarioExcluidoDevolveVazio() {
		when(refreshTokens.findByTokenHash(anyString())).thenReturn(Optional.of(tokenArmazenado(AGORA.plusSeconds(60), null)));
		when(usuarios.findById(5)).thenReturn(Optional.empty());

		assertFalse(service.refresh("opaco").isPresent());
	}

	// ---- logout

	@Test
	public void logoutRevogaAFamiliaDoToken() {
		when(refreshTokens.findByTokenHash(anyString())).thenReturn(Optional.of(tokenArmazenado(AGORA.plusSeconds(60), null)));

		service.logout("opaco");

		verify(refreshTokens).revogarFamilia("fam-1", AGORA);
	}

	@Test
	public void logoutDeTokenDesconhecidoEIgnorado() {
		when(refreshTokens.findByTokenHash(anyString())).thenReturn(Optional.empty());

		service.logout("nada");

		verify(refreshTokens, never()).revogarFamilia(anyString(), any());
	}

	// ---- login legado

	@Test
	public void loginLegadoValidoDevolveUsuarioETokenSemRefresh() {
		autenticaComSucesso();

		Optional<LoginLegadoResponse> resposta = service.loginLegado("ana@teste.com", "Segredo123");

		assertTrue(resposta.isPresent());
		assertEquals("jwt", resposta.get().getToken());
		assertEquals(ana, resposta.get().getUsuario());
		verifyNoInteractions(refreshTokens);
	}

	@Test
	public void loginLegadoComCredenciaisInvalidasDevolveVazio() {
		when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("x"));

		assertFalse(service.loginLegado("a@b.com", "errada").isPresent());
	}

	@Test
	public void loginLegadoSemEmailOuSenhaOuComSenhaAcimaDe72BytesNemTentaAutenticar() {
		assertFalse(service.loginLegado(null, "x").isPresent());
		assertFalse(service.loginLegado("a@b.com", null).isPresent());
		// 40 caracteres de 2 bytes = 80 bytes
		assertFalse(service.loginLegado("a@b.com", "é".repeat(40)).isPresent());

		verifyNoInteractions(authenticationManager);
	}

	@Test
	public void loginLegadoAceitaSenhaDeExatamente72Bytes() {
		autenticaComSucesso();

		assertNotNull(service.loginLegado("ana@teste.com", "a".repeat(72)).orElse(null));
		verify(authenticationManager).authenticate(any());
	}
}
