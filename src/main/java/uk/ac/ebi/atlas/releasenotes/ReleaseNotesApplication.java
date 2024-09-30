package uk.ac.ebi.atlas.releasenotes;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import picocli.CommandLine;
import uk.ac.ebi.atlas.releasenotes.command.ReleaseNoteCommand;
import uk.ac.ebi.atlas.releasenotes.exception.GitHubCliProcessException;
import uk.ac.ebi.atlas.releasenotes.logging.PicoCLIColorizedAppender;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.ProcessBuilder.Redirect.PIPE;
import static java.lang.System.exit;

@SpringBootApplication
@Slf4j
public class ReleaseNotesApplication {

	static String apiTokenCache;

	private static final Pattern GH_CLI_STATUS_TOKEN_REGEX =
			Pattern.compile("Token\\s*:\\s+(\\S+)$", Pattern.MULTILINE);

	static final ObjectMapper objectMapper = new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
			.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

	public static GitHubClient gitHubClient = Feign.builder()
			.decoder(new JacksonDecoder(objectMapper))
			.encoder(new JacksonEncoder(objectMapper))
			.requestInterceptor(request -> request.header("Authorization", "Bearer " + getApiToken()))
			.target(GitHubClient.class, "https://api.github.com");

	public static void main(String[] args) {
		SpringApplication.run(ReleaseNotesApplication.class, args);
		configureLogback();
		int exitCode = new CommandLine(new ReleaseNoteCommand()).execute(args);
		exit(exitCode);
	}

//	public static void main(String... args) {
//		configureLogback();
//		int exitCode = new CommandLine(new ReleaseNoteCommand()).execute(args);
//		exit(exitCode);
//	}

	static String getApiToken() {
		if (apiTokenCache != null) {
			return apiTokenCache;
		}
		try {
			Process statusProcess = new ProcessBuilder("gh", "auth", "status", "-t")
					.redirectOutput(PIPE)
					.redirectError(PIPE)
					.start();
			String statusOutput = IOUtils.toString(statusProcess.getInputStream(), Charset.defaultCharset());
			String statusError = IOUtils.toString(statusProcess.getErrorStream(), Charset.defaultCharset());

			if (statusError.contains("You are not logged into any GitHub hosts.")) {
				new ProcessBuilder("gh", "auth", "login")
						.inheritIO()
						.start()
						.waitFor();
			} else if (!statusOutput.contains("Logged in to github.com account")) {
				throw new GitHubCliProcessException("Unrecognized GitHub CLI auth status:\n" + statusOutput + statusError);
			}

			Matcher tokenMatcher = GH_CLI_STATUS_TOKEN_REGEX.matcher(statusOutput);
			if (tokenMatcher.find()) {
				apiTokenCache = tokenMatcher.group(1);
				return apiTokenCache;
			} else {
				throw new GitHubCliProcessException("Unable to extract token from output: " + statusOutput);
			}

		} catch (IOException | InterruptedException e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			throw new GitHubCliProcessException("GitHub CLI process error: " + e.getMessage(), e);
		}
	}

	private static void configureLogback() {
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

		PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		encoder.setContext(context);
		encoder.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
		encoder.start();

		PicoCLIColorizedAppender appender = new PicoCLIColorizedAppender();
		appender.setContext(context);
		appender.setEncoder(encoder);
		appender.start();

		Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		rootLogger.detachAndStopAllAppenders();
		rootLogger.addAppender(appender);
		rootLogger.setLevel(Level.DEBUG);
	}
}
