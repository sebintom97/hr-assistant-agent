package com.hrassistant.hrcore.learning.oop;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class PolymorphismTest {

    @Test
    void theJvmRunsTheMethodOfTheRealObjectNotOfTheStickyNote() {
        LeavePolicy p = new LongSickLeave();     // note type: LeavePolicy, real object: LongSickLeave

        assertThat(p.name()).isEqualTo("Long-term sick");   // overridden in LongSickLeave
        assertThat(p.usesAllowance()).isFalse();            // not overridden: inherited from SickLeave

        SickLeave s = new LongSickLeave();       // note type: SickLeave, real object: LongSickLeave
        assertThat(s.name()).isEqualTo("Long-term sick");   // the JVM picks the child's version at runtime
    }

    @Test
    void oneQuestionManyAnswers() {
        List<LeavePolicy> pile = List.of(new AnnualLeave(), new SickLeave(), new LongSickLeave());

        StringBuilder answers = new StringBuilder();
        for (LeavePolicy policy : pile) {        // the officer never checks which kind it is
            answers.append(policy.name()).append("=").append(policy.usesAllowance()).append("; ");
        }

        assertThat(answers.toString())
                .isEqualTo("Annual=true; Sick=false; Long-term sick=false; ");
    }
}
