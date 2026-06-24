package com.covenantcode.crm.service.impl;

import com.covenantcode.crm.dto.student.StudentResponse;
import com.covenantcode.crm.exception.ResourceNotFoundException;
import com.covenantcode.crm.mapper.StudentMapper;
import com.covenantcode.crm.repository.StudentRepository;
import com.covenantcode.crm.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;

    @Override
    @Transactional(readOnly = true)
    public StudentResponse getById(Long id) {
        return studentRepository.findById(id)
                .map(studentMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Студент не найден с ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentResponse> getAll() {
        return studentRepository.findAll().stream()
                .map(studentMapper::toResponse)
                .collect(Collectors.toList());
    }
}
