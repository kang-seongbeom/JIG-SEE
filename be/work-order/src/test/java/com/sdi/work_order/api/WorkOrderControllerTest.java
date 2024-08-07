package com.sdi.work_order.api;

import com.sdi.work_order.entity.WorkOrderRDBEntity;
import com.sdi.work_order.repository.WorkOrderNosqlRepository;
import com.sdi.work_order.repository.WorkOrderRDBRepository;
import com.sdi.work_order.util.WorkOrderCheckItem;
import com.sdi.work_order.util.WorkOrderStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorkOrderRDBRepository workOrderRDBRepository;

    @Autowired
    private WorkOrderNosqlRepository workOrderNosqlRepository;

    @Test
    @DisplayName("상세조회")
    void detail() throws Exception {
        // given
        Long workOrderId = 8L;
        WorkOrderRDBEntity rdb = workOrderRDBRepository.findById(workOrderId).get();

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .get("/v1/work-order/detail")
                .param("work-order-id", String.valueOf(workOrderId))
                .contentType(MediaType.APPLICATION_JSON);

        // when
        ResultActions perform = mockMvc.perform(request);

        // then
        perform.andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(workOrderId))
                .andExpect(jsonPath("$.result.status").value(rdb.getStatus().toString()));
    }

    @Test
    @DisplayName("전체 조회")
    void all() throws Exception {
        // given
        int page = 1;
        int size = 5;
        long totalPageSize = Math.max(1, workOrderRDBRepository.count() / size);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .get("/v1/work-order/all")
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size))
                .contentType(MediaType.APPLICATION_JSON);

        // when
        ResultActions perform = mockMvc.perform(request);

        // then
        perform.andExpect(status().isOk())
                .andExpect(jsonPath("$.result.currentPage").value(String.valueOf(page)))
                .andExpect(jsonPath("$.result.endPage").value(String.valueOf(totalPageSize)));
    }

    @Test
    @DisplayName("상태 그룹핑")
    void grouping() throws Exception {
        // given
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .get("/v1/work-order/grouping")
                .contentType(MediaType.APPLICATION_JSON);

        // when
        ResultActions perform = mockMvc.perform(request);

        // then
        perform.andExpect(status().isOk());
        String contentAsString = perform.andReturn().getResponse().getContentAsString();
        System.out.println(contentAsString);
    }

    @Test
    @DisplayName("사번 조회")
    void findByPersonEmployeeNo() throws Exception {
        // given
        String employeeNo = "creator1";
        int page = 1;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        int totalElements = (int) workOrderRDBRepository.findAllByCreatorEmployeeNoOrderByCreatedAtDesc(employeeNo, pageable).getTotalElements();

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .get("/v1/work-order")
                .param("employee-no", employeeNo)
                .contentType(MediaType.APPLICATION_JSON);

        // when
        ResultActions perform = mockMvc.perform(request);

        // then
        perform.andExpect(status().isOk())
                .andExpect(jsonPath("$.result.list").isArray())
                .andExpect(jsonPath("$.result.list", hasSize(totalElements)));
    }

    @Test
    @DisplayName("WO 생성")
    void create() throws Exception {
        // given
        String serialNo = "js2";
        JSONObject body = new JSONObject();
        body.put("serialNo", serialNo);
        int beforeSizeRDB = (int) workOrderRDBRepository.count();
        int beforeSizeNosql = (int) workOrderNosqlRepository.count();

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .post("/v1/work-order")
                .content(body.toString())
                .contentType(MediaType.APPLICATION_JSON);

        // when
        ResultActions perform = mockMvc.perform(request);

        // then
        perform.andExpect(status().isOk());
        assertEquals(beforeSizeRDB + 1, (int) workOrderRDBRepository.count());
        assertEquals(beforeSizeNosql + 1, (int) workOrderNosqlRepository.count());
    }

    @Test
    @DisplayName("임시 저장")
    void tmpSave() throws Exception {
        // given
        Long id = workOrderRDBRepository.findAll().getFirst().getId();
        String uuid = UUID.randomUUID().toString();
        String measure = "10";
        String memo = "memo";
        boolean passOrNot = true;

        JSONObject body = new JSONObject();
        JSONArray list = new JSONArray();
        JSONObject item1 = new JSONObject();
        item1.put("uuid", uuid);
        item1.put("measure", measure);
        item1.put("memo", memo);
        item1.put("passOrNot", passOrNot);
        list.put(item1);
        body.put("id", id);
        body.put("checkList", list);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .put("/v1/work-order/tmp")
                .content(body.toString())
                .contentType(MediaType.APPLICATION_JSON);

        // when
        ResultActions perform = mockMvc.perform(request);

        // then
        perform.andExpect(status().isOk());

        List<WorkOrderCheckItem> checkList = workOrderNosqlRepository.findById(
                workOrderRDBRepository.findById(id).get().getCheckListId()).get().getCheckList();
        assertEquals(uuid, checkList.get(0).uuid());
        assertEquals(measure, checkList.get(0).measure());
        assertEquals(memo, checkList.get(0).memo());
        assertTrue(checkList.get(0).passOrNot());
    }

    @Test
    @DisplayName("저장")
    void done() throws Exception {
        // given
        Long id = workOrderRDBRepository.findAll().getFirst().getId();
        String uuid = UUID.randomUUID().toString();
        String measure = "10";
        String memo = "memo";
        boolean passOrNot = true;

        JSONObject body = new JSONObject();
        JSONArray list = new JSONArray();
        JSONObject item1 = new JSONObject();
        item1.put("uuid", uuid);
        item1.put("measure", measure);
        item1.put("memo", memo);
        item1.put("passOrNot", passOrNot);
        list.put(item1);
        body.put("id", id);
        body.put("checkList", list);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .put("/v1/work-order/done")
                .content(body.toString())
                .contentType(MediaType.APPLICATION_JSON);

        // when
        ResultActions perform = mockMvc.perform(request);

        // then
        perform.andExpect(status().isOk());

        List<WorkOrderCheckItem> checkList = workOrderNosqlRepository.findById(
                workOrderRDBRepository.findById(id).get().getCheckListId()).get().getCheckList();
        assertEquals(uuid, checkList.get(0).uuid());
        assertEquals(measure, checkList.get(0).measure());
        assertEquals(memo, checkList.get(0).memo());
        assertTrue(checkList.get(0).passOrNot());
        assertTrue(workOrderRDBRepository.findById(id).get().getTerminatorEmployeeNo() != null);
    }

    @Test
    @DisplayName("상태 변경")
    void updateStatus() throws Exception {
        // given
        Long id = workOrderRDBRepository.findAll().getFirst().getId();
        WorkOrderStatus status = WorkOrderStatus.FINISH;

        JSONObject body = new JSONObject();
        JSONObject item = new JSONObject();
        item.put("id", id);
        item.put("status", status);
        JSONArray list = new JSONArray();
        list.put(item);
        body.put("list", list);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .put("/v1/work-order/status")
                .content(body.toString())
                .contentType(MediaType.APPLICATION_JSON);

        // when
        ResultActions perform = mockMvc.perform(request);

        // then
        perform.andExpect(status().isOk());
        assertEquals(status, workOrderRDBRepository.findById(id).get().getStatus());
    }
}