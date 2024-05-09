import Navbar from "./navbar";
import styled from "@/styles/engineer.module.scss";
// test 확인 지우기
import WOtest from "@/components/repair/WOtestresult";
import RepairList from "@/components/repair/List";
import RepairRequest from "@/components/repair/Requests";
import Information from "@/components/repair/JigDetail";
import {
  useWoStore,
  useCompoStore,
  useWoGroupStore,
  useUserWoListStore,
} from "@/store/workorderstore";
import { DndProvider, useDrop } from "react-dnd";
import { HTML5Backend } from "react-dnd-html5-backend";
import React, { useEffect } from "react";
import { useReleaseModalStore } from "@/store/releasestore";
import { useState } from "react";
import Report from "@/components/workorder/template";
import Request from "@/components/repair/Requests";
import { useGroupFilter } from "@/store/repairrequeststore";
import Modal from "@mui/material/Modal";
import Box from "@mui/material/Box";
import CreateWoModal from "@/components/workorder/CreateWoModal";
import { userStore } from "@/store/memberstore";
import ReuseModal from "@/components/repair/ReuseModal";
import DisposeModal from "@/components/repair/DisposeModal";

export default function Repair() {
  const { rightCompo } = useCompoStore();
  const { fetchWoGroup, publish, progress, finish } = useWoGroupStore();
  const { fetchUserWo } = useUserWoListStore();
  useEffect(() => {
    const employeeNo = localStorage.getItem("employeeNo") || "";
    const name = localStorage.getItem("name") || "";
    fetchUserWo(employeeNo, name, 1, 10)
      .then((res) => {
        console.log("on reapir userwo", res);
      })
      .catch((error) => {
        console.log(error.message);
      });
    fetchWoGroup()
      .then((res) => {
        console.log("on reapir groupwo", res);
      })
      .catch((error) => {
        console.log(error.message);
      });
  }, []);
  const { setModal, setModalName, modal, modalName } = useCompoStore();

  return (
    <>
      <Navbar />
      <div className={styled.container}>
        <DndProvider backend={HTML5Backend}>
          <div className={styled.jigrepairlist}>
            <RepairList />
          </div>
          <div className={styled.repairrequest}>
            {rightCompo === "PUBLISH" && <Information />}
            {rightCompo === "PROGRESS" && <Information />}
            {rightCompo === "FINISH" && <Information />}
            {rightCompo === "REQUEST" && <Request />}
            {rightCompo === "TEST" && <WOtest />}
          </div>
        </DndProvider>
      </div>
      <Modal
        open={modal} // Corrected from 'open'
        onClose={() => {
          setModal(false);
        }} // Added onClose handler
        aria-labelledby="modal-modal-title"
        aria-describedby="modal-modal-description"
        sx={{
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          "& .MuiBox-root": {
            // Assuming the box is causing issues
            outline: "none",
            border: "none",
            boxShadow: "none",
          },
        }}
      >
        <Box
          sx={{
            width: "100%",
            height: "80%",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
          }}
        >
          {modalName === "CREATEWO" && <CreateWoModal />}
          {modalName === "REPORT" && <Report />}
          {modalName === "REUSE" && <ReuseModal />}
          {modalName === "DISPOSE" && <DisposeModal />}
        </Box>
      </Modal>
    </>
  );
}
