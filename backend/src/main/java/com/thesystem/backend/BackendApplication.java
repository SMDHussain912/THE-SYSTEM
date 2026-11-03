package com.thesystem.backend;

import java.nio.file.Files;
import java.nio.file.Path;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		loadDotEnv();
		SpringApplication.run(BackendApplication.class, args);
	}

	private static void loadDotEnv() {
		Path workingDirectory = Path.of("").toAbsolutePath().normalize();
		Path envDirectory = Files.exists(workingDirectory.resolve(".env"))
				? workingDirectory
				: workingDirectory.getParent();

		if (envDirectory == null || !Files.exists(envDirectory.resolve(".env"))) {
			return;
		}

		Dotenv dotenv = Dotenv.configure()
				.directory(envDirectory.toString())
				.ignoreIfMissing()
				.load();

		dotenv.entries().forEach(entry -> {
			if (System.getenv(entry.getKey()) == null
					&& System.getProperty(entry.getKey()) == null) {
				System.setProperty(entry.getKey(), entry.getValue());
			}
		});
	}
}
