package br.com.fateczs.seazs.security;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import br.com.fateczs.seazs.model.Usuario;

/** Emite o access token (JWT HS256, vida curta). Claims minimos: sub (id), iss, iat, exp, jti e roles. */
@Service
public class JwtTokenService {

	/** Nome do claim com as authorities (lido por {@link SecurityConfig}). */
	public static final String CLAIM_ROLES = "roles";

	private final JwtEncoder encoder;
	private final JwtProperties props;
	private final Clock clock;

	public JwtTokenService(JwtEncoder encoder, JwtProperties props, Clock clock) {
		this.encoder = encoder;
		this.props = props;
		this.clock = clock;
	}

	public AccessToken emitir(Usuario usuario) {
		Instant agora = clock.instant();
		Instant expira = agora.plus(props.accessTtl());
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(props.issuer())
				.subject(String.valueOf(usuario.getId()))
				.issuedAt(agora)
				.expiresAt(expira)
				.id(UUID.randomUUID().toString())
				.claim(CLAIM_ROLES, Autoridades.de(usuario))
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String valor = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new AccessToken(valor, props.accessTtl().getSeconds());
	}

	/** Token emitido e sua duracao em segundos. */
	public record AccessToken(String valor, long expiraEmSegundos) {
	}
}
