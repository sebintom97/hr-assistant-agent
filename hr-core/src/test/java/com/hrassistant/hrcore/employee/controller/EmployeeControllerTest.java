package com.hrassistant.hrcore.employee.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import com.hrassistant.hrcore.TestcontainersConfiguration;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Import(TestcontainersConfiguration.class)
public class EmployeeControllerTest {

    private static final String ACME = "5f264e76-22a0-0cf1-f3e9-c36326aed1a8";
    private static final String BRIGHTWAVE = "e99f03ab-dc03-ac67-d48a-9a8047a6f641";
    private static final String LIAM = "da55bc36-7063-1691-62bf-7d8262e95137";
    private static final String Niamh ="accadaa4-d0af-cd87-d4e8-e1e59ccd3575"; //Liam's manger
    private static final String Sarah ="f6089549-c865-2703-ecbe-f76563afd741"; //Brightwave 


    @Autowired
    MockMvc mockMvc;

    @Test
    void colleagueInSameCompanyCanSeeEmployee() throws Exception {
        mockMvc.perform(get("/api/v1/employees/" + LIAM).header("X-Employee-Id", Niamh))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Liam O'Connor"))
                .andExpect(jsonPath("$.tenantId").doesNotExist());
    }

    @Test
    void hidesEmployeeFromAnotherTenant() throws Exception {
        mockMvc.perform(get("/api/v1/employees/" + LIAM).header("X-Employee-Id", Sarah))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.name").doesNotExist());
    }

    @Test
    void rejectsRequestWithoutIdentity() throws Exception {
        mockMvc.perform(get("/api/v1/employees/" + LIAM))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsTheCallersOwnProfile() throws Exception {
        mockMvc.perform(get("/api/v1/employees/me").header("X-Employee-Id", LIAM))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Liam O'Connor"));

    }
}
