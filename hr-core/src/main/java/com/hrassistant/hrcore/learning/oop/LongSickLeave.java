package com.hrassistant.hrcore.learning.oop;

/** Lesson 3: "is a" SickLeave. Replaces name(), inherits usesAllowance() unchanged. */
class LongSickLeave extends SickLeave {
    @Override 
    public String name() { return "Long-term sick"; }
}