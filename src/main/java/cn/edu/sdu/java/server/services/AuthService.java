package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.exception.BusinessException;
import cn.edu.sdu.java.server.exception.ErrorCodes;
import cn.edu.sdu.java.server.models.*;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.request.LoginRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.payload.response.JwtResponse;
import cn.edu.sdu.java.server.repositorys.*;
import cn.edu.sdu.java.server.util.CommonMethod;
import cn.edu.sdu.java.server.util.DateTimeTool;
import cn.edu.sdu.java.server.util.LoginControlUtil;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class AuthService {
    private final PersonRepository personRepository;
    private final UserRepository userRepository;
    private final UserTypeRepository userTypeRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordEncoder encoder;

    public AuthService(PersonRepository personRepository, UserRepository userRepository, UserTypeRepository userTypeRepository, StudentRepository studentRepository, TeacherRepository teacherRepository, AuthenticationManager authenticationManager, JwtService jwtService, PasswordEncoder encoder, ResourceLoader resourceLoader) {
        this.personRepository = personRepository;
        this.userRepository = userRepository;
        this.userTypeRepository = userTypeRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.encoder = encoder;
    }
    public DataResponse authenticateUser(LoginRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        
        Optional<User> op = userRepository.findByUserName(loginRequest.getUsername());
        if(op.isPresent()) {
            User user = op.get();
            
            UserType userType = user.getUserType();
            if (userType == null || userType.getName() == null) {
                throw new BusinessException(ErrorCodes.AUTH_ROLE_INVALID, "登录失败：用户角色信息缺失");
            }
            
            boolean isValidRole = Arrays.stream(EUserType.values())
                    .anyMatch(validRole -> validRole.name().equals(userType.getName()));
            
            if (!isValidRole) {
                throw new BusinessException(ErrorCodes.AUTH_ROLE_INVALID, "登录失败：用户角色无效，不允许登录");
            }
            
            // 更新登录统计信息
            user.setLastLoginTime(DateTimeTool.parseDateTime(new Date()));
            user.setLoginCount(user.getLoginCount() == null ? 1 : user.getLoginCount() + 1);
            userRepository.save(user);
        }
        
        String jwt = jwtService.generateToken(userDetails);
        JwtResponse jwtResponse = new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getPerName(),
                roles.getFirst());
        return DataResponse.success(jwtResponse);
    }

    public DataResponse getValidateCode(DataRequest dataRequest) {
        return CommonMethod.getReturnData(LoginControlUtil.getInstance().getValidateCodeDataMap());
    }




}
