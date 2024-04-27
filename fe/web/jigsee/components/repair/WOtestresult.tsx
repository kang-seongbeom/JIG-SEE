import React from "react";
import styles from "@/styles/wotestresult.module.scss"; // Corrected import
import { DateInput } from "@nextui-org/react";
import { CalendarDate } from "@internationalized/date";
import Box from "@mui/material/Box";
import { DataGrid, GridColDef } from "@mui/x-data-grid";

export default function WOtestresult() {
  const columns: GridColDef<(typeof rows)[number]>[] = [
    { field: "contents", headerName: "기준 항목", width: 90 },
    {
      field: "standard",
      headerName: "기준 값",
      width: 90,
    },
    {
      field: "measure",
      headerName: "측정 값",
      width: 90,
      editable: true,
    },
    {
      field: "memo",
      headerName: "비고",
      width: 90,
      editable: true,
    },
    {
      field: "passOrNot",
      headerName: "판정 결과",
      width: 100,
      type: "boolean",
      editable: true,
    },
  ];
  const rows = [
    { id: 1, contents: "직경", standard: "2.99-3m", measure: "2", memo: "모름", passOrNot: true },
  ];
  return (
    <div className={styles.container}>
      <div className={styles.header}>Test Result</div>
      <div className={styles.body}>
        <div className={styles.inputname}>
          <label htmlFor="inputname">책임자</label>
          <input type="text" className={styles.inputname} />
        </div>

        <div className={styles.datepicker}>
          <DateInput
            className={styles.dateInput}
            label={"시작일"}
            isRequired
            placeholderValue={new CalendarDate(1995, 11, 6)}
          />
          <DateInput
            className={styles.dateInput}
            label={"종료일"}
            isRequired
            placeholderValue={new CalendarDate(1995, 11, 6)}
          />
        </div>

        <Box className={styles.box}>
          <DataGrid
            rows={rows}
            columns={columns}
            disableColumnMenu
            disableColumnSorting
            disableColumnResize
            disableVirtualization
            hideFooter
          />
        </Box>
        <button className={styles.editbtn}>수정</button>
      </div>
    </div>
  );
}
