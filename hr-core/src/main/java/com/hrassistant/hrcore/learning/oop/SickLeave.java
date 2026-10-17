package com.hrassistant.hrcore.learning.oop;

/** Lesson 3: also fulfils LeavePolicy, with different answers. */
class SickLeave implements LeavePolicy {
    @Override
    public String name() { return "Sick"; }

    @Override
    public boolean usesAllowance() { return false; }
}
