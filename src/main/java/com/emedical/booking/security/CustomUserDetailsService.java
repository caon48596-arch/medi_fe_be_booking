package com.emedical.booking.security;

import com.emedical.booking.repository.DoctorRepository;
import com.emedical.booking.repository.UserRepository;
import com.emedical.booking.entity.Doctor;
import com.emedical.booking.entity.User;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;

    public UserDetails loadUserOnlyByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản bệnh nhân: " + username));

        String role = user.getRole();
        if (role == null || role.isBlank()) {
            throw new UsernameNotFoundException("Tài khoản chưa được gán quyền");
        }

        String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        GrantedAuthority grantedAuthority = new SimpleGrantedAuthority(authority);

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(grantedAuthority)
                .build();
    }

    public UserDetails loadDoctorByUsername(String username) throws UsernameNotFoundException {
        Doctor doctor = doctorRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản bác sĩ: " + username));

        return org.springframework.security.core.userdetails.User
                .withUsername(doctor.getUsername())
                .password(doctor.getPassword())
                .authorities("ROLE_DOCTOR")
                .disabled(!doctor.isActive())
                .build();
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Tìm tài khoản trong bảng User trước
        User user = userRepository.findByUsername(username).orElse(null);

        if (user != null) {
            String role = user.getRole();
            if (role == null || role.isBlank()) {
                throw new UsernameNotFoundException("Tài khoản chưa được gán quyền");
            }

            String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
            GrantedAuthority grantedAuthority = new SimpleGrantedAuthority(authority);

            return org.springframework.security.core.userdetails.User
                    .withUsername(user.getUsername())
                    .password(user.getPassword())
                    .authorities(grantedAuthority)
                    .build();
        }

        // 2. Nếu không có trong User, tìm trong bảng Doctor
        Doctor doctor = doctorRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản: " + username));

        // 3. Tạo thông tin xác thực cho bác sĩ
        return org.springframework.security.core.userdetails.User
                .withUsername(doctor.getUsername())
                .password(doctor.getPassword())
                .authorities("ROLE_DOCTOR")
                .disabled(!doctor.isActive())
                .build();
    }
}
