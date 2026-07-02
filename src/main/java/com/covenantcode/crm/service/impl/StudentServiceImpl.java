package com.covenantcode.crm.service.impl;

import com.covenantcode.crm.dto.student.StudentCreateRequest;
import com.covenantcode.crm.dto.student.StudentResponse;
import com.covenantcode.crm.dto.student.StudentUpdateRequest;
import com.covenantcode.crm.entity.Student;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.entity.enums.GroupStatus;
import com.covenantcode.crm.entity.enums.RoleName;
import com.covenantcode.crm.exception.ConflictException;
import com.covenantcode.crm.exception.ResourceNotFoundException;
import com.covenantcode.crm.mapper.StudentMapper;
import com.covenantcode.crm.repository.StudentRepository;
import com.covenantcode.crm.repository.StudentSpecifications;
import com.covenantcode.crm.repository.StudyGroupRepository;
import com.covenantcode.crm.repository.UserRepository;
import com.covenantcode.crm.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;
    private final UserRepository userRepository;
    private final StudyGroupRepository studyGroupRepository;

    @Override
    @Transactional(readOnly = true)
    public StudentResponse getById(Long id, User currentUser) {

        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student с id " + id + " не найден"));

        RoleName roleName = currentUser.getRole().getName();

        boolean isStaff = roleName == RoleName.ADMIN || roleName == RoleName.MANAGER;

        boolean isOwner = student.getUser() != null &&
                student.getUser().getId().equals(currentUser.getId());

        if (isStaff || isOwner) {
            return studentMapper.toResponse(student);
        }

        if (roleName == RoleName.TEACHER) {
            if (studyGroupRepository.existsByTeacherAndStudentsContaining(currentUser, student)) {
                return studentMapper.toResponse(student);
            }
        }

        throw new AccessDeniedException("У вас нет прав для просмотра данных этого студента");
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentResponse> getAll() {
        return studentRepository.findAll().stream()
                .map(studentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StudentResponse create(StudentCreateRequest request) {
        User user = null;

        if (request.getUserId() != null) {
            user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", request.getUserId()));

            if (studentRepository.existsByUser_Id(request.getUserId())) {
                throw new ConflictException(
                        String.format("Пользователь с id %d уже привязан к другому студенту", request.getUserId())
                );
            }
        }

        Student student = Student.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .birthDate(request.getBirthDate())
                .user(user)
                .build();

        Student savedStudent = studentRepository.saveAndFlush(student);

        return studentMapper.toResponse(savedStudent);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StudentResponse> getAll(String search, Pageable pageable) {
        Specification<Student> spec = Specification.where(null);

        if (StringUtils.hasText(search)) {
            spec = spec.and(StudentSpecifications.searchByText(search));
        }
        return studentRepository.findAll(spec, pageable)
                .map(studentMapper::toResponse);
    }

    @Override
    @Transactional
    public StudentResponse update(Long id, StudentUpdateRequest request) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student с id " + id + " не найден"));
        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        student.setPhone(request.getPhone());
        student.setEmail(request.getEmail());
        student.setBirthDate(request.getBirthDate());
        Student savedStudent = studentRepository.save(student);
        return studentMapper.toResponse(savedStudent);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {

        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student с id " + id + " не найден"));

        boolean isInActiveGroup = studyGroupRepository.existsByStudents_IdAndStatus(id, GroupStatus.ACTIVE);
        if (isInActiveGroup) {
            throw new ConflictException("Студент с id " + id + " состоит в активной учебной группе и не может быть удалён");
        }

        studentRepository.delete(student);
    }
}
