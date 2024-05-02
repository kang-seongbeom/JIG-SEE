import { create } from "zustand";
import { getJigrestorerList } from "@/pages/api/restoreAxios";

interface ListData {
  createdAt: string;
  from: string;
  id: number;
}
// interface DetailData {

// }
interface RestoreState {
  page: number; // 보수요청내역 전체보기 page
  restoreList: ListData[];
  endpage: number;
  setPage: (page: number) => void;
  getRestoreList: () => Promise<void>; // page별 리스트 불러오기
  detailList:
}

export const useRestoreStore = create<RestoreState>((set, get) => ({
  page: 1,
  restoreList: [],
  endpage: 1,
  setPage: (page) => set({ page }),
  getRestoreList: async () => {
    const page = get().page; // store에 저장된 page 변수 값을 가져온다
    try {
      console.log("Fetching data for page:", page);
      const result = await getJigrestorerList(page); // 보수 요청 조회 api 함수 실행
      set({ restoreList: result.data.result.list }); // 보수 요청 리스트 업데이트
      set({ endpage: result.data.result.endPage }); // 보수 요청 전체보기 총 페이지 수 업데이트
    } catch (error) {
      console.error("Failed to fetch data:", error);
      set({ restoreList: [] }); // Handle error by setting an empty array
    }
  },
}));
