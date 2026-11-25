import React from "react";
import CatmeDownloadForm from "main/components/Catme/CatmeDownloadForm";
import { downloadCsv } from "main/utils/downloadUtils";

export default function CatmeTabComponent({ courseId, testIdPrefix }) {
  const handleDownloadCatmeCsv = () => {
    downloadCsv({
      url: "/api/csv/catme",
      params: { courseId },
      filename: `catme_roster_${courseId}.csv`,
    });
  };

  return (
    <div data-testid={`${testIdPrefix}-catme-tab-component`}>
      <CatmeDownloadForm
        downloadAction={handleDownloadCatmeCsv}
        testIdPrefix={testIdPrefix}
      />
    </div>
  );
}
