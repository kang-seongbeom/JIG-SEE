import { create } from "zustand";
import { getMonthJig, getJigcount, updateChecked, getOptimal, getManagerGraph } from "@/pages/api/dashboard";

import { AxiosResponse } from "axios";
interface Jiglocation {
  model: string;
  countReady: number;
  countWarehouse: number;
}
interface CheckItem {
  content: string;
  standard: string;
}
interface Updated {
  model: string;
  checkItems: CheckItem[];
}
interface dashboardstore {
  // 지그 수리, 폐기 수
  deleted: number;
  change: number;
  request: number;
  finish: number;
  isLoading: boolean;
  setIsLoading: (n: boolean) => void;
  getJignumbers: () => Promise<void>;
  // 지그 창고, 현장대기 수
  maxcount: number;
  modelscount: Jiglocation[];
  getJigcounts: () => Promise<void>;
  //점검항목 수정된 지그 리스트
  getJigupdated: () => Promise<void>;
  updatedList: Updated[];
  // Jig model
  jigmodel: string;
  setJigmodel: (jigmodel: string) => void;
  //jig 수리 횟수별 수명
  getInterval: (id: string) => Promise<void>;
  optimalList: [];
  xlabelList: string[];
}

export const useDashboardstore = create<dashboardstore>((set) => ({
  // 지그 수리, 폐기 수
  deleted: 0,
  change: 0,
  request: 0,
  finish: 0,
  isLoading: true,
  setIsLoading: (n: boolean) => {
    set({ isLoading: n });
  },
  getJignumbers: async () => {
    const data = await getMonthJig();
    set({
      change: data.countChange,
      deleted: data.countDelete,
      request: data.countRepairRequest,
      finish: data.countRepairFinish,
    });
    return data;
  },
  // 지그 창고, 현장대기 수
  maxcount: 0,
  modelscount: [],
  getJigcounts: async () => {
    const data = await getJigcount();
    set({ modelscount: data.jigModelCountList, maxcount: data.maxCount });
  },
  updatedList: [],
  getJigupdated: async () => {
    const data = await updateChecked();
    set({ updatedList: data.updatedJigList });
  },
  //jig model 아이디 변수 변경
  jigmodel: "",
  setJigmodel: (newjig: string) => {
    set({ jigmodel: newjig });
  },
  optimalList: [],
  xlabelList: [],
  getInterval: async (id: string) => {
    const data = await getOptimal(id);
    set({ optimalList: data.data });
    // optimalList의 길이 가져오기
    const optimalListLength = data.data.length;

    // ["0회", "1회", ..., "length회"] 리스트 생성
    const countList = Array.from({ length: optimalListLength }, (_, i) => `${i}회`);
    set({ xlabelList: countList });
  },
}));


interface graphInfo {
  day: number,
  output: number,
  cost: number,
  yield: number,
  countMissingJig : number,
}

interface Infos {
  infos: graphInfo[],
  fetchManagerGraph: () => Promise<void>,
}


export const useManagerGraphStore = create<Infos>((set) => ({
  infos: [],
  fetchManagerGraph: async() => {
    const data = await getManagerGraph();
    set({
      infos: data
    })
    return
  }
}))