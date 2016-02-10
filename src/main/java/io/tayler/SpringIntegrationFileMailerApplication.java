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
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.filters.LastModifiedFileListFilter;
import org.springframework.integration.file.transformer.FileToByteArrayTransformer;

@SpringBootApplication
public class SpringIntegrationFileMailerApplication {

	public static void main(String[] args) {
		new SpringApplicationBuilder(SpringIntegrationFileMailerApplication.class)
        	.web(false)
        	.run(args);
	}

	@Bean
	public IntegrationFlow start_prepareInboundFilesToBeEmailedFlow() {
		return IntegrationFlows.from(inboundFileChannel(), e -> e.poller(Pollers.fixedDelay(10000)))
				.transform(fileIntoEmailAttachment())
				.channel("emailFileAsAttachmentOutboundChannel")
				.get();
	}

	@Bean
	public IntegrationFlow end_emailFileAsAttachmentFlow() {
	    return IntegrationFlows.from("emailFileAsAttachmentOutboundChannel")
	    		.enrichHeaders(Mail.headers()
	    				.attachmentFilename("filename.pdf")
	    				.to("example@example.com")
	    				.from("example@example.com")
	    				.subject("here is your file")
	    		)
	            .handle(Mail.outboundAdapter("localhost")
	                            .port(587)
	                            .credentials("user", "pw")
	                            .protocol("smtp")
	                            .javaMailProperties(p -> p.put("mail.debug", "true")),
	                    e -> e.id("sendMailEndpoint"))
	            .get();
	}


	@Bean
	public FileReadingMessageSource inboundFileChannel() {
		return Files.inboundAdapter(new File("/inbound-file-directory-here"))
				.preventDuplicates()
				.ignoreHidden()
				.regexFilter(".*\\.pdf")
				.filter(giveFilesTimeToFinishWritingToDisk())
				.get();
	}

	@Bean
	public FileToByteArrayTransformer fileIntoEmailAttachment() {
		return Transformers.fileToByteArray();
	}

	@Bean
	public LastModifiedFileListFilter giveFilesTimeToFinishWritingToDisk() {
		LastModifiedFileListFilter filter = new LastModifiedFileListFilter();
		filter.setAge(60000);
		return filter;
	}
}
