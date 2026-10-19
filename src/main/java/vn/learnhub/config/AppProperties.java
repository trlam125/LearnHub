package vn.learnhub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties("app")
public record AppProperties(String baseUrl, Path storage, boolean seedDemo,
                            String bootstrapEmail, String bootstrapPassword, String mailMode, String fromEmail,
                            String supportEmail,
                            String paymentMode, String bankName, String bankAccount, String bankOwner,
                            String payosClientId, String payosApiKey, String payosChecksumKey, boolean jobsEnabled) {
    public boolean hasPayosCredentials() {
        return payosClientId != null && !payosClientId.isBlank()
                && payosApiKey != null && !payosApiKey.isBlank()
                && payosChecksumKey != null && !payosChecksumKey.isBlank();
    }
}
