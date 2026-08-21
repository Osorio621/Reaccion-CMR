package com.reactivosdelvalle.crm_api.config;

import com.reactivosdelvalle.crm_api.audit.AuditEventListener;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Registra el AuditEventListener en el SessionFactory de Hibernate mediante
 * un Integrator, para auditar automáticamente INSERT y UPDATE de las entidades
 * del CRM. El registro ocurre durante la construcción del EntityManagerFactory,
 * garantizando que los listeners queden activos desde el inicio.
 */
@Configuration
public class HibernateAuditConfig {

    private static final Logger log = LoggerFactory.getLogger(HibernateAuditConfig.class);

    @Bean
    public HibernatePropertiesCustomizer auditIntegratorCustomizer(AuditEventListener auditEventListener) {
        return properties -> properties.put(
                "hibernate.integrator_provider",
                (IntegratorProvider) () -> List.of(new Integrator() {
                    @Override
                    public void integrate(Metadata metadata, BootstrapContext bootstrapContext,
                                          SessionFactoryImplementor sessionFactory) {
                        EventListenerRegistry registry = sessionFactory.getServiceRegistry()
                                .getService(EventListenerRegistry.class);
                        registry.appendListeners(EventType.POST_INSERT, auditEventListener);
                        registry.appendListeners(EventType.PRE_UPDATE, auditEventListener);
                        log.info("AuditEventListener registrado (POST_INSERT y PRE_UPDATE)");
                    }

                    @Override
                    public void disintegrate(SessionFactoryImplementor sessionFactory,
                                             SessionFactoryServiceRegistry serviceRegistry) {
                        // Nada que hacer al cerrar
                    }
                }));
    }
}
