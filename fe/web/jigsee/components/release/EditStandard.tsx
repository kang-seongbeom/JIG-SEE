import React, { useState, useEffect } from "react";
import styled from "@/styles/jigrequest.module.scss";
import { Card, CardHeader, CardBody, CardFooter } from "@nextui-org/react";
import { Select, SelectItem, Divider, Input } from "@nextui-org/react";
import { updatejigMethod } from "@/pages/api/jigAxios";
import { useFacilityStore } from "@/store/facilitystore";

// Step 1: prop types 정의
interface EditStandardProps {
  onClose: () => void; // Function that will be called to close the modal
}
interface RowData {
  content: string;
  standard: string;
}

export default function EditStandard({ onClose }: EditStandardProps) {
  const { facilityID, facilities, loadFacilities, setfacilityID, editStandardJigs, setEditJigs } =
    useFacilityStore();
  const [selectedFacility, setSelectedFacility] = useState(0);
  const [selectedModel, setSelectedModel] = useState("");

  // Row 타입의 배열로 rows 상태를 정의합니다.
  const [rows, setRows] = useState<RowData[]>([{ content: "", standard: "" }]);

  // 새로운 행을 추가하는 함수
  const addRow = () => {
    setRows([
      ...rows,
      { content: "", standard: "" }, // Added an `id` and changed 'contents' to 'content'
    ]);
  };

  // 특정 행의 데이터를 변경하는 함수
  const updateRow = (content: number, field: keyof RowData, value: string) => {
    const newRow = [...rows];
    if (field === "content" || field === "standard") {
      newRow[content][field] = value; // Ensuring the field matches the RowData keys
      setRows(newRow);
    }
  };

  // 지그 점검 항목 제출 버튼
  const submitRow = async () => {
    console.log("ssmodel", selectedModel);
    try {
      console.log("tttt", rows);
      // jig model도 전달하는 로직으로 수정필요
      const result = await updatejigMethod(selectedModel, rows);
      console.log("점검항목수정완", result);
      window.alert("점검 항목 수정이 완료되었습니다.");
    } catch (error) {
      console.error("Failed to fetch data:", error);
    }
    onClose();
  };
  // 설비 선택이 바뀔때마다 설비 아이디 세팅
  useEffect(() => {
    loadFacilities();
    // loadFacilities()가 완료된 후에 실행되어야 하므로 이를 보장하는 로직 필요
    setfacilityID(selectedFacility);
    setEditJigs(selectedFacility);
  }, [selectedFacility, setfacilityID, setEditJigs, loadFacilities]);
  return (
    <>
      <Card className={styled.requestcontainer}>
        <CardHeader className={styled.cardheader}>지그 점검 항목 수정</CardHeader>
        <Divider className={styled.headerline} />
        <CardBody>
          <div className={styled.cardbody}>
            <Select
              variant="bordered"
              label="설비 이름"
              labelPlacement="outside-left"
              placeholder="설비를 선택 하세요"
              onChange={(e) => setSelectedFacility(+e.target.value)}
              className={styled.select}
            >
              {facilities.map((facility) => (
                <SelectItem key={facility.id} value={facility.model}>
                  {facility.model}
                </SelectItem>
              ))}
            </Select>
            <Select
              variant="bordered"
              label="Jig model"
              labelPlacement="outside-left"
              placeholder="모델을 선택 하세요"
              className={styled.select}
              onChange={(e) => setSelectedModel(e.target.value)}
            >
              {editStandardJigs.map((model) => (
                <SelectItem key={model} value={model}>
                  {model}
                </SelectItem>
              ))}
            </Select>
            <div className={styled.addbtnline}>
              보수 방법{" "}
              <img
                className={styled.icon}
                src="/images/add_blue_btn.svg"
                alt="add-button"
                onClick={addRow}
              />
            </div>
            <Divider className={styled.headerline} />

            {rows.map((row, content) => (
              <div key={content} className={styled.rowcontainer}>
                <label>
                  방법
                  <input
                    type="text"
                    value={row.content}
                    onChange={(e) => updateRow(content, "content", e.target.value)}
                  />
                </label>
                <label>
                  기준
                  <input
                    type="text"
                    value={row.standard}
                    onChange={(e) => updateRow(content, "standard", e.target.value)}
                  />
                </label>
              </div>
            ))}
          </div>
        </CardBody>

        <CardFooter className={styled.cardfooter}>
          <button className={styled.addbtn} onClick={submitRow}>
            제출
          </button>
        </CardFooter>
      </Card>
    </>
  );
}
