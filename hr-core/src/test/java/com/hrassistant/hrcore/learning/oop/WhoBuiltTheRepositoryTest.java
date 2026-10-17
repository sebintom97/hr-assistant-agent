package com.hrassistant.hrcore.learning.oop;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.hrassistant.hrcore.TestcontainersConfiguration;
import com.hrassistant.hrcore.employee.repository.EmployeeRepository;

/** Lesson 3, question 4: what real object sits behind the EmployeeRepository sticky note? */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class WhoBuiltTheRepositoryTest {

    @Autowired
    EmployeeRepository employeeRepository;

    @Test
    void springGeneratedAClassAtStartupThatImplementsOurInterface() throws Exception {
        Class<?> realClass = employeeRepository.getClass();
        Object target = ((Advised) employeeRepository).getTargetSource().getTarget();

        System.out.println("LESSON3 real class of the object: " + realClass.getName());
        System.out.println("LESSON3 it implements:            " + Arrays.toString(realClass.getInterfaces()));
        System.out.println("LESSON3 calls end up in:          " + target.getClass().getName());

        assertThat(employeeRepository).isInstanceOf(EmployeeRepository.class);
        assertThat(realClass.getName()).contains("Proxy");   // a class nobody wrote in a .java file
    }
}
