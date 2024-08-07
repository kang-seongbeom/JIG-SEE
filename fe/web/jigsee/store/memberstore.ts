import { create } from "zustand";
import {searchUser} from "@/pages/api/memberAxios";
import { useEffect, useState } from "react";
import {AxiosResponse} from "axios";

interface user {
  name: string;
  setName: (n: string) => void;
  role: string;
  setRole: (n: string) => void;
  employeeNo: string;
  setEmployeeNo: (n:string) => void;
}

export const userStore = create<user>((set) => ({
  name: "", // userName 초기값
  role: "", // userRole 초기값
  setName: (newName: string) => {
    if (typeof window !== "undefined") {
      // 클라이언트 사이드에서만 실행
      localStorage.setItem("name", newName); // 로컬 스토리지에 이름 저장
      set({ name: newName });
    }
  },
  setRole: (newRole: string) => {
    if (typeof window !== "undefined") {
      // 클라이언트 사이드에서만 실행
      localStorage.setItem("role", newRole); // 로컬 스토리지에 역할 저장
      set({ role: newRole });
    }
  },
  employeeNo: "",
  setEmployeeNo: (n:string) => {
    if (typeof window !== "undefined") {
      localStorage.setItem("employeeNo", n);
      set({employeeNo: n})
    }
  }
}));

interface searchUser {
  id: number,
  name: string,
  employeeNo: string,
  role: string,
  fetchSearchUser: () => void;
}


export const useSearchUser = create<searchUser>((set) => ({
  id: 0,
  name: "",
  employeeNo: "",
  role: "",
  fetchSearchUser: async() => {
    const data = await searchUser();
    set({
      id: data.id,
      name: data.name,
      employeeNo: data.employeeNo,
      role: data.role,
    })
  }
}))

















