package com.hrassistant.hrcore.common;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    static {
        // CurrentUser is filled in from the header, not sent as a request parameter: hide it from Swagger.
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(CurrentUser.class);
    }

    private final CurrentUserResolver currentUserResolver;

    public WebConfig(CurrentUserResolver currentUserResolver) {
        this.currentUserResolver = currentUserResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserResolver);
    }

    /**
     * "Today" comes from an injected Clock, never LocalDate.now(), so tests can control time.
     * Dublin because the demo tenants are Irish; per-tenant time zones would be a later change.
     */
    @Bean
    Clock clock() {
        return Clock.system(ZoneId.of("Europe/Dublin"));
    }

    /** Swagger UI: an "Authorize" button where you paste an employee id to act as them. */
    @Bean
    OpenAPI openApi() {
        String scheme = "employeeId";
        return new OpenAPI()
                .info(new Info()
                        .title("hr-core API")
                        .version("phase-1")
                        .description("""
                                HR system of record: leave balances, requests and approvals.
                                Phase 1: identify yourself with the X-Employee-Id header (click Authorize). \
                                This is a temporary, insecure stand-in for JWT login (Phase 2)."""))
                .components(new Components().addSecuritySchemes(scheme, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name(CurrentUserResolver.HEADER)))
                .addSecurityItem(new SecurityRequirement().addList(scheme));
    }
}
