package io.security.corespringsecurity.security.listener;

import io.security.corespringsecurity.domain.*;
import io.security.corespringsecurity.repository.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Component
public class SetupDataLoader implements ApplicationListener<ContextRefreshedEvent> {

    private boolean alreadySetup = false;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ResourcesRepository resourcesRepository;

    @Autowired
    private RoleHierarchyRepository roleHierarchyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccessIpRepository accessIpRepository;

    private static AtomicInteger count = new AtomicInteger(0);

    @Override
    @Transactional
    public void onApplicationEvent(final ContextRefreshedEvent event) {

        if (alreadySetup) {
            return;
        }

        setupSecurityResources();
        setupAccessIpData();

        alreadySetup = true;
    }



    private void setupSecurityResources() {
        Set<Role> roles = new HashSet<>();
        Role adminRole = createRoleIfNotFound("ROLE_ADMIN", "관리자");
        roles.add(adminRole);
        createResourceIfNotFound("/admin/**", "", roles, "url");
        User user = createUserIfNotFound("admin@gmail.com", "pass", roles);
        
        Set<Role> roles1 = new HashSet<>();

        Role managerRole = createRoleIfNotFound("ROLE_MANAGER", "매니저");
        roles1.add(managerRole);
        createResourceIfNotFound("io.security.corespringsecurity.test.method.MethodService.methodTest", "", roles1, "method");
        createResourceIfNotFound("io.security.corespringsecurity.test.method.MethodService.innerCallMethodTest", "", roles1, "method");
        createResourceIfNotFound("execution(* io.security.corespringsecurity.test.aop.*Service.*(..))", "", roles1, "pointcut");
        createUserIfNotFound("manager@gmail.com", "pass", roles1);
        createRoleHierarchyIfNotFound(managerRole, adminRole);
        
        Set<Role> roles2 = new HashSet<>();

        Role directorRole = createRoleIfNotFound("ROLE_DIRECTOR", "디렉터");
        roles2.add(directorRole);
        createResourceIfNotFound("/director/**", "", roles2, "url");
        createUserIfNotFound("director@gmail.com", "pass", roles2);
        createRoleHierarchyIfNotFound(directorRole, adminRole);

        Set<Role> roles3 = new HashSet<>();

        Role childRole1 = createRoleIfNotFound("ROLE_USER", "정회원");
        roles3.add(childRole1);
        createResourceIfNotFound("/users/**", "", roles3, "url");
        createUserIfNotFound("onjsdnjs@gmail.com", "pass", roles3);
        createRoleHierarchyIfNotFound(childRole1, managerRole);

        Set<Role> roles4 = new HashSet<>();

        Role childRole3 = createRoleIfNotFound("ROLE_IUSER", "준회원");
        roles4.add(childRole3);
        createResourceIfNotFound("/users/**", "", roles4, "url");
        createUserIfNotFound("onjsdnjs@daum.com", "pass", roles4);
        createRoleHierarchyIfNotFound(childRole3, managerRole);

    }

    @Transactional
    public Role createRoleIfNotFound(String roleName, String roleDesc) {

        Role role = roleRepository.findByRoleName(roleName);

        if (role == null) {
            role = Role.builder()
                    .roleName(roleName)
                    .roleDesc(roleDesc)
                    .build();
        }
        return roleRepository.save(role);
    }

    @Transactional
    public User createUserIfNotFound(String userName, String password, Set<Role> roleSet) {

        User user = userRepository.findByUsername(userName);

        if (user == null) {
            user = User.builder()
                    .username(userName)
                    .password(passwordEncoder.encode(password))
                    .enabled(true)
                    .userRoles(roleSet)
                    .build();
        }
        return userRepository.save(user);
    }

    @Transactional
    public Resources createResourceIfNotFound(String resourceName, String httpMethod, Set<Role> roleSet, String resourceType) {
        Resources resources = resourcesRepository.findByResourceNameAndHttpMethod(resourceName, httpMethod);

        if (resources == null) {
            resources = Resources.builder()
                    .resourceName(resourceName)
                    .roleSet(roleSet)
                    .httpMethod(httpMethod)
                    .resourceType(resourceType)
                    .ordernum(count.incrementAndGet())
                    .build();
        }
        return resourcesRepository.save(resources);
    }

    @Transactional
    public void createRoleHierarchyIfNotFound(Role childRole, Role parentRole) {

        RoleHierarchy roleHierarchy = roleHierarchyRepository.findByChildName(parentRole.getRoleName());
        if (roleHierarchy == null) {
            roleHierarchy = RoleHierarchy.builder()
                    .childName(parentRole.getRoleName())
                    .build();
        }
        RoleHierarchy parentRoleHierarchy = roleHierarchyRepository.save(roleHierarchy);

        roleHierarchy = roleHierarchyRepository.findByChildName(childRole.getRoleName());
        if (roleHierarchy == null) {
            roleHierarchy = RoleHierarchy.builder()
                    .childName(childRole.getRoleName())
                    .build();
        }

        RoleHierarchy childRoleHierarchy = roleHierarchyRepository.save(roleHierarchy);
        childRoleHierarchy.setParentName(parentRoleHierarchy);
    }

    private void setupAccessIpData() {
        AccessIp byIpAddress = accessIpRepository.findByIpAddress("127.0.0.1");
        if (byIpAddress == null) {
        AccessIp accessIp = AccessIp.builder()
                    .ipAddress("127.0.0.1")
                    .build();
        accessIpRepository.save(accessIp);
        }

    }
}