package com.hrassistant.hrcore.common;

import com.hrassistant.hrcore.employee.Employee;
import com.hrassistant.hrcore.employee.EmployeeService;
import java.util.UUID;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Fills in the {@link CurrentUser} parameter of controller methods.
 *
 * ⚠️ PHASE 1 STAND-IN, NOT SECURE: it trusts the X-Employee-Id header, so anyone can claim to be
 * anyone. It exists so the leave API can be built and tried before login exists.
 * TODO(Sebin, Phase 2): replace with the authenticated JWT principal from Spring Security.
 * Controllers and services don't change; only this class does.
 */
@Component
public class CurrentUserResolver implements HandlerMethodArgumentResolver {

    public static final String HEADER = "X-Employee-Id";

    private final EmployeeService employees;

    public CurrentUserResolver(EmployeeService employees) {
        this.employees = employees;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(CurrentUser.class);
    }

    @Override
    public CurrentUser resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                       NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        String header = webRequest.getHeader(HEADER);
        if (header == null || header.isBlank()) {
            throw new ApiException.Unauthenticated("Missing " + HEADER + " header");
        }
        UUID employeeId;
        try {
            employeeId = UUID.fromString(header.trim());
        } catch (IllegalArgumentException e) {
            throw new ApiException.Unauthenticated(HEADER + " is not a valid UUID");
        }
        // The tenant comes from the employee record, never from the caller.
        Employee employee = employees.findActive(employeeId)
                .orElseThrow(() -> new ApiException.Unauthenticated("Unknown or inactive employee"));
        return new CurrentUser(employee.getId(), employee.getTenantId(), employee.getRole());
    }
}
