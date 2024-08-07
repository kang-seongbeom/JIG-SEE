import React, { useState, useEffect } from "react";
import ReactApexChart from "react-apexcharts";
import { ApexOptions } from "apexcharts";
import { useDashboardstore } from "@/store/dashboardstore";
export default function Lifetime() {
  const [model, setModel] = useState("");
  const { jigmodel, getInterval, optimalList, xlabelList } =
    useDashboardstore();
  useEffect(() => {
    setModel(jigmodel);
    if (jigmodel !== "") {
      getInterval(jigmodel);
    }
  }, [jigmodel]);
  // 차트에 표시할 데이터
  const series = [
    {
      name: "예상 점검 주기",
      data: optimalList,
    },
  ];

  // 차트 옵션 설정
  const options: ApexOptions = {
    chart: {
      height: 350,
      type: "line",
      zoom: {
        enabled: false,
      },
      animations: {
        enabled: true,
        easing: "easeinout",
        speed: 800,
        animateGradually: {
          enabled: true,
          delay: 150,
        },
        dynamicAnimation: {
          enabled: true,
          speed: 350,
        },
      },
    },
    markers: {
      size: 6,
    },
    dataLabels: {
      enabled: false,
    },
    stroke: {
      curve: "straight",
    },
    title: {
      text: `수리 횟수에 따른 예상 점검주기 : ${model || "모델을 선택 하세요"}`,
      align: "center",
    },
    grid: {
      row: {
        colors: ["transparent", "transparent"], // takes an array which will be repeated on columns
        opacity: 0.5,
      },
    },
    xaxis: {
      categories: xlabelList,
    },
  };

  // ReactApexChart 컴포넌트를 사용하여 차트 렌더링
  return (
    <div>
      <ReactApexChart
        width="500px"
        options={options}
        series={series}
        type="line"
        height={270}
      />
    </div>
  );
}
