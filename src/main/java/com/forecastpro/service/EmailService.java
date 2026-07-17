package com.forecastpro.service;

import com.forecastpro.repository.UserRepository;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sends inventory alert emails via JavaMail. When SMTP is not configured or unavailable,
 * the full message is written to the server log instead.
 */
@Stateless
public class EmailService {

    private static final Logger LOG = Logger.getLogger(EmailService.class.getName());
    private static final String FROM_ADDRESS = "alerts@forecastpro.local";
    private static final long DEDUP_MINUTES = 60;

    private static final Map<String, Instant> RECENT_ALERTS = new ConcurrentHashMap<>();

    @Inject
    private UserRepository userRepository;

    public void sendInventoryAlert(String productName, int currentStock,
                                   String forecastDemand, String recommendedPurchase, String status) {
        String dedupKey = productName + "|" + currentStock + "|" + forecastDemand;
        Instant last = RECENT_ALERTS.get(dedupKey);
        if (last != null && last.isAfter(Instant.now().minusSeconds(DEDUP_MINUTES * 60))) {
            return;
        }
        RECENT_ALERTS.put(dedupKey, Instant.now());

        String subject = "ForecastPro Inventory Alert";
        String body = "Product: " + productName + "\n"
                + "Current Stock: " + currentStock + "\n"
                + "Forecast Demand: " + forecastDemand + "\n"
                + "Recommended Purchase: " + recommendedPurchase + "\n"
                + "Status: " + status;

        List<String> recipients = userRepository.findAdminAndSalesManagers().stream()
                .map(u -> u.getUsername() + "@forecastpro.local")
                .toList();

        if (recipients.isEmpty()) {
            logEmail(subject, body, List.of("(no admin/manager users)"));
            return;
        }

        if (!trySendSmtp(recipients, subject, body)) {
            logEmail(subject, body, recipients);
        }
    }

    private boolean trySendSmtp(List<String> recipients, String subject, String body) {
        Session session = buildSessionFromSystemProperties();
        if (session == null) {
            return false;
        }
        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(FROM_ADDRESS));
            for (String to : recipients) {
                msg.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            }
            msg.setSubject(subject, "UTF-8");
            msg.setText(body, "UTF-8");
            Transport.send(msg);
            LOG.info(() -> "Inventory alert email sent to " + recipients.size() + " recipient(s).");
            return true;
        } catch (MessagingException e) {
            LOG.log(Level.FINE, "SMTP send failed, falling back to log", e);
            return false;
        }
    }

    private void logEmail(String subject, String body, List<String> recipients) {
        LOG.warning(() -> "EMAIL (SMTP unavailable) — To: " + String.join(", ", recipients)
                + "\nSubject: " + subject + "\n" + body);
    }

    /** SMTP via -Dforecastpro.mail.smtp.host=... and optional -Dforecastpro.mail.smtp.port=25 */
    public static Session buildSessionFromSystemProperties() {
        String host = System.getProperty("forecastpro.mail.smtp.host");
        if (host == null || host.isBlank()) {
            return null;
        }
        java.util.Properties props = new java.util.Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", System.getProperty("forecastpro.mail.smtp.port", "25"));
        return Session.getInstance(props);
    }
}
