package com.travelmate.service.impl;

import com.travelmate.dto.request.LoginRequest;
import com.travelmate.dto.request.RegisterRequest;
import com.travelmate.dto.response.AuthResponse;
import com.travelmate.entity.User;
import com.travelmate.enums.Role;
import com.travelmate.enums.UserStatus;
import com.travelmate.exception.BadRequestException;
import com.travelmate.exception.UnauthorizedException;
import com.travelmate.repository.UserRepository;
import com.travelmate.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * AuthServiceImpl - Cai dat cu the cac nghiep vu xac thuc.
 *
 * Day la noi viet code xu ly thuc su cho register va login.
 *
 * Annotation giai thich:
 * - @Service: danh dau day la bean Service, Spring se tu dong quan ly
 * - @RequiredArgsConstructor: Lombok tu dong tao constructor voi cac field final,
 *   tuong duong voi @Autowired tren constructor (cach inject dependency tot nhat)
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /**
     * UserRepository: de thao tac voi bang users trong DB.
     * PasswordEncoder: de ma hoa va so sanh mat khau.
     *
     * Dung "final" de:
     * 1. Dam bao khong bi thay doi sau khi khoi tao
     * 2. Ket hop voi @RequiredArgsConstructor de tu dong inject
     */
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Xu ly dang ky tai khoan moi.
     *
     * Buoc 1: Kiem tra email da ton tai chua
     * - Neu co: nem BadRequestException
     * - Neu chua: tiep tuc
     *
     * Buoc 2: Tao entity User tu du lieu request
     * - Ma hoa password bang BCrypt truoc khi luu
     * - Gan role mac dinh la USER
     * - Gan status mac dinh la ACTIVE
     *
     * Buoc 3: Luu vao database
     *
     * Buoc 4: Tra ve AuthResponse (KHONG chua password)
     */
    @Override
    public AuthResponse register(RegisterRequest request) {

        // Buoc 1: Kiem tra email trung
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email da ton tai trong he thong");
        }

        // Buoc 2: Tao entity User
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // Ma hoa password
                .phone(request.getPhone())
                .role(Role.USER)            // Mac dinh role la USER
                .status(UserStatus.ACTIVE)  // Mac dinh trang thai la ACTIVE
                .build();

        // Buoc 3: Luu vao database
        // save() se INSERT neu user chua co id, UPDATE neu da co
        User savedUser = userRepository.save(user);

        // Buoc 4: Tra ve response
        return buildAuthResponse(savedUser, "Dang ky thanh cong");
    }

    /**
     * Xu ly dang nhap.
     *
     * Buoc 1: Tim user theo email
     * - Neu khong tim thay: nem UnauthorizedException
     *
     * Buoc 2: So sanh password
     * - passwordEncoder.matches(rawPassword, encodedPassword)
     * - Neu khong khop: nem UnauthorizedException
     *
     * Buoc 3: Kiem tra trang thai tai khoan
     * - Neu BLOCKED: nem UnauthorizedException
     *
     * Buoc 4: Tra ve AuthResponse
     *
     * Luu y: Thong bao loi chung chung "Email hoac mat khau khong dung"
     * thay vi noi cu the "email khong ton tai" hay "password sai"
     * de bao mat (tranh ke xau biet email nao da dang ky).
     */
    @Override
    public AuthResponse login(LoginRequest request) {

        // Buoc 1: Tim user theo email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Email hoac mat khau khong dung"));

        // Buoc 2: So sanh password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Email hoac mat khau khong dung");
        }

        // Buoc 3: Kiem tra trang thai tai khoan
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new UnauthorizedException("Tai khoan da bi khoa. Vui long lien he admin.");
        }

        // Buoc 4: Tra ve response
        return buildAuthResponse(user, "Dang nhap thanh cong");
    }

    /**
     * Method noi bo: tao AuthResponse tu entity User.
     * Dung chung cho ca register va login de tranh lap code.
     *
     * KHONG BAO GIO dua password vao response.
     */
    private AuthResponse buildAuthResponse(User user, String message) {
        return AuthResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())     // Chuyen enum -> String
                .message(message)
                .build();
    }
}
