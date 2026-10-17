package com.hrassistant.hrcore.learning.oop;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ReferencesTest {

    @Test
    void copyingAVariableCopiesTheStickyNoteNotTheForm() {
        LeaveCard a = new LeaveCard();   // form #1 in the heap; a holds its address
        a.days = 2;

        LeaveCard b = a;                 // line X: copy the address, not the form
        b.days = 0;                      // write on form #1 through the second note

        LeaveCard c = new LeaveCard();   // form #2: a different address
        c.days = 0;

        assertThat(a.days).isEqualTo(0);         // a and b point at the same form
        assertThat(a == b).isTrue();             // same address
        assertThat(a == c).isFalse();            // same content, different forms
        assertThat(a.equals(c)).isFalse();       // without our own equals(), equals() is just ==
    }

    @Test
    void javaPassesACopyOfTheStickyNoteIntoMethods() {
        LeaveCard card = new LeaveCard();
        card.days = 2;

        LeaveCard.setToFive(card);       // the method writes on OUR form
        assertThat(card.days).isEqualTo(5);

        LeaveCard.replaceWithNew(card);  // the method only replaced ITS copy of the note
        assertThat(card.days).isEqualTo(5);
    }
}
