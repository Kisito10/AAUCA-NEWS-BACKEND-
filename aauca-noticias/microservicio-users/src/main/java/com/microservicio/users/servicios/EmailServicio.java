package com.microservicio.users.servicios;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServicio {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private static final String CORREO_SISTEMA = "aaucanews@gmail.com";

    // ─────────────────────────────────────────────────────────────
    // Plantilla HTML Base
    // ─────────────────────────────────────────────────────────────

    private String plantilla(String contenido) {
        return """
            <div style="
                    font-family:Arial,sans-serif;
                    max-width:600px;
                    margin:0 auto;
                    border:1px solid #e0e0e0;
                    border-radius:10px;
                    overflow:hidden;
                    background:#ffffff;
                ">
                <div style="
                        background:#1a3a6b;
                        padding:24px;
                        text-align:center;
                    ">
                    <h1 style="
                            margin:0;
                            color:#ffffff;
                            font-size:24px;
                            letter-spacing:1px;
                        ">
                        AAUCA <span style="color:#ffc107;">NEWS</span>
                    </h1>
                    <p style="
                            margin-top:8px;
                            color:rgba(255,255,255,0.7);
                            font-size:12px;
                        ">
                        Sistema de Gestión Residencial Universitaria
                    </p>
                </div>
                <div style="
                        padding:32px;
                        background:#f9fafb;
                    ">
                    %s
                </div>
                <div style="
                        padding:18px;
                        text-align:center;
                        background:#f0f4f8;
                        color:#6b7280;
                        font-size:11px;
                        border-top:1px solid #e5e7eb;
                    ">
                    © AAUCA NEWS — Todos los derechos reservados.<br>
                    <span style="font-size:10px;">
                        Este correo fue generado automáticamente.
                        Por favor no respondas directamente a este mensaje.
                    </span>
                </div>
            </div>
            """.formatted(contenido);
    }

    // ─────────────────────────────────────────────────────────────
    // 1. Confirmación de Solicitud
    // ─────────────────────────────────────────────────────────────

    public void enviarConfirmacionSolicitud(String destinatario, String nombre) {

        String cuerpo = """
            <h2 style="color:#1a3a6b;margin-top:0;">
                ¡Hola, %s!
            </h2>
            <p>
                Hemos recibido correctamente tu solicitud de acceso
                a <strong>AAUCA NEWS</strong>.
            </p>
            <div style="
                    background:#ffffff;
                    border-left:4px solid #ffc107;
                    padding:18px;
                    border-radius:6px;
                    margin:24px 0;
                ">
                <p style="margin:0;">
                    📋 Tu solicitud está siendo revisada por la administración.
                </p>
                <p style="margin-top:10px;">
                    Recibirás una notificación cuando sea aprobada o rechazada.
                </p>
            </div>
            <p style="color:#6b7280;font-size:14px;">
                Si no realizaste esta solicitud, puedes ignorar este mensaje.
            </p>
            """.formatted(nombre);

        enviar(destinatario, "📋 Solicitud recibida — AAUCA NEWS", plantilla(cuerpo));
    }

    // ─────────────────────────────────────────────────────────────
    // 2. Confirmación de Rectificación
    // Se envía cuando el solicitante reemplaza su solicitud anterior
    // antes de que haya sido revisada por el Director.
    // ─────────────────────────────────────────────────────────────

    public void enviarConfirmacionRectificacion(
            String destinatario,
            String nombre,
            int numRectificacion) {

        String cuerpo = """
            <h2 style="color:#1a3a6b;margin-top:0;">
                ¡Hola, %s!
            </h2>
            <p>
                Tu solicitud anterior ha sido
                <strong>reemplazada correctamente</strong>
                con los nuevos datos que has enviado.
            </p>
            <div style="
                    background:#ffffff;
                    border-left:4px solid #0ea5e9;
                    padding:18px;
                    border-radius:6px;
                    margin:24px 0;
                ">
                <p style="margin:0;">
                    🔄 Esta es tu rectificación número <strong>%d</strong>.
                </p>
                <p style="margin-top:10px;">
                    Tu nueva solicitud está pendiente de revisión
                    por la administración.
                </p>
                <p style="
                        margin-top:10px;
                        color:#6b7280;
                        font-size:13px;
                    ">
                    ⚠️ Solo puedes rectificar mientras tu solicitud
                    no haya sido revisada. Una vez aprobada o rechazada
                    no podrás modificarla.
                </p>
            </div>
            <p style="color:#6b7280;font-size:14px;">
                Si no realizaste este cambio, contacta con la
                administración de la residencia universitaria.
            </p>
            """.formatted(nombre, numRectificacion);

        enviar(destinatario, "🔄 Solicitud rectificada — AAUCA NEWS", plantilla(cuerpo));
    }

    // ─────────────────────────────────────────────────────────────
    // 3. Envío de Credenciales
    // ─────────────────────────────────────────────────────────────

    public void enviarCredenciales(
            String destinatario,
            String nombre,
            String email,
            String password) {

        String cuerpo = """
            <h2 style="color:#1a3a6b;margin-top:0;">
                ¡Hola, %s!
            </h2>
            <p>
                Tu solicitud ha sido
                <strong style="color:#16a34a;">✅ aprobada</strong>.
            </p>
            <p>
                Ya puedes acceder al sistema
                usando las siguientes credenciales:
            </p>
            <div style="
                    background:#ffffff;
                    border:1px solid #d1fae5;
                    border-radius:10px;
                    padding:20px;
                    margin:24px 0;
                ">
                <table style="width:100%%;border-collapse:collapse;">
                    <tr>
                        <td style="padding:10px 0;color:#6b7280;width:140px;">
                            📧 Email:
                        </td>
                        <td style="padding:10px 0;font-weight:bold;">
                            %s
                        </td>
                    </tr>
                    <tr>
                        <td style="padding:10px 0;color:#6b7280;">
                            🔑 Contraseña:
                        </td>
                        <td style="
                                padding:10px 0;
                                font-weight:bold;
                                font-family:monospace;
                                font-size:16px;
                                color:#1a3a6b;
                                letter-spacing:1px;
                            ">
                            %s
                        </td>
                    </tr>
                </table>
            </div>
            <div style="text-align:center;margin:32px 0 12px;">
                <a href="%s/login"
                   style="
                        background:#1a3a6b;
                        color:#ffffff;
                        padding:14px 30px;
                        text-decoration:none;
                        border-radius:8px;
                        font-weight:bold;
                        display:inline-block;
                    ">
                    Iniciar sesión →
                </a>
            </div>
            <p style="color:#dc2626;font-size:13px;margin-top:24px;">
                ⚠️ Por seguridad, cambia tu contraseña
                después del primer inicio de sesión.
            </p>
            """.formatted(nombre, email, password, frontendUrl);

        enviar(destinatario, "✅ Cuenta aprobada — AAUCA NEWS", plantilla(cuerpo));
    }

    // ─────────────────────────────────────────────────────────────
    // 4. Rechazo de Solicitud
    // ─────────────────────────────────────────────────────────────

    public void enviarRechazo(String destinatario, String nombre) {

        String cuerpo = """
            <h2 style="color:#1a3a6b;margin-top:0;">
                Hola, %s
            </h2>
            <p>
                Lamentamos informarte que tu solicitud
                de acceso a <strong>AAUCA NEWS</strong>
                ha sido
                <strong style="color:#dc2626;">❌ rechazada</strong>.
            </p>
            <div style="
                    background:#ffffff;
                    border-left:4px solid #dc2626;
                    padding:18px;
                    border-radius:6px;
                    margin:24px 0;
                ">
                <p style="margin:0;">
                    Si consideras que esto es un error,
                    contacta con la administración
                    de la residencia universitaria.
                </p>
            </div>
            """.formatted(nombre);

        enviar(destinatario, "❌ Solicitud no aprobada — AAUCA NEWS", plantilla(cuerpo));
    }

    // ─────────────────────────────────────────────────────────────
    // 5. Bloqueo automático por límite de 4 años
    // Se envía cuando el sistema detecta en el login que el residente
    // ha completado 4 años y desactiva su acceso automáticamente.
    // ─────────────────────────────────────────────────────────────

    public void enviarNotificacionBloqueoAnios(String destinatario, String nombre) {

        String cuerpo = """
            <h2 style="color:#1a3a6b;margin-top:0;">
                Hola, %s
            </h2>
            <p>
                Tu acceso a <strong>AAUCA NEWS</strong> ha sido
                <strong>desactivado automáticamente</strong>
                al haber completado <strong>4 años</strong>
                en la Residencia Universitaria AAUCA.
            </p>
            <div style="
                    background:#ffffff;
                    border-left:4px solid #f59e0b;
                    padding:18px;
                    border-radius:6px;
                    margin:24px 0;
                ">
                <p style="margin:0;">
                    ⏳ Este es el límite máximo de estancia en la residencia.
                </p>
                <p style="margin-top:10px;">
                    Si crees que esto es un error o tienes alguna circunstancia
                    especial, contacta directamente con la
                    <strong>Dirección de la Residencia</strong>.
                </p>
            </div>
            <p style="color:#6b7280;font-size:13px;">
                Gracias por haber sido parte de la comunidad AAUCA.
            </p>
            """.formatted(nombre);

        enviar(destinatario, "⏳ Acceso desactivado — AAUCA NEWS", plantilla(cuerpo));
    }

    // ─────────────────────────────────────────────────────────────
    // 6. Expulsión definitiva
    // Se envía cuando el Director expulsa manualmente a un residente.
    // ─────────────────────────────────────────────────────────────

    public void enviarNotificacionExpulsion(String destinatario, String nombre) {

        String cuerpo = """
            <h2 style="color:#1a3a6b;margin-top:0;">
                Hola, %s
            </h2>
            <p>
                Tu acceso a <strong>AAUCA NEWS</strong> ha sido
                <strong style="color:#dc2626;">
                    revocado definitivamente
                </strong>
                por decisión de la administración de la
                Residencia Universitaria AAUCA.
            </p>
            <div style="
                    background:#ffffff;
                    border-left:4px solid #dc2626;
                    padding:18px;
                    border-radius:6px;
                    margin:24px 0;
                ">
                <p style="margin:0;">
                    🚫 No podrás iniciar sesión ni enviar nuevas
                    solicitudes de acceso al sistema.
                </p>
                <p style="margin-top:10px;">
                    Si consideras que esto es un error, contacta
                    directamente con la
                    <strong>Dirección de la Residencia</strong>.
                </p>
            </div>
            """.formatted(nombre);

        enviar(destinatario, "🚫 Acceso revocado — AAUCA NEWS", plantilla(cuerpo));
    }

    // ─────────────────────────────────────────────────────────────
    // Método interno de envío
    // ─────────────────────────────────────────────────────────────

    private void enviar(String destinatario, String asunto, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("AAUCA NEWS <aaucanews@gmail.com>");
            helper.setReplyTo(CORREO_SISTEMA);
            helper.setPriority(1);
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(htmlBody, true);

            mailSender.send(message);

            log.info("✅ Email '{}' enviado correctamente a {}", asunto, destinatario);

        } catch (MessagingException e) {
            log.error("❌ Error enviando email a {}: {}", destinatario, e.getMessage());
            throw new RuntimeException("Error al enviar email: " + e.getMessage(), e);
        }
    }
}