import Head from "next/head";
import Image from "next/image";
import { Inter } from "next/font/google";
import Stocklist from "@/components/release/StockList"
import styled from "@/styles/stocklist.module.css"
import Jigdetail from "@/components/repair/JigDetail"
const inter = Inter({ subsets: ["latin"] });

export default function Home() {

  return (
      <>
          <div>
              <Jigdetail/>
          </div>
      </>
  );
}
