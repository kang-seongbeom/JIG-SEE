import styled from "@/styles/dashboard/mywo.module.css";
import { useUserWoListStore } from "@/store/workorderstore";
import { useEffect, useState } from "react";
import { Link, divider } from "@nextui-org/react";
import VerifiedIcon from "@mui/icons-material/Verified";
import RadioButtonUncheckedIcon from "@mui/icons-material/RadioButtonUnchecked";
import BuildCircleIcon from "@mui/icons-material/BuildCircle";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";

export default function MyWoList() {
  const { list } = useUserWoListStore();
  return (
    <>
      <div className={styled.box}>
        <div className={styled.title}> 나의 수리 진행 내역</div>
        {list.length != 0 ? (
          <>
            <div className={styled.content}>
              {list.map((lst, index) => (
                <div className={styled.card} key={index}>
                  {lst.status === "PUBLISH" && (
                    <div>
                      <RadioButtonUncheckedIcon color="primary" /> 발행
                    </div>
                  )}
                  {lst.status === "PROGRESS" && (
                    <div>
                      <BuildCircleIcon color="primary" /> 진행 중
                    </div>
                  )}
                  {lst.status === "FINISH" && (
                    <div>
                      <CheckCircleIcon color="primary" /> 완료
                    </div>
                  )}
                  <div>{lst.model}</div>
                  {lst.terminator === "" ? (
                    <div>
                      {lst.createdAt[0]}.{lst.createdAt[1]}.{lst.createdAt[2]} |{" "}
                      {lst.creator}
                    </div>
                  ) : (
                    <div>
                      {lst.updatedAt[0]}.{lst.updatedAt[1]}.{lst.updatedAt[2]} |{" "}
                      {lst.terminator}
                    </div>
                  )}
                </div>
              ))}
            </div>
            <div className={styled.link}>
              <Link href="/common/RepairTotal/MyTotal">전체 보기</Link>
            </div>
          </>
        ) : (
          <div className={styled.isempty}>
            {" "}
            현재 수리 진행 중인 지그가 없습니다{" "}
          </div>
        )}
      </div>
    </>
  );
}
