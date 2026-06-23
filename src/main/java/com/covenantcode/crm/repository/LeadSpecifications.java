package com.covenantcode.crm.repository;

import com.covenantcode.crm.entity.Lead;
import com.covenantcode.crm.entity.enums.LeadStatus;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public final class LeadSpecifications {

    private LeadSpecifications() {
        throw new IllegalStateException("Utility class");
    }

    // --- Поиск по статусу ---
    public static Specification<Lead> hasStatus(LeadStatus status) {
        return (root, query, cb) ->
                status == null
                        ? cb.conjunction()
                        : cb.equal(root.get("status"), status);
    }

    // --- Поиск по менеджеру ---
    public static Specification<Lead> assignedToManager(Long managerId) {
        return (root, query, cb) ->
                managerId == null
                        ? cb.conjunction()
                        : cb.equal(root.join("assignedManager").get("id"), managerId);
    }

    // --- Поиск по курсу ---
    public static Specification<Lead> interestedInCourse(Long courseId) {
        return (root, query, cb) ->
                courseId == null
                        ? cb.conjunction()
                        : cb.equal(root.join("interestedCourse").get("id"), courseId);
    }

    // --- Поиск по тексту (регистронезависимый LIKE) ---
    public static Specification<Lead> searchByText(String search) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(search)) {
                return cb.conjunction(); // Пустое условие, если поиск пустой
            }

            // Приводим строку поиска к нижнему регистру для регистронезависимости
            String searchLower = "%" + search.toLowerCase() + "%";

            // Создаем условия для каждого поля
            Predicate firstName = cb.like(
                    cb.lower(root.get("firstName")),
                    searchLower
            );

            Predicate lastName = cb.like(
                    cb.lower(root.get("lastName")),
                    searchLower
            );

            Predicate email = cb.like(
                    cb.lower(root.get("email")),
                    searchLower
            );

            Predicate phone = cb.like(  // Phone НЕ приводим к нижнему регистру (если это номер)
                    root.get("phone"),
                    "%" + search + "%"
            );

            // Объединяем все условия через OR
            return cb.or(firstName, lastName, email, phone);
        };
    }
}

