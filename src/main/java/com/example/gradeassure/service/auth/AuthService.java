package com.example.gradeassure.service.auth;

import com.example.gradeassure.config.JwtUtils;
import com.example.gradeassure.dto.request.RegisterRequest;
import com.example.gradeassure.dto.request.RegisterUserRequest;
import com.example.gradeassure.dto.response.JWTResponse;
import com.example.gradeassure.model.SchoolAdmin;
import com.example.gradeassure.model.Student;
import com.example.gradeassure.model.Teacher;
import com.example.gradeassure.model.User;
import com.example.gradeassure.model.enums.Role;
import com.example.gradeassure.repository.SchoolAdminRepository;
import com.example.gradeassure.repository.StudentRepository;
import com.example.gradeassure.repository.TeacherRepository;
import com.example.gradeassure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JavaMailSender javaMailSender;
    private final JwtUtils jwtUtils;
    private final TeacherRepository teacherRepository;
    private final SchoolAdminRepository schoolAdminRepository;
    private final StudentRepository studentRepository;

    public JWTResponse register(RegisterRequest request) {
        User user = User.builder()
                .role(Role.USER)
                .email(request.getEmail())
                .fullName(request.getFullName())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        if (userRepository.existsByEmail(request.getEmail()))
            throw new RuntimeException("Вы уже зарегистрированы email: " + request.getEmail());
        try {
            Random random = new Random();
            String code = String.valueOf(random.nextInt(99)) + String.valueOf(random.nextInt(99)) + String.valueOf(random.nextInt(99));
            SimpleMailMessage message = new SimpleMailMessage();
            System.out.println(code);
            message.setTo(request.getEmail());
            message.setSubject("Код:" + code);
            message.setText("Не кому не показывайте этот код");
            javaMailSender.send(message);
            user.setCode(code);
        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
            user.setCode("123456");
        }
        userRepository.save(user);
        Role role = roles(user, String.valueOf(request.getRoleRequest()));
        user.setRole(role);
        userRepository.save(user);
        return new JWTResponse(
                user.getEmail(),
                jwtUtils.generateToken(user.getEmail()),
                "register",
                role
        );
    }

    private Role roles(User user, String role) {
        if (Role.valueOf(role) == Role.TEACHER) {
            Teacher teacher = Teacher.builder()
                    .user(user)
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .blocked(false)
                    .build();
            teacherRepository.save(teacher);
            return Role.TEACHER;
        } else if (Role.valueOf(role) == Role.STUDENT) {
            Student student = Student.builder()
                    .user(user)
                    .email(user.getEmail())
                    .blocked(false)
                    .fullName(user.getFullName())
                    .build();
            studentRepository.save(student);
            return Role.STUDENT;
        } else if (user.getSchoolAdmin() != null || user.getStudent() != null || user.getTeacher() != null) {
            throw new RuntimeException("Вы уже зарегистрированы");
        } else {
            SchoolAdmin schoolAdmin = SchoolAdmin.builder()
                    .user(user)
                    .email(user.getEmail())
                    .blocked(false)
                    .fullName(user.getFullName())
                    .build();
            schoolAdminRepository.save(schoolAdmin);
            return Role.ADMINSCHOOL;
        }
    }

    public JWTResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("email не найден: " + email));
        if (!passwordEncoder.matches(password, user.getPassword()))
            throw new BadCredentialsException("Пароль неверный");
        String token = jwtUtils.generateToken(user.getEmail());
        return new JWTResponse(
                user.getEmail(),
                token,
                "login",
                user.getRole()
        );
    }
}