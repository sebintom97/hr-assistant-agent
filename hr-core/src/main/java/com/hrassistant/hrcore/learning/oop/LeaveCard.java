package com.hrassistant.hrcore.learning.oop;

/**
 * Lesson 2 (classes and objects): a deliberately tiny "form" with one box.
 * The field is package-private on purpose, so the lesson can poke at it directly.
 */
class LeaveCard {
    int days;

    /** Changes the form the sticky note points at: the caller sees this. */
    static void setToFive(LeaveCard card) {
        card.days = 5;
    }

    /** Swaps the parameter's own sticky note for a new form: the caller does NOT see this. */
    static void replaceWithNew(LeaveCard card) {
        card = new LeaveCard();
        card.days = 99;
    }
}
