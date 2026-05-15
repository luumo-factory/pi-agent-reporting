package ai.luumo.tools.picodingagent.reporting.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Component
public class BuildInfoProvider {

    private static final Logger log = LoggerFactory.getLogger(BuildInfoProvider.class);

    private String gitCommitId = "dev";
    private String buildVersion = "1.0.0-SNAPSHOT";

    @PostConstruct
    public void load() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("git.properties")) {
            if (input != null) {
                Properties props = new Properties();
                props.load(input);
                gitCommitId = props.getProperty("git.commit.id.abbrev", gitCommitId);
                buildVersion = props.getProperty("git.build.version", buildVersion);
                log.info("Loaded build info - commit: {}, version: {}", gitCommitId, buildVersion);
            } else {
                log.warn("git.properties not found, using defaults");
            }
        } catch (IOException e) {
            log.warn("Failed to load git.properties: {}", e.getMessage());
        }
    }

    public String getGitCommitId() {
        return gitCommitId;
    }

    public String getBuildVersion() {
        return buildVersion;
    }
}
