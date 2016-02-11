package io.tayler;

import java.io.File;

import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.integration.file.transformer.FileToByteArrayTransformer;
import org.springframework.integration.mail.MailSendingMessageHandler;

@SpringBootApplication
public class SpringIntegrationFileMailerApplication {

	@Value("${spring.mail.username}")
	private String username;
	
	@Value("${spring.mail.password}")
	private String password;
	
	@Value("${spring.mail.host}")
	private String smtpRelayHostName;
	
	@Value("${spring.mail.port}")
	private int port;
	
	@Value("${email.attachment.name}")
	private String attachmentName;
	
	@Value("${email.to}")
	private String recipientAddress;
	
	@Value("${email.from}")
	private String senderAddress;
	
	@Value("${email.subject}")
	private String subject;
	
	@Value("${files.inbound.directory}")
	private String inboundFileDirectory;
	
	public static void main(String[] args) {
		new SpringApplicationBuilder(SpringIntegrationFileMailerApplication.class)
        	.web(false)
        	.run(args);
	}
	
	@Bean /*INBOUND ADAPTER*/
	public FileReadingMessageSource inboundFileChannel() {
		return Files.inboundAdapter(new File(inboundFileDirectory))
				.ignoreHidden()
				.preventDuplicates()
				.get();
	}
	
	@Bean /*FILE TRANSFORMER*/
	public FileToByteArrayTransformer intoEmailAttachmentAndDeleteOriginalFile() {
		FileToByteArrayTransformer transformer = Transformers.fileToByteArray();
		transformer.setDeleteFiles(true);
		return transformer;
	}
	
	@Bean /*START FLOW*/
	public IntegrationFlow prepareInboundFilesToBeEmailedFlow() {
		return IntegrationFlows.from(inboundFileChannel(), e -> e.poller(Pollers.fixedDelay(10000)))
				.transform(intoEmailAttachmentAndDeleteOriginalFile())
				.channel("sendEmailChannel")
				.get();
	}
	
	@Bean /*OUTBOUND ADAPTER*/
	public MailSendingMessageHandler sendEmailViaSmtpRelay() {
		return Mail.outboundAdapter(smtpRelayHostName)
				.port(port)
				.credentials(username, password)
				.protocol("smtp")
				.javaMailProperties(p -> p.put("mail.debug", "true"))
				.get();
	}
	
	@Bean /*END FLOW*/
	public IntegrationFlow emailFileAsAttachmentFlow() {
	    return IntegrationFlows.from("sendEmailChannel")
	    		.enrichHeaders(Mail.headers()
	    				.attachmentFilename(attachmentName)
	    				.to(recipientAddress)
	    				.from(senderAddress)
	    				.subject(subject))
	            .handle(sendEmailViaSmtpRelay(),
	                    e -> e.id("sendMailEndpoint"))
	            .get();
	}
}