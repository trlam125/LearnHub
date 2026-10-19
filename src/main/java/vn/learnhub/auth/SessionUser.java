package vn.learnhub.auth;

import lombok.Getter;

import vn.learnhub.user.Account;

import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

public class SessionUser implements UserDetails, CredentialsContainer {
    @Getter
    private final String username;
    @Getter
    private String password;
    public final long version;
    @Getter
    private final boolean enabled;
    @Getter
    private final List<GrantedAuthority> authorities;
    @Getter
    private final boolean accountNonExpired = true;
    @Getter
    private final boolean accountNonLocked = true;
    @Getter
    private final boolean credentialsNonExpired = true;

    public SessionUser(Account user) {
        username = user.email;
        password = user.password;
        version = user.sessionVersion;
        enabled = user.active && user.verified;
        authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.role.name()));
    }

    @Override
    public void eraseCredentials() {
        password = null;
    }
}
