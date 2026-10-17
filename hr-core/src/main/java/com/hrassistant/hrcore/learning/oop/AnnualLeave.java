package com.hrassistant.hrcore.learning.oop;

/** Lesson 3: fulfils the LeavePolicy promise. */
class AnnualLeave implements LeavePolicy {
    @Override
    public String name() { return "Annual"; }

    @Override
    public boolean usesAllowance() { return true; }
}
