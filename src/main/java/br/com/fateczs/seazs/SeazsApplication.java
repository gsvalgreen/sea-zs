package br.com.fateczs.seazs;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import br.com.fateczs.seazs.utils.DataUtils;

@SpringBootApplication
public class SeazsApplication {

	/** Unico ponto de leitura do relogio do sistema; testes substituem por Clock fixo. */
	@Bean
	public Clock clock() {
		return Clock.system(DataUtils.TIME_ZONE.toZoneId());
	}

	public static void main(String[] args) {
		SpringApplication.run(SeazsApplication.class, args);
	}
}
