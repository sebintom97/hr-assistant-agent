package com.hrassistant.hrcore.auth;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.server.ResponseStatusException;
import com.hrassistant.hrcore.common.security.CurrentUser;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import com.hrassistant.hrcore.employee.service.EmployeeService;

/**
 * Fills every controller parameter of type CurrentUser.
 * TEMPORARY: trusts the X-Employee-Id header. Phase 2 replaces this with login (JWT).
 */
@Component
public class CurrentUserResolver implements HandlerMethodArgumentResolver {
   private static final String HEADER = "X-Employee-Id";
   private final EmployeeService employeeService;
   
   CurrentUserResolver (EmployeeService employeeService){
    this.employeeService = employeeService;
   }

   @Override
   public boolean supportsParameter(MethodParameter parameter){
    return parameter.getParameterType().equals(CurrentUser.class);
   }

   @Override
    public CurrentUser resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                       NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        String header = webRequest.getHeader(HEADER);
        if(header == null || header.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Missing X-Employee-Id header");
        UUID employeeId;
        try{
            employeeId= UUID.fromString(header);
        }
        catch(IllegalArgumentException e){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"X-Employee-Id is not a valid UUID");
        }
        return employeeService.identity(employeeId);
    }
}
