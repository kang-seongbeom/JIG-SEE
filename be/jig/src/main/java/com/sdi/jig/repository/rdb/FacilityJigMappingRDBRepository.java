package com.sdi.jig.repository.rdb;

import com.sdi.jig.entity.rdb.FacilityJigMappingRDBEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FacilityJigMappingRDBRepository extends JpaRepository<FacilityJigMappingRDBEntity, Long> {

    List<FacilityJigMappingRDBEntity> findByJigId(Long jigId);
    List<FacilityJigMappingRDBEntity> findByFacilityId(Long facilityId);
}
