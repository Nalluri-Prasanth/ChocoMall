package com.chocomall.portal.security;

import com.chocomall.model.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberUserDetailsService implements UserDetailsService {

    private final MemberRepository members;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return members.findByUsername(username)
                .map(m -> new MemberUserDetails(m.getId(), m.getUsername(), m.getPassword(), m.getStatus() == 1))
                .orElseThrow(() -> new UsernameNotFoundException("Unknown member " + username));
    }
}
