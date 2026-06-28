package com.communityheroai.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class FirebaseAuthenticationFilterTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void verifiedAllowlistedEmailReceivesAdminAuthority() throws Exception {
        FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
        FirebaseToken token = mock(FirebaseToken.class);
        when(firebaseAuth.verifyIdToken("valid-token")).thenReturn(token);
        when(token.getEmail()).thenReturn("HeroCommunity96@gmail.com");
        when(token.isEmailVerified()).thenReturn(true);
        when(token.getUid()).thenReturn("firebase-uid");
        when(token.getName()).thenReturn("Community Hero Admin");
        FirebaseAuthenticationFilter filter = new FirebaseAuthenticationFilter(
                firebaseAuth, new ObjectMapper(), "herocommunity96@gmail.com");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/issues");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_CITIZEN", "ROLE_ADMIN");
    }
}
