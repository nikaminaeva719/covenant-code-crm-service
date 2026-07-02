package com.covenantcode.crm.service;

import com.covenantcode.crm.dto.student.StudentCreateRequest;
import com.covenantcode.crm.dto.student.StudentResponse;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.dto.student.StudentUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StudentService {

    StudentResponse getById(Long id, User currentUser);

    List<StudentResponse> getAll();

    Page<StudentResponse> getAll(String search, Pageable pageable);

    StudentResponse update(Long id, StudentUpdateRequest request);

    StudentResponse create(StudentCreateRequest studentCreateRequest);

    void deleteById(Long id);
}
