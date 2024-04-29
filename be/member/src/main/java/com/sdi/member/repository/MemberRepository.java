package com.sdi.member.repository;

import com.sdi.member.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<MemberEntity, Long> {
    Optional<MemberEntity> findByEmployeeNo(String employeeNo);
    Optional<MemberEntity> findByName(String name);
}