package com.rajlaxmi.jewellers.service.impl;

import com.rajlaxmi.jewellers.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * ================================================================
 * EmailServiceImpl — HTML Email Templates
 * ================================================================
 * @Async on each method:
 *   Email sending can take 1-3 seconds (SMTP call).
 *   @Async runs it on a separate thread pool so the API response
 *   is returned to the user immediately without waiting for email.
 *   Requires @EnableAsync (auto-configured by Spring Boot).
 *
 * HTML templates use inline CSS (some email clients strip <style>).
 * Colors match RajLaxmi brand: Gold (#D4AF37), Maroon (#5A0F16).
 *
 * PRODUCTION TODO: Use a templating engine like Thymeleaf
 * or a transactional email service like Brevo/SendGrid for
 * better deliverability and template management.
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${business.name:राज लक्ष्मी ज्वेलर्स}")
    private String businessName;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Override
    @Async
    public void sendOtpEmail(String to, String name, String otp) {
        if (isMailDeliveryDisabled("OTP", to)) {
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, businessName);
            helper.setTo(to);
            helper.setSubject("Your OTP for " + businessName + " — " + otp);
            helper.setText(buildOtpEmailHtml(name, otp), true);
            mailSender.send(message);
            log.info("OTP email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", to, e.getMessage());
        }
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String to, String name, String resetToken) {
        if (isMailDeliveryDisabled("Password reset", to)) {
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, businessName);
            helper.setTo(to);
            helper.setSubject("Password Reset Request — " + businessName);
            helper.setText(buildPasswordResetHtml(name, resetToken), true);
            mailSender.send(message);
            log.info("Password reset email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", to, e.getMessage());
        }
    }

    @Override
    @Async
    public void sendWelcomeEmail(String to, String name) {
        if (isMailDeliveryDisabled("Welcome", to)) {
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, businessName);
            helper.setTo(to);
            helper.setSubject("Welcome to " + businessName + " — शाश्वत सुंदरता, भरोसे की विरासत");
            helper.setText(buildWelcomeEmailHtml(name), true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", to, e.getMessage());
        }
    }

    // ── HTML Templates ────────────────────────────────────────

    private boolean isMailDeliveryDisabled(String emailType, String to) {
        if (mailEnabled) {
            return false;
        }

        log.info("{} email delivery skipped for {} because app.mail.enabled=false.", emailType, to);
        return true;
    }

    private String buildOtpEmailHtml(String name, String otp) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="margin:0;padding:0;background:#F8F6F0;font-family:Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0">
                <tr><td align="center" style="padding:40px 20px;">
                  <table width="600" style="background:#fff;border-radius:8px;overflow:hidden;">
                    <!-- Header -->
                    <tr><td style="background:#1a0a0d;padding:30px;text-align:center;">
                      <h1 style="color:#D4AF37;margin:0;font-size:24px;">राज लक्ष्मी ज्वेलर्स</h1>
                      <p style="color:#D4AF3799;margin:6px 0 0;font-size:13px;">भगवान दास एंड संस</p>
                    </td></tr>
                    <!-- Body -->
                    <tr><td style="padding:40px;">
                      <h2 style="color:#1a1a1a;margin:0 0 16px;">Verify Your Email</h2>
                      <p style="color:#555;line-height:1.6;">Dear %s,</p>
                      <p style="color:#555;line-height:1.6;">Your one-time password (OTP) for email verification is:</p>
                      <!-- OTP Box -->
                      <div style="background:#F8F6F0;border:2px solid #D4AF37;border-radius:8px;padding:20px;text-align:center;margin:24px 0;">
                        <span style="font-size:36px;font-weight:bold;color:#1a0a0d;letter-spacing:8px;">%s</span>
                      </div>
                      <p style="color:#888;font-size:13px;">This OTP is valid for <strong>10 minutes</strong>. Do not share it with anyone.</p>
                    </td></tr>
                    <!-- Footer -->
                    <tr><td style="background:#F8F6F0;padding:20px;text-align:center;">
                      <p style="color:#999;font-size:12px;margin:0;">Near Santoshi Mata Mandir, Wazirganj, Gaya, Bihar — 805131</p>
                      <p style="color:#999;font-size:12px;margin:4px 0 0;">📧 rajlaxmijewellers.gaya@gmail.com | 📞 9102316789</p>
                    </td></tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(name, otp);
    }

    private String buildPasswordResetHtml(String name, String resetToken) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="margin:0;padding:0;background:#F8F6F0;font-family:Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0">
                <tr><td align="center" style="padding:40px 20px;">
                  <table width="600" style="background:#fff;border-radius:8px;">
                    <tr><td style="background:#1a0a0d;padding:30px;text-align:center;">
                      <h1 style="color:#D4AF37;margin:0;font-size:24px;">राज लक्ष्मी ज्वेलर्स</h1>
                    </td></tr>
                    <tr><td style="padding:40px;">
                      <h2 style="color:#1a1a1a;">Password Reset Request</h2>
                      <p style="color:#555;line-height:1.6;">Dear %s,</p>
                      <p style="color:#555;line-height:1.6;">We received a request to reset your password. Use the token below:</p>
                      <div style="background:#F8F6F0;border:1px solid #D4AF37;border-radius:6px;padding:16px;word-break:break-all;font-family:monospace;font-size:14px;color:#1a0a0d;margin:20px 0;">%s</div>
                      <p style="color:#888;font-size:13px;">This token expires in <strong>10 minutes</strong>. If you did not request a password reset, please ignore this email.</p>
                    </td></tr>
                    <tr><td style="background:#F8F6F0;padding:20px;text-align:center;">
                      <p style="color:#999;font-size:12px;margin:0;">राज लक्ष्मी ज्वेलर्स — Wazirganj, Gaya, Bihar</p>
                    </td></tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(name, resetToken);
    }

    private String buildWelcomeEmailHtml(String name) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="margin:0;padding:0;background:#F8F6F0;font-family:Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0">
                <tr><td align="center" style="padding:40px 20px;">
                  <table width="600" style="background:#fff;border-radius:8px;">
                    <tr><td style="background:linear-gradient(135deg,#1a0a0d,#2d1018);padding:40px;text-align:center;">
                      <h1 style="color:#D4AF37;margin:0;font-size:28px;">राज लक्ष्मी ज्वेलर्स</h1>
                      <p style="color:#D4AF3799;margin:8px 0 0;">शाश्वत सुंदरता, भरोसे की विरासत</p>
                    </td></tr>
                    <tr><td style="padding:40px;text-align:center;">
                      <h2 style="color:#1a1a1a;">Welcome, %s! 🪙</h2>
                      <p style="color:#555;line-height:1.8;">Your account has been verified. Explore our curated collection of BIS hallmark certified gold, diamond, and silver jewellery.</p>
                      <p style="color:#5A0F16;font-style:italic;">"पीढ़ियों से विश्वास का नाम"</p>
                    </td></tr>
                    <tr><td style="background:#F8F6F0;padding:20px;text-align:center;">
                      <p style="color:#999;font-size:12px;">Near Santoshi Mata Mandir, Wazirganj, Gaya, Bihar — 805131 | 9102316789</p>
                    </td></tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(name);
    }
}
