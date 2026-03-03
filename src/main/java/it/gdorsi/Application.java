package it.gdorsi;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
 // eventuell besser als bean overriding (?)
// Wegen vectorStore-Bean selbst konfigurierbar machen (mit 1024 Dimensionen, Cosine etc.), sollten wir Spring sagen,
// dass es die automatische (Standard-)Konfiguration ignorieren soll.
//@SpringBootApplication(exclude = { PgVectorStoreAutoConfiguration.class })
@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
