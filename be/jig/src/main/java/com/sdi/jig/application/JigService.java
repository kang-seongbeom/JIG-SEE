package com.sdi.jig.application;

import com.sdi.jig.client.WorkOrderClient;
import com.sdi.jig.dto.response.*;
import com.sdi.jig.dto.response.JigModelCountResponseDto.JigModelCount;
import com.sdi.jig.dto.response.JigUpdatedCheckListResponseDto.UpdatedJig;
import com.sdi.jig.entity.nosql.JigNosqlEntity;
import com.sdi.jig.entity.rdb.JigItemRDBEntity;
import com.sdi.jig.entity.rdb.JigRDBEntity;
import com.sdi.jig.entity.rdb.JigStatsRDBEntity;
import com.sdi.jig.repository.nosql.JigNosqlRepository;
import com.sdi.jig.repository.rdb.*;
import com.sdi.jig.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

import static com.sdi.jig.util.DownTime.getDownTime;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class JigService {

    private final int MAX_COUNT = 100;
    private final int PRODUCTION_HOUR = 1000;
    private final int PRODUCTION_PROFIT = 5;
    private final int FACILITY_HOUR_OPERATING_COST = 2500;
    private final int BREAKEDOWN_COST = 11000;
    private final int MAINTANANCE_COST = 1000;
    private final int MONTH_DAY = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH);
    private final int DAY_TIME = 24;
    private final int CHECK_PEOPLE = 20;
    private final int SPLIT_NUMBER = 100000;
    private final int SPLIT_HALF = 2;
    private final int REAPIR_TIME_MIN = 5;

    private final JigRDBRepository jigRDBRepository;
    private final JigItemRDBRepository jigItemRDBRepository;
    private final JigNosqlRepository jigNosqlRepository;
    private final JigItemIOHistoryRepository jigItemIOHistoryRepository;
    private final JigItemRepairHistoryRepository jigItemRepairHistoryRepository;
    private final JigStatsRDBRepository jigStatsRDBRepository;
    private final WorkOrderClient workOrderClient;

    public JigResponseDto findByModel(String model) {
        JigRDBEntity rdb = getJigRdbEntityByModel(model);
        JigNosqlEntity nosql = getJigNosqlEntityByModel(model);

        return JigResponseDto.from(rdb, nosql);
    }

    @Transactional
    public void updateCheckList(String model, List<CheckItem> checkList) {
        JigNosqlEntity entity = getJigNosqlEntityByModel(model);
        entity.updateCheckList(checkList);
        jigNosqlRepository.save(entity);
    }

    public JigMonthResponseDto monthStatus(String accessToken, Integer year, Integer month) {
        Map<String, LocalDateTime> startAndEndDate = integerToLocalDateTime(year, month);

        LocalDateTime startDate = startAndEndDate.get("startDate");
        LocalDateTime endDate = startAndEndDate.get("endDate");

        // 폐기 갯수
        int countDelete = jigItemRDBRepository.countByDeletedTimeBetween(startDate, endDate);

        // 교체된 지그 갯수
        int countChange = jigItemIOHistoryRepository.countByInOutTimeBetweenAndStatus(startDate, endDate, IOStatus.OUT);

        // 보수 요청 갯수
        int countRepairRequest = workOrderClient.countRepairRequest(accessToken, year, month).getResult().count();

        // 보수 완료 갯수
        int countRepairFinish = jigItemRepairHistoryRepository.countByRepairTimeBetween(startDate, endDate);

        return new JigMonthResponseDto(countDelete, countChange, countRepairRequest, countRepairFinish);
    }

    public JigModelCountResponseDto jigCountStatus() {
        List<JigRDBEntity> jigList = jigRDBRepository.findAll();
        List<JigModelCount> jigModelCountList = new ArrayList<>();

        for (JigRDBEntity jig : jigList) {
            int countReady = jigItemRDBRepository.countByStatusAndJigId(JigStatus.READY, jig.getId());
            int countWarehouse = jigItemRDBRepository.countByStatusAndJigId(JigStatus.WAREHOUSE, jig.getId());
            jigModelCountList.add(JigModelCount.of(jig.getModel(), countReady, countWarehouse));
        }

        return JigModelCountResponseDto.from(MAX_COUNT, jigModelCountList);
    }

    public JigUpdatedCheckListResponseDto updatedCheckList(Integer year, Integer month) {
        Map<String, LocalDateTime> startAndEndDate = integerToLocalDateTime(year, month);

        LocalDateTime startDate = startAndEndDate.get("startDate");
        LocalDateTime endDate = startAndEndDate.get("endDate");

        List<UpdatedJig> updatedJigList =
                jigNosqlRepository.findAllByUpdatedAtBetween(startDate, endDate)
                        .map(list -> list.stream()
                                .map(UpdatedJig::of)
                                .collect(Collectors.toList())
                        )
                        .orElse(Collections.emptyList());
        return JigUpdatedCheckListResponseDto.from(updatedJigList);
    }

    public JigOptimalIntervalResponseDto jigOptimalInterval(String model) {
        JigRDBEntity jig = getJigRdbEntityByModel(model);

        List<Double> data = jigStatsRDBRepository.findAllByJigOrderByRepairCount(jig).stream()
                .map(JigStatsRDBEntity::getOptimalInterval)
                .map(JigService::roundedValue)
                .collect(Collectors.toList());

        return JigOptimalIntervalResponseDto.of(data);
    }

    public List<JigGraphResponseDto> jigGraph() {
        int startDay = 1;
        int endDay = 50;

        // 응답 리스트
        List<JigGraphResponseDto> jigGraphResponseDtoList = new ArrayList<>();

        // 현재 장착된 지그들 리스트
        List<JigItemRDBEntity> jigItemList = jigItemRDBRepository.findByStatus(JigStatus.IN);

        // 장착된 지그 별 적정 점검 주기 Map
        Map<JigItemRDBEntity, Double> optimalIntervalMap = getOptimalIntervalMap(jigItemList);

        // 장착된 지그의 적정 점검 주기 List
        List<Double> optimalIntervalList = new ArrayList<>(optimalIntervalMap.values());

        // 날짜 별 값을 확인하기 위한 for 문
        for (int day = startDay; day <= endDay; day++) {
            // 해당 점검 주기에 따른 예상 점검 횟수
            double monthCheckNumber = (double) MONTH_DAY / day;

            // 점검 리스트 지그의 적정 점검 주기 Map
            int finalDay = day;
            List<JigItemRDBEntity> checkJigList = optimalIntervalMap.entrySet().stream()
                    .filter(entry -> entry.getValue() >= finalDay)
                    .map(Map.Entry::getKey)
                    .toList();

            // 점검을 놓친 지그 갯수
            int countMissingJig = optimalIntervalList.size() - checkJigList.size();

            // 비정규 점검 소요 시간
            int downTime = getDownTime(countMissingJig);
            if (downTime == DownTime.MANY.getTime()) {
                jigGraphResponseDtoList.add(JigGraphResponseDto.of(day, -1, -1, -1, -1));
                continue;
            }

            // 정규 유지 보수 소요 시간
            int maintenanceTime = getMaintenanceTime(checkJigList);

            // 한달 소요 되는 총 비정규 점검 시간
            double monthDownTime = downTime * monthCheckNumber;
            // 한달 소요 되는 정규 유지 보수 소요 시간
            double monthMaintenanceTime = maintenanceTime * monthCheckNumber;
            // 한달 간 설비 총 가동 시간
            double monthOperatingTime = MONTH_DAY * DAY_TIME - (monthDownTime + monthMaintenanceTime);

            // 생산량
            double outputValue = monthOperatingTime * PRODUCTION_HOUR * PRODUCTION_PROFIT;

            // 생산 비용
            double costValue = monthOperatingTime * FACILITY_HOUR_OPERATING_COST
                    + monthDownTime * BREAKEDOWN_COST
                    + (double) (maintenanceTime * MAINTANANCE_COST) / day;

            // 차이
            double yieldValue = outputValue - costValue;

            jigGraphResponseDtoList.add(JigGraphResponseDto.of(
                    day,
                    outputValue / SPLIT_NUMBER,
                    costValue / SPLIT_NUMBER,
                    yieldValue / SPLIT_NUMBER,
                    countMissingJig
            ));
        }

        return jigGraphResponseDtoList;
    }

    public JigRDBEntity getJigRdbEntityByModel(String model) {
        return jigRDBRepository.findByModel(model)
                .orElseThrow(() -> new IllegalArgumentException("모델을 찾을 수 없습니다."));
    }

    public JigNosqlEntity getJigNosqlEntityByModel(String model) {
        return jigNosqlRepository.findById(model)
                .orElseThrow(() -> new IllegalArgumentException("모델을 찾을 수 없습니다."));
    }

    private Map<String, LocalDateTime> integerToLocalDateTime(Integer year, Integer month) {
        if (year == null) {
            year = Calendar.getInstance().get(Calendar.YEAR);
        }
        if (month == null) {
            month = Calendar.getInstance().get(Calendar.MONTH) + 1;
        }

        // 월의 시작과 끝
        LocalDateTime startDate = YearMonth.of(year, month).atDay(1).atStartOfDay();
        LocalDateTime endDate = YearMonth.of(year, month).atEndOfMonth().atTime(23, 59, 59);

        Map<String, LocalDateTime> startAndEndDate = new HashMap<>();
        startAndEndDate.put("startDate", startDate);
        startAndEndDate.put("endDate", endDate);

        return startAndEndDate;
    }

    private Map<JigItemRDBEntity, Double> getOptimalIntervalMap(List<JigItemRDBEntity> jigItemList) {
        // 지그 별 적정 점검 주기 List
        Map<JigItemRDBEntity, Double> optimalIntervalMap = new HashMap<>();

        for (JigItemRDBEntity jigItem : jigItemList) {
            Long jigItemId = jigItem.getId();
            Long jigModelId = jigItem.getJig().getId();
            int countRepair = jigItemRepairHistoryRepository.countByJigItemId(jigItemId);
            double optimalInterval = jigStatsRDBRepository.findByJigIdAndRepairCount(jigModelId, countRepair).getOptimalInterval();
            double optimalIntervalDouble = roundedValue(optimalInterval);
            optimalIntervalMap.put(jigItem, optimalIntervalDouble);
        }

        return optimalIntervalMap;
    }

    private Map<JigItemRDBEntity, Double>  getMaxOptimalIntervalMap(List<JigItemRDBEntity> jigItemList) {
        // 지그 모델 별 수리 횟수 기준 적정 점검 주기 Map
        Map<JigItemRDBEntity, Double> optimalIntervalMap = new HashMap<>();

        for (JigItemRDBEntity jigItem : jigItemList) {
            Long jigId = jigItem.getJig().getId();
            double maxOptimalInterval = jigStatsRDBRepository.findMaxOptimalIntervalByJigId(jigId);
            double optimalIntervalDouble = roundedValue(maxOptimalInterval);
            optimalIntervalMap.put(jigItem, optimalIntervalDouble);
        }

        return optimalIntervalMap;
    }

    private Integer getMaintenanceTime(List<JigItemRDBEntity> checkJigList) {
        double sumValue = 0;
        for (JigItemRDBEntity jigItem : checkJigList) {
            sumValue += jigItem.getJig().getId() * REAPIR_TIME_MIN;
        }

        // 합산된 값을 Integer로 변환하여 반환
        return (int) (sumValue / SPLIT_HALF) / CHECK_PEOPLE;
    }

    private static double roundedValue(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
