package com.delight.account.service;

import java.util.Map;

/**
 * Service interface for sending notifications (email, SMS, etc.)
 */
public interface NotificationService {
    
    /**
     * Send an email notification using SendGrid template
     * 
     * @param toEmail Recipient email address
     * @param templateId SendGrid template ID
     * @param templateData Template data for dynamic content
     * @throws Exception if email sending fails
     */
    void sendTemplatedEmail(String toEmail, String templateId, Map<String, Object> templateData) throws Exception;
    
    /**
     * Send a simple email notification
     * 
     * @param toEmail Recipient email address
     * @param subject Email subject
     * @param content Email content (plain text or HTML)
     * @throws Exception if email sending fails
     */
    void sendEmail(String toEmail, String subject, String content) throws Exception;
    
    /**
     * Send user activation notification email
     * 
     * @param toEmail Recipient email address
     * @param firstName User's first name
     * @param lastName User's last name
     * @throws Exception if email sending fails
     */
    void sendActivationEmail(String toEmail, String firstName, String lastName) throws Exception;
    
    /**
     * Send signup welcome email with activation link
     * 
     * @param toEmail Recipient email address
     * @param firstName User's first name
     * @param lastName User's last name
     * @param activationToken The activation token to include in the link
     * @param domain The account domain for building the activation URL
     * @throws Exception if email sending fails
     */
    void sendSignupEmail(String toEmail, String firstName, String lastName, String activationToken, String domain) throws Exception;
}

