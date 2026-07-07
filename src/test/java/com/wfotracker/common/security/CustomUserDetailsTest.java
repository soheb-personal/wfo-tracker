package com.wfotracker.common.security;

import org.junit.jupiter.api.Test;

import com.wfotracker.domain.entity.User;

import static org.junit.jupiter.api.Assertions.*;

class CustomUserDetailsTest {

    @Test
    void testEqualsAndHashCode() {
        User u1 = new User();
        u1.setId(1L);
        u1.setFullName("User One");

        User u2 = new User();
        u2.setId(1L);
        u2.setFullName("User One Copy");

        User u3 = new User();
        u3.setId(2L);
        u3.setFullName("User Two");

        CustomUserDetails cud1 = new CustomUserDetails(u1);
        CustomUserDetails cud2 = new CustomUserDetails(u2);
        CustomUserDetails cud3 = new CustomUserDetails(u3);

        // Reflexive
        assertEquals(cud1, cud1);

        // Symmetric & Equal IDs
        assertEquals(cud1, cud2);
        assertEquals(cud2, cud1);
        assertEquals(cud1.hashCode(), cud2.hashCode());

        // Different IDs
        assertNotEquals(cud1, cud3);
        assertNotEquals(cud1.hashCode(), cud3.hashCode());

        // Null and different class checks
        assertNotEquals(null, cud1);
        assertNotEquals("String", cud1);
    }

    @Test
    void testHashCodeWithNullId() {
        User u = new User();
        u.setId(null);
        CustomUserDetails cud = new CustomUserDetails(u);

        assertEquals(0, cud.hashCode());

        User u2 = new User();
        u2.setId(null);
        CustomUserDetails cud2 = new CustomUserDetails(u2);

        assertNotEquals(cud, cud2);
    }
}
