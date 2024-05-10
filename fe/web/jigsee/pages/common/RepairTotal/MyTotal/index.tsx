import {Link, Pagination, Select, SelectItem} from "@nextui-org/react";
import React, { useState, useEffect } from "react";
import styled from "@/styles/Total/MyTotal.module.css";
import EngineerNav from "@/pages/engineer/navbar";
import ManagerNav from "@/pages/manager/navbar";
import { useReleaseStore } from "@/store/releasestore";
import {useUserWoListStore} from "@/store/workorderstore";

interface Option {
  label: string;
  value: string;
}

interface JigData {
  date: string;
  serialNumber: string;
  model: string;
  status: string; // 가정으로 추가한 속성입니다.
}

interface Props {
  onClick(): void;
}

export default function RepairTotal() {
  const { list,fetchUserWo, endPage} = useUserWoListStore()
  const [role, setRole] = useState<string>(""); // 초기 상태를 명시적으로 string 타입으로 설정
  useEffect(() => {
    // 컴포넌트가 클라이언트 사이드에서 마운트되었을 때 로컬 스토리지에서 role 읽기
    const storedRole = localStorage.getItem("role");
    if (storedRole !== null) {
      setRole(storedRole); // 로컬 스토리지의 값이 null이 아닌 경우에만 상태 업데이트
    }
  }, []);
  let Navbar;
  if (role === "MANAGER") {
    Navbar = <ManagerNav />;
  } else if (role === "ENGINEER") {
    Navbar = <EngineerNav />;
  } else {
    Navbar = <ManagerNav />; // 기본값으로 ManagerNav 설정
  }
  // 수리요청온 지그의 WO 에 따른 상태 데이터
  const lst: Option[] = [
    { label: "ALL", value: "ALL" },
    { label: "발행", value: "PUBLISH" },
    { label: "불출완료", value: "FINISH" },
    { label: "반려", value: "REJECT" },
  ];
  // 불출 상황 옵션 변수
  const [values, setValues] = useState<string>("ALL");
  const [page, setPage] = useState<number>(1);
  // page와 선택 옵션이 바뀜에 따라 api 호출
  useEffect(() => {
    const employeeNo = localStorage.getItem("employeeNo") || ""
    const name = localStorage.getItem("name") || ""
    setIsLoading(true); // API 호출 시작 전에 로딩 상태를 true로 설정
    fetchUserWo(employeeNo, name, page, 5)
      .then(() => {
        setIsLoading(false); // 데이터를 성공적으로 받아온 후에 로딩 상태를 false로 설정
      })
      .catch(() => {
        setIsLoading(false); // 에러가 발생해도 로딩 상태를 false로 설정
      });
  }, [page, values]);
  // 임시 JIG 데이터- api 요청으로 불러오기
  const jigData: JigData[] = [
    { date: "2024.04.21", serialNumber: "S/N S00000001", model: "Model Name", status: "PUBLISH" },
    { date: "2024.04.22", serialNumber: "S/N S00000002", model: "Model Name", status: "PUBLISH" },
    { date: "2024.04.23", serialNumber: "S/N S00000003", model: "Model Name", status: "REJECT" },
    { date: "2024.04.21", serialNumber: "S/N S00000004", model: "Model Name", status: "REJECT" },
    { date: "2024.04.22", serialNumber: "S/N S00000005", model: "Model Name", status: "FINISH" },
    // 다른 JIG 데이터 객체들...
  ];
  // 선택한 값에 따라 필터링된 jigData를 저장할 상태 변수
  const [filteredJigData, setFilteredJigData] = useState<JigData[]>([]);
  // 값이 변경될 때마다 필터링된 데이터 업데이트
  useEffect(() => {
    console.log("vvv", values);
    if (values === "ALL") {
      // "ALL"이면 전체 데이터 표시
      setFilteredJigData(jigData);
    } else {
      // 선택한 값에 따라 jigData 필터링
      const filteredData = jigData.filter((jig) => jig.status === values);
      setFilteredJigData(filteredData);
    }
  }, [values]);

  function cardClick(jigid: number) {
    console.log("clicked", jigid);
  }
  const [isLoading, setIsLoading] = useState(true);


  return (
    <>
      {Navbar}
      <div className={styled.bigcontainer}>
        <div className={styled.right}>
          <Link
              href="/common/RepairTotal"
              // passHref
              underline="hover"
              style={{color: "black", fontSize: "12px", fontWeight: "lighter"}}
          >
            전체 내역 보기
          </Link>
          <div></div>
        </div>

        <div className={styled.container}>
          {isLoading ? (
            <p>Loading...</p>
          ) : (
            list.map((jig, index) => (
              <div key={index} onClick={() => cardClick(jig.id)} className={styled.fullWidth}>
                <p>
                  {jig.id} | 요청자 {jig.creator} | 생성일 {jig.createdAt} | 승인자 {jig.terminator} | 수정일 {jig.updatedAt} | status: {jig.status}
                </p>
              </div>
            )))
          }
        </div>
        <div className={styled.center}>
          <Pagination onChange={(e) => setPage(e)} total={endPage} />
        </div>
      </div>
    </>
  );
}