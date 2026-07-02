package com.covenantcode.crm.repository;

import com.covenantcode.crm.entity.Student;
import com.covenantcode.crm.entity.StudyGroup;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.entity.enums.GroupStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyGroupRepository extends JpaRepository<StudyGroup, Long> {
    boolean existsByCourseIdAndStatus(Long courseId, GroupStatus status);

    boolean existsByTeacherAndStudentsContaining(User teacher, Student student);

    boolean existsByStudents_IdAndStatus(Long studentId, GroupStatus status);
}
