package com.sdi.common.entity;

import com.sdi.common.dto.JigItemDto;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "want_responses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class WantResponseEntity {
    @Id
    private String id; // uuid
    private LocalDateTime time; // 응답 시간
    @Field("sender")
    private Long senderId; // 송신자의 DB 아이디
    @Field("receiver")
    private Long receiverId; // 수신자의 DB 아이디
    @Field("is_accept")
    private Boolean isAccept; // 승인 여부
    @Field("request_id")
    private String requestId; // 원본 요청의 mongoDB 아이디
    @Field("serial_no_list")
    private List<JigItemDto> jigList; // 불출 지그 일련번호 리스트
    private String memo; // 알림 내용
}
