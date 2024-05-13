package com.sdi.jig.repository.rdb;

import com.sdi.jig.entity.rdb.JigItemIOHistoryRDBEntity;
import com.sdi.jig.util.IOStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JigItemIOHistoryRepository extends JpaRepository<JigItemIOHistoryRDBEntity, Long> {

    List<JigItemIOHistoryRDBEntity> findByJigItemIdAndStatus(Long jigItemId, IOStatus status);

    Optional<JigItemIOHistoryRDBEntity> findFirstByJigItemIdAndStatusOrderByInOutTime(Long jigItemId, IOStatus status);
}