package com.microservicio.noticias.config;



import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

    @Configuration
    public class WebConfig implements WebMvcConfigurer {

        /**
         * Configurar para servir archivos estáticos desde /uploads/
         */
        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            System.out.println("✅ Configurando recursos estáticos: /uploads/ -> file:uploads/");

            registry.addResourceHandler("/uploads/**")
                    .addResourceLocations("file:uploads/")
                    .setCachePeriod(3600); // Cache por 1 hora
        }

        /**
         * Configurar CORS para permitir peticiones desde el frontend
         */
        @Override
        public void addCorsMappings(CorsRegistry registry) {
            System.out.println("✅ Configurando CORS para frontend");

            registry.addMapping("/**") // Aplica a todas las rutas
                    .allowedOrigins(
                            "http://localhost:4200",  // Angular
                            "http://localhost:8100"   // Ionic (si lo usas)
                    )
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
                    .allowCredentials(true)
                    .maxAge(3600); // Cache de preflight por 1 hora
        }
    }

