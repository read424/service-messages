package org.walrex.infrastructure.config.consul;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Registra el servicio automáticamente en Consul al iniciar.
 */
@ApplicationScoped
@Slf4j
public class ConsulServiceRegistrar {

    @ConfigProperty(name = "quarkus.application.name")
    String serviceName;

    @ConfigProperty(name = "quarkus.http.port")
    int servicePort;

    @ConfigProperty(name = "consul.host", defaultValue = "localhost")
    String consulHost;

    @ConfigProperty(name = "consul.port", defaultValue = "8500")
    int consulPort;

    @ConfigProperty(name = "consul.service.host", defaultValue = "localhost")
    String serviceHost;

    @ConfigProperty(name = "consul.health.check.host", defaultValue = "host.docker.internal")
    String healthCheckHost;

    private String serviceId;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    void onStart(@Observes StartupEvent event) {
        serviceId = serviceName + "-" + servicePort;

        // Si host.docker.internal no está configurado, intentar detectar la IP del host accesible desde Docker
        String effectiveHealthCheckHost = healthCheckHost;
        if ("host.docker.internal".equals(healthCheckHost)) {
            // Intentar detectar la IP del docker bridge (típicamente 172.17.0.1 o 172.18.0.1)
            // Esta es la IP del host vista desde contenedores Docker
            try {
                // Intentar detectar el bridge docker0 primero
                ProcessBuilder pb = new ProcessBuilder("sh", "-c",
                    "ip addr show docker0 2>/dev/null | grep 'inet ' | awk '{print $2}' | cut -d'/' -f1");
                Process process = pb.start();
                String bridgeIp = new String(process.getInputStream().readAllBytes()).trim();

                if (!bridgeIp.isEmpty()) {
                    effectiveHealthCheckHost = bridgeIp;
                    log.info("🔍 IP del bridge Docker detectada: {}", effectiveHealthCheckHost);
                } else {
                    // Fallback: intentar con IPs comunes de Docker
                    effectiveHealthCheckHost = "172.17.0.1";
                    log.info("🔍 Usando IP de Docker por defecto: {}", effectiveHealthCheckHost);
                }
            } catch (Exception e) {
                log.warn("⚠️ No se pudo detectar la IP del bridge, usando fallback: 172.17.0.1");
                effectiveHealthCheckHost = "172.17.0.1";
            }
        }

        // Registrar sin health check por ahora
        String json = String.format("""
                {
                  "ID": "%s",
                  "Name": "%s",
                  "Address": "%s",
                  "Port": %d,
                  "Tags": ["quarkus", "microservice"]
                }
                """, serviceId, serviceName, serviceHost, servicePort);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + consulHost + ":" + consulPort + "/v1/agent/service/register"))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("✅ Servicio registrado en Consul: {} (ID: {})", serviceName, serviceId);
            } else {
                log.error("❌ Error registrando servicio en Consul. Status: {}", response.statusCode());
            }
        } catch (Exception e) {
            log.error("❌ Error conectando con Consul: {}", e.getMessage());
        }
    }

    void onStop(@Observes ShutdownEvent event) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + consulHost + ":" + consulPort + "/v1/agent/service/deregister/" + serviceId))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("✅ Servicio deregistrado de Consul");
            }
        } catch (Exception e) {
            log.warn("⚠️ Error deregistrando servicio de Consul: {}", e.getMessage());
        }
    }
}