import { Input, Button, Checkbox } from "@nextui-org/react";
import React from "react";
import styled from "@/styles/login.module.scss";
import {useRouter} from "next/router";
import  from "@/utils/instance";



export default function Login() {
  return (
    <>
      <div className={styled.container}>
        <div className={styled.logincontainer}>
          <img
            src="/images/userprofile.svg"
            alt="profile-logo"
            style={{ width: "50px", height: "50px" }}
          />
          {/* 로그인 인풋 영역 */}
          <div style={{ marginBottom: "1rem" }}>
            <Input type="id" variant={"underlined"} label="사번" />
          </div>
          <div style={{ marginBottom: "1rem" }}>
            <Input type="password" variant={"underlined"} label="비밀번호" />
          </div>
          <div style={{ marginBottom: "2rem", justifyContent: "flex-end", display: "flex" }}>
            <Checkbox defaultSelected>아이디 저장</Checkbox>
          </div>
          <Button className={styled.samsungbutton}>로그인</Button>
        </div>
      </div>
    </>
  );
}
