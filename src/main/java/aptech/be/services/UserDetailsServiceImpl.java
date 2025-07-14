package aptech.be.services;

import aptech.be.models.UserEntity;
import aptech.be.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        System.out.println("USERNAME = " + user.getUsername());
        System.out.println("PASSWORD_HASH = " + user.getPassword());

        boolean match = new BCryptPasswordEncoder().matches("123456", user.getPassword());
        System.out.println("PASSWORD MATCHES? " + match);

        return new CustomUserDetails(user);
    }

}
