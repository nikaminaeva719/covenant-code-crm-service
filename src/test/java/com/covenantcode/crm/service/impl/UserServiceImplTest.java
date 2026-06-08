package com.covenantcode.crm.service.impl;

import com.covenantcode.crm.dto.user.UserResponse;
import com.covenantcode.crm.entity.Role;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.entity.enums.RoleName;
import com.covenantcode.crm.mapper.UserMapper;
import com.covenantcode.crm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private Pageable pageable;
    private OffsetDateTime now;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 20);
        now = OffsetDateTime.now();
    }

    @Test
    public void getAll_success() {
        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setName(RoleName.ADMIN);

        Role managerRole = new Role();
        managerRole.setId(2L);
        managerRole.setName(RoleName.MANAGER);

        User user1 = User.builder()
                .id(1L)
                .firstName("Admin")
                .lastName("System")
                .email("admin@covenantcode.ru")
                .phone(null)
                .role(adminRole)
                .enabled(true)
                .createdAt(now)
                .build();

        User user2 = User.builder()
                .id(2L)
                .firstName("Иван")
                .lastName("Петров")
                .email("ivan.petrov@company.ru")
                .phone("+79161234567")
                .role(managerRole)
                .enabled(true)
                .createdAt(now)
                .build();

        List<User> users = List.of(user1, user2);
        Page<User> userPage = new PageImpl<>(users, pageable, users.size());

        UserResponse response1 = UserResponse.builder()
                .id(1L)
                .firstName("Admin")
                .lastName("System")
                .email("admin@covenantcode.ru")
                .phone(null)
                .role("ADMIN")
                .enabled(true)
                .createdAt(now.toString())
                .build();

        UserResponse response2 = UserResponse.builder()
                .id(2L)
                .firstName("Иван")
                .lastName("Петров")
                .email("ivan.petrov@company.ru")
                .phone("+79161234567")
                .role("MANAGER")
                .enabled(true)
                .createdAt(now.toString())
                .build();

        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(userMapper.toResponse(user1)).thenReturn(response1);
        when(userMapper.toResponse(user2)).thenReturn(response2);

        Page<UserResponse> result = userService.getAll(pageable);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());

        UserResponse firstResponse = result.getContent().get(0);
        assertEquals("ADMIN", firstResponse.getRole());

        UserResponse secondResponse = result.getContent().get(1);
        assertEquals("MANAGER", secondResponse.getRole());

        verify(userRepository).findAll(pageable);
        verify(userMapper).toResponse(user1);
        verify(userMapper).toResponse(user2);
    }

    @Test
    public void getAll_emptyList() {
        Page<User> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(userRepository.findAll(pageable)).thenReturn(emptyPage);

        Page<UserResponse> result = userService.getAll(pageable);

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());

        verify(userRepository).findAll(pageable);
        verify(userMapper, never()).toResponse(any());
    }
}
