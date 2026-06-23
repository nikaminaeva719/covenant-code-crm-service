package com.covenantcode.crm.service.impl;

import com.covenantcode.crm.dto.lead.LeadCreateRequest;
import com.covenantcode.crm.dto.lead.LeadResponse;
import com.covenantcode.crm.entity.Lead;
import com.covenantcode.crm.entity.enums.LeadStatus;
import com.covenantcode.crm.exception.ResourceNotFoundException;
import com.covenantcode.crm.mapper.LeadMapper;
import com.covenantcode.crm.repository.CourseRepository;
import com.covenantcode.crm.repository.LeadRepository;
import com.covenantcode.crm.repository.LeadSpecifications;
import com.covenantcode.crm.repository.UserRepository;
import com.covenantcode.crm.service.LeadService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LeadServiceImpl implements LeadService {

    private final LeadRepository leadRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final LeadMapper leadMapper;

    @Override
    @Transactional
    public LeadResponse create(LeadCreateRequest leadCreateRequest) {
        var course = leadCreateRequest.getInterestedCourseId() != null
                ? courseRepository.findById(leadCreateRequest.getInterestedCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", leadCreateRequest.getInterestedCourseId()))
                : null;

        var manager = leadCreateRequest.getAssignedManagerId() != null
                ? userRepository.findById(leadCreateRequest.getAssignedManagerId())
                .orElseThrow(() -> new ResourceNotFoundException("User", leadCreateRequest.getAssignedManagerId()))
                : null;

        Lead lead = new Lead();
        lead.setFirstName(leadCreateRequest.getFirstName());
        lead.setLastName(leadCreateRequest.getLastName());
        lead.setPhone(leadCreateRequest.getPhone());
        lead.setEmail(leadCreateRequest.getEmail());
        lead.setSource(leadCreateRequest.getSource());
        lead.setComment(leadCreateRequest.getComment());
        lead.setInterestedCourse(course);
        lead.setAssignedManager(manager);
        lead.setStatus(LeadStatus.NEW);

        Lead saved = leadRepository.save(lead);
        return leadMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public LeadResponse getById(Long id) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead с id " + id + " не найден"));
        LeadResponse response = leadMapper.toResponse(lead);
        response.setCommentsCount(0L);
        return response;
    }

    @Transactional(readOnly = true)
    public Page<LeadResponse> getAll(
            String search,
            LeadStatus status,
            Long assignedManagerId,
            Long interestedCourseId,
            Pageable pageable
    ) {
        Specification<Lead> spec = Specification.where(null);

        // --- Используем методы LeadSpecifications ---
        if (StringUtils.hasText(search)) {
            spec = spec.and(LeadSpecifications.searchByText(search));
        }

        if (status != null) {
            spec = spec.and(LeadSpecifications.hasStatus(status));
        }

        if (assignedManagerId != null) {
            spec = spec.and(LeadSpecifications.assignedToManager(assignedManagerId));
        }

        if (interestedCourseId != null) {
            spec = spec.and(LeadSpecifications.interestedInCourse(interestedCourseId));
        }

        return leadRepository.findAll(spec, pageable)
                .map(leadMapper::toResponse);
    }
}
