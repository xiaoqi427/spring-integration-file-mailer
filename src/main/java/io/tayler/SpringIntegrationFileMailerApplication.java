package io.tayler;

import java.io.File;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.core.Pollers;
import org.springframework.integration.dsl.file.Files;
import org.springframework.integration.dsl.mail.Mail;
import org.springframework.integration.dsl.support.Transformers;
import org.springframework.integration.file.filters.LastModifiedFileListFilter;

@SpringBootApplication
public class SpringIntegrationFileMailerApplication {

	public static void main(String[] args) {
		//SpringApplication.run(SpringIntegrationFileMailerApplication.class, args);

		new SpringApplicationBuilder(SpringIntegrationFileMailerApplication.class)
        	.web(false)
        	.run(args);
	}
	
	@Bean
	public IntegrationFlow inboundFiles() {
		return IntegrationFlows.from(Files.inboundAdapter(new File("/home/vagrant/loan-applications"))
				.preventDuplicates().ignoreHidden().regexFilter("*.pdf").filter(minuteOldFile()),
				e -> e.poller(Pollers.fixedDelay(10000)).id("inboundFileChannel"))
				.transform(Transformers.fileToByteArray())
				.channel("emailFileChannel")
				.get();
	}
	
	private LastModifiedFileListFilter minuteOldFile() {
		LastModifiedFileListFilter filter = new LastModifiedFileListFilter();
		filter.setAge(60000);
		return filter; 
	}
}
