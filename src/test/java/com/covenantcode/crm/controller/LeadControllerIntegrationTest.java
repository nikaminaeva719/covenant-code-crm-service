package com.covenantcode.crm.controller;

import com.covenantcode.crm.BaseIntegrationTest;
import com.covenantcode.crm.dto.lead.LeadCreateRequest;
import com.covenantcode.crm.entity.Course;
import com.covenantcode.crm.entity.Role;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.entity.enums.CourseStatus;
import com.covenantcode.crm.entity.Lead;
import com.covenantcode.crm.entity.enums.LeadStatus;
import com.covenantcode.crm.entity.enums.RoleName;
import com.covenantcode.crm.repository.CourseRepository;
import com.covenantcode.crm.repository.RoleRepository;
import com.covenantcode.crm.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import com.covenantcode.crm.repository.LeadRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "MANAGER")
public class LeadControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LeadRepository leadRepository;

    private Course testCourse;
    private User testManager;
    private final String baseUrl = "/api/v1/leads";

    @BeforeEach
    void setUp() {
        testCourse = new Course();
        testCourse.setTitle("Test Course");
        testCourse.setDescription("Integration test course");
        testCourse.setPrice(BigDecimal.valueOf(199.99));
        testCourse.setDurationInWeeks(8);
        testCourse.setStatus(CourseStatus.ACTIVE);
        testCourse = courseRepository.save(testCourse);

        Role managerRole = roleRepository.findByName(RoleName.MANAGER)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName(RoleName.MANAGER);
                    return roleRepository.save(newRole);
                });

        testManager = new User();
        testManager.setFirstName("Manager");
        testManager.setLastName("Test");
        testManager.setEmail("manager@test.com");
        testManager.setPassword(passwordEncoder.encode("password"));
        testManager.setRole(managerRole);
        testManager.setEnabled(true);
        testManager = userRepository.save(testManager);
    }

    @Test
    @DisplayName("Создание лида со всеми полями -> HTTP 201, статус NEW, курс и менеджер проставлены")
    void createLead_fullFields_shouldReturn201() throws Exception {
        LeadCreateRequest request = new LeadCreateRequest();
        request.setFirstName("Ivan");
        request.setLastName("Petrov");
        request.setPhone("+79001234567");
        request.setEmail("ivan@example.com");
        request.setSource("website");
        request.setInterestedCourseId(testCourse.getId());
        request.setAssignedManagerId(testManager.getId());
        request.setComment("Integration test");

        mockMvc.perform(post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value(LeadStatus.NEW.name()))
                .andExpect(jsonPath("$.interestedCourse.id").value(testCourse.getId()))
                .andExpect(jsonPath("$.interestedCourse.title").value(testCourse.getTitle()))
                .andExpect(jsonPath("$.assignedManager.id").value(testManager.getId()))
                .andExpect(jsonPath("$.assignedManager.firstName").value(testManager.getFirstName()))
                .andExpect(jsonPath("$.assignedManager.lastName").value(testManager.getLastName()));
    }

    @Test
    @DisplayName("Создание лида только с обязательными полями -> HTTP 201, статус NEW, курс и менеджер null")
    void createLead_minimalFields_shouldReturn201() throws Exception {
        LeadCreateRequest request = new LeadCreateRequest();
        request.setFirstName("John");
        request.setPhone("+79998887766");

        mockMvc.perform(post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value(LeadStatus.NEW.name()))
                .andExpect(jsonPath("$.interestedCourse").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.assignedManager").value(Matchers.nullValue()));
    }

    @Test
    @DisplayName("Передан несуществующий interestedCourseId -> HTTP 404, тип ошибки resource-not-found")
    void createLead_invalidCourseId_shouldReturn404() throws Exception {
        LeadCreateRequest request = new LeadCreateRequest();
        request.setFirstName("Anna");
        request.setPhone("+79001112233");
        request.setInterestedCourseId(9999L);
        request.setAssignedManagerId(testManager.getId());

        mockMvc.perform(post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("resource-not-found"))
                .andExpect(jsonPath("$.detail").value(containsString("Course")))
                .andExpect(jsonPath("$.detail").value(containsString("9999")));
    }

    @Test
    @DisplayName("Отсутствует firstName -> HTTP 400, ошибка валидации для поля firstName")
    void createLead_emptyFirstName_shouldReturn400() throws Exception {
        LeadCreateRequest request = new LeadCreateRequest();
        request.setPhone("+79001112233");
        mockMvc.perform(post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("validation-error"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").value("firstName"))
                .andExpect(jsonPath("$.errors[0].message").value(containsString("обязательно")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getLead_whenLeadExists_thenHttp200WithAllFields() throws Exception {

        Lead lead = new Lead();
        lead.setFirstName("Ivan");
        lead.setLastName("Petrov");
        lead.setEmail("ivan@example.com");
        lead.setPhone("+79990000000");
        lead.setStatus(LeadStatus.NEW);

        Lead saved = leadRepository.saveAndFlush(lead);

        mockMvc.perform(get("/api/v1/leads/{id}", saved.getId())
                        .accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(saved.getId().intValue())))
                .andExpect(jsonPath("$.status", is(saved.getStatus().name())));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getLead_whenLeadNotExists_thenHttp404() throws Exception {

        mockMvc.perform(get("/api/v1/leads/{id}", 9999L)
                        .accept(APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type", is("resource-not-found")));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void getLead_whenUserHasTeacherRole_thenHttp403() throws Exception {
        Lead lead = new Lead();
        lead.setFirstName("Ivan");
        lead.setPhone("+79990000000");
        lead.setStatus(LeadStatus.NEW);
        Lead saved = leadRepository.saveAndFlush(lead);

        mockMvc.perform(get("/api/v1/leads/{id}", saved.getId())
                        .accept(APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

// Добавьте в существующий класс LeadControllerIntegrationTest

    // ===== Тесты получения всех лидов с пагинацией и фильтрацией =====

    @Test
    @DisplayName("Получить все лиды без фильтров - возвращает все лиды постранично")
    void testControllerIntegrationGetAllLeadsNoFiltersShouldReturnAllWithPagination() throws Exception {
        // Создаем 3 лида с разными статусами
        Lead lead1 = createLeadWithStatus(LeadStatus.NEW);
        Lead lead2 = createLeadWithStatus(LeadStatus.IN_PROGRESS);
        Lead lead3 = createLeadWithStatus(LeadStatus.CONTACTED);

        // Получаем все ID сохранённых лидов
        List<Long> leadIds = List.of(lead1.getId(), lead2.getId(), lead3.getId());

        // Выполняем один запрос и проверяем результат
        mockMvc.perform(get(baseUrl)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.content", Matchers.hasSize(3)))
                .andExpect(jsonPath("$.content[*].id", Matchers.containsInAnyOrder(
                        lead1.getId().intValue(),
                        lead2.getId().intValue(),
                        lead3.getId().intValue()
                )));
    }


    @Test
    @DisplayName("Фильтрация лидов по статусу NEW - возвращает только NEW лиды")
    void testControllerIntegrationGetAllLeadsByStatusNewShouldReturnOnlyNew() throws Exception {
        // Создаем 2 NEW лида и 1 IN_PROGRESS
        Lead lead1 = createLeadWithStatus(LeadStatus.NEW);
        Lead lead2 = createLeadWithStatus(LeadStatus.NEW);
        Lead lead3 = createLeadWithStatus(LeadStatus.IN_PROGRESS);

        mockMvc.perform(get(baseUrl)
                        .param("status", LeadStatus.NEW.name())
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.content[*].status", Matchers.everyItem(is(LeadStatus.NEW.name()))));
    }

    @Test
    @DisplayName("Поиск лидов по части телефона - возвращает лиды с совпадающим номером")
    void testControllerIntegrationSearchLeadsByPhonePartShouldReturnMatchingLeads() throws Exception {
        // Создаем лид с нужным телефоном
        Lead lead = new Lead();
        lead.setFirstName("Client");
        lead.setPhone("+79161234567");
        lead.setStatus(LeadStatus.NEW);
        leadRepository.saveAndFlush(lead);

        mockMvc.perform(get(baseUrl)
                        .param("search", "9161")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].phone", is("+79161234567")));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("Попытка доступа с ролью TEACHER к эндпоинту getAll -> HTTP 403")
    void testControllerIntegrationGetAllLeadsWithTeacherRoleShouldReturn403() throws Exception {
        // Создаем несколько лидов для теста
        for (int i = 0; i < 3; i++) {
            createLeadWithStatus(LeadStatus.NEW);
        }

        mockMvc.perform(get(baseUrl)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isForbidden());
    }

    // ===== Вспомогательный метод для создания лидов =====
    private Lead createLeadWithStatus(LeadStatus status) {
        Lead lead = new Lead();
        lead.setFirstName("Test_" + status.name());
        lead.setPhone("+7900" + (int)(Math.random() * 100000000));
        lead.setStatus(status);
        return leadRepository.saveAndFlush(lead);
    }

}
