package com.vivance.hotel.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class HotelAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider {

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails,
            UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        // JWT was already validated in JwtAuthenticationFilter; no further checks needed.
    }

    @Override
    protected UserDetails retrieveUser(String username,
            UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        Object credentials = authentication.getCredentials();
        if (credentials instanceof JwtAuthToken authToken && authToken.getUserSessionId() != null) {
            return new User(authToken.getUserSessionId(), "",
                    AuthorityUtils.createAuthorityList("USER"));
        }
        throw new UsernameNotFoundException("Cannot find user with authentication token");
    }
}
