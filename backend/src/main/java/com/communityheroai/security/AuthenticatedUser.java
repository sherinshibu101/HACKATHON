package com.communityheroai.security;

public record AuthenticatedUser(String uid, String email, String name) {
}
