import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, test, vi } from "vitest";

import CatmeDownloadForm from "main/components/Catme/CatmeDownloadForm";

describe("CatmeDownloadForm tests", () => {
  test("renders the download button labeled as the CATME roster download", () => {
    render(
      <CatmeDownloadForm downloadAction={() => {}} testIdPrefix="TestPrefix" />,
    );

    const button = screen.getByTestId("TestPrefix-btn-download-catme-csv");
    expect(button).toHaveTextContent(
      "Download CSV Roster in CATME format",
    );
  });

  test("calls downloadAction when the form is submitted", () => {
    const downloadAction = vi.fn();
    render(
      <CatmeDownloadForm
        downloadAction={downloadAction}
        testIdPrefix="TestPrefix"
      />,
    );

    fireEvent.click(screen.getByTestId("TestPrefix-btn-download-catme-csv"));

    expect(downloadAction).toHaveBeenCalledTimes(1);
  });
});
