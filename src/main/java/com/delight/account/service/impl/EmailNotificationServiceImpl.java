package com.delight.account.service.impl;

import com.delight.account.service.NotificationService;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

/**
 * Email notification service implementation using SendGrid
 */
@Service
public class EmailNotificationServiceImpl implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationServiceImpl.class);

    private final SendGrid sendGrid;
    private final String fromEmail;
    private final String fromName;
    private final String activationTemplateId;
    private final String signupTemplateId;
    private final String activationUrlBase;
    private final String appDomainName;

    public EmailNotificationServiceImpl(
            @Value("${sendgrid.api.key:}") String apiKey,
            @Value("${sendgrid.from.email:support@delighthub.app}") String fromEmail,
            @Value("${sendgrid.from.name:DelightHub Support}") String fromName,
            @Value("${sendgrid.template.activation:d-744a0fb88ddf469da452ddcffa4876fa}") String activationTemplateId,
            @Value("${sendgrid.template.signup:d-744a0fb88ddf469da452ddcffa4876fa}") String signupTemplateId,
            @Value("${sendgrid.activation.url.base:http://localhost:8080}") String activationUrlBase,
            @Value("${app.domain.name:localhost}") String appDomainName) {
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("SendGrid API key is not configured. Email notifications will be disabled.");
            this.sendGrid = null;
        } else {
            this.sendGrid = new SendGrid(apiKey);
            logger.info("SendGrid email service initialized with from: {} <{}>", fromName, fromEmail);
        }
        
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        this.activationTemplateId = activationTemplateId;
        this.signupTemplateId = signupTemplateId;
        this.activationUrlBase = activationUrlBase;
        this.appDomainName = appDomainName;
    }

    @Override
    public void sendTemplatedEmail(String toEmail, String templateId, Map<String, Object> templateData) throws Exception {
        if (sendGrid == null) {
            logger.warn("SendGrid not configured. Skipping email to: {} with template: {}", toEmail, templateId);
            return;
        }

        logger.info("Sending templated email to: {} using template: {}", toEmail, templateId);

        try {
            Email from = new Email(fromEmail, fromName);
            Email to = new Email(toEmail);
            Mail mail = new Mail();
            mail.setFrom(from);
            mail.setTemplateId(templateId);

            // Add template data as dynamic template data
            Personalization personalization = new Personalization();
            personalization.addTo(to);
            
            // Add template data
            if (templateData != null) {
                for (Map.Entry<String, Object> entry : templateData.entrySet()) {
                    personalization.addDynamicTemplateData(entry.getKey(), entry.getValue());
                }
            }
            
            mail.addPersonalization(personalization);

            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);
            
            int statusCode = response.getStatusCode();
            if (statusCode >= 200 && statusCode < 300) {
                logger.info("Email sent successfully to: {} using template: {} (status: {})", 
                    toEmail, templateId, statusCode);
            } else {
                logger.error("Failed to send email to: {} using template: {} (status: {}, body: {})", 
                    toEmail, templateId, statusCode, response.getBody());
                throw new IOException("SendGrid API returned status: " + statusCode + ", body: " + response.getBody());
            }
        } catch (IOException ex) {
            logger.error("Error sending email to: {} using template: {}", toEmail, templateId, ex);
            throw ex;
        }
    }

    @Override
    public void sendEmail(String toEmail, String subject, String content) throws Exception {
        if (sendGrid == null) {
            logger.warn("SendGrid not configured. Skipping email to: {} with subject: {}", toEmail, subject);
            return;
        }

        logger.info("Sending email to: {} with subject: {}", toEmail, subject);

        try {
            Email from = new Email(fromEmail, fromName);
            Email to = new Email(toEmail);
            Content emailContent = new Content("text/html", content);
            Mail mail = new Mail(from, subject, to, emailContent);

            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);
            
            int statusCode = response.getStatusCode();
            if (statusCode >= 200 && statusCode < 300) {
                logger.info("Email sent successfully to: {} with subject: {} (status: {})", 
                    toEmail, subject, statusCode);
            } else {
                logger.error("Failed to send email to: {} with subject: {} (status: {}, body: {})", 
                    toEmail, subject, statusCode, response.getBody());
                throw new IOException("SendGrid API returned status: " + statusCode + ", body: " + response.getBody());
            }
        } catch (IOException ex) {
            logger.error("Error sending email to: {} with subject: {}", toEmail, subject, ex);
            throw ex;
        }
    }

    @Override
    public void sendActivationEmail(String toEmail, String firstName, String lastName) throws Exception {
        logger.info("Sending activation email to: {} for user: {} {}", toEmail, firstName, lastName);
        
        // Prepare template data for activation email
        Map<String, Object> templateData = Map.of(
            "firstName", firstName != null ? firstName : "",
            "lastName", lastName != null ? lastName : "",
            "fullName", (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "").trim(),
            "email", toEmail
        );
        
        sendTemplatedEmail(toEmail, activationTemplateId, templateData);
    }

    @Override
    public void sendSignupEmail(String toEmail, String firstName, String lastName, String activationToken, String domain) throws Exception {
        logger.info("Sending signup email to: {} for user: {} {} with domain: {}", toEmail, firstName, lastName, domain);
        
        // Build activation URL in subdomain.domain format
        // Format: {protocol}://{subdomain}.{domain}/activate?token={token}
        // Examples:
        // - Local: http://tecompany.localhost:8080/activate?token=...
        // - Production: https://tecompany.delighthub.app/activate?token=...
        String activationUrl;
        
        // Determine protocol and port from activationUrlBase
        boolean isHttps = activationUrlBase.startsWith("https://");
        String protocol = isHttps ? "https://" : "http://";
        
        // Extract port if present in base URL (for local development)
        String port = "";
        if (activationUrlBase.contains(":8080") || activationUrlBase.contains(":80") || activationUrlBase.contains(":443")) {
            // Extract port from base URL
            String baseHost = activationUrlBase.replace("http://", "").replace("https://", "").split("/")[0];
            if (baseHost.contains(":")) {
                port = ":" + baseHost.substring(baseHost.indexOf(":") + 1);
            }
        }
        
        // Construct URL as subdomain.domain format
        if ("localhost".equals(appDomainName)) {
            // Local development: http://{subdomain}.localhost:8080
            activationUrl = String.format("%s%s.%s%s/activate?token=%s", 
                protocol, domain, appDomainName, port.isEmpty() ? ":8080" : port, activationToken);
        } else {
            // Production: https://{subdomain}.{domain}
            activationUrl = String.format("%s%s.%s/activate?token=%s", 
                protocol, domain, appDomainName, activationToken);
        }
        
        logger.debug("Generated activation URL: {}", activationUrl);
        
        // Prepare template data for signup email with activation link
        Map<String, Object> templateData = Map.of(
            "firstName", firstName != null ? firstName : "",
            "lastName", lastName != null ? lastName : "",
            "fullName", (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "").trim(),
            "email", toEmail,
            "activationUrl", activationUrl,
            "activationToken", activationToken,
            "domain", domain
        );
        
        sendTemplatedEmail(toEmail, signupTemplateId, templateData);
    }
}

