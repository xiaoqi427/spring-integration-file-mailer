package io.tayler;

import java.io.File;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.core.Pollers;
import org.springframework.integration.dsl.file.FileInboundChannelAdapterSpec;
import org.springframework.integration.dsl.file.Files;
import org.springframework.integration.dsl.mail.Mail;
import org.springframework.integration.dsl.support.Transformers;
import org.springframework.integration.file.filters.LastModifiedFileListFilter;
import org.springframework.integration.file.transformer.FileToByteArrayTransformer;

@SpringBootApplication
public class SpringIntegrationFileMailerApplication {

	public static void main(String[] args) {
		//SpringApplication.run(SpringIntegrationFileMailerApplication.class, args);

		new SpringApplicationBuilder(SpringIntegrationFileMailerApplication.class)
        	.web(false)
        	.run(args);
	}
	
	@Bean
	public IntegrationFlow prepareInboundFilesToBeEmailedFlow() {
		return IntegrationFlows.from(inboundFileChannel(), e -> e.poller(Pollers.fixedDelay(10000)))
				.transform(intoEmailAttachment())
				.channel("emailFileAsAttachmentOutboundChannel")
				.get();
	}
	
	@Bean
	public IntegrationFlow emailFileAsAttachmentFlow() {
	    return IntegrationFlows.from("emailFileAsAttachmentOutboundChannel")
	            .handle(Mail.outboundAdapter("smtp.sendgrid.net")
	                            .port(587)
	                            .credentials("user", "pw")
	                            .protocol("smtp")
	                            .javaMailProperties(p -> p.put("mail.debug", "true")),
	                    e -> e.id("sendMailEndpoint"))
	            .get();
	}

	
	
	
	@Bean
	public FileInboundChannelAdapterSpec inboundFileChannel() {
		return Files.inboundAdapter(new File("/home/vagrant/loan-applications"))
				.preventDuplicates()
				.ignoreHidden()
				.regexFilter("*.pdf")
				.filter(giveFilesTimeToFinishWritingToDisk());
	}
	
	
	@Bean
	public FileToByteArrayTransformer intoEmailAttachment() {
		return Transformers.fileToByteArray();
	}
	
	@Bean
	public LastModifiedFileListFilter giveFilesTimeToFinishWritingToDisk() {
		LastModifiedFileListFilter filter = new LastModifiedFileListFilter();
		filter.setAge(60000);
		return filter; 
	}
}
