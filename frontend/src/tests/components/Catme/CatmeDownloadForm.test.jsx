import { createEvent, fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, test, vi } from "vitest";

import CatmeDownloadForm from "main/components/Catme/CatmeDownloadForm";

describe("CatmeDownloadForm tests", () => {
  test("renders the download form with the CATME download header and button", () => {
    render(
      <CatmeDownloadForm downloadAction={() => {}} testIdPrefix="TestPrefix" />,
    );

    expect(
      screen.getByTestId("TestPrefix-catme-download-header"),
    ).toHaveTextContent("CATME Downloads");
    const button = screen.getByTestId("TestPrefix-btn-download-catme-csv");
    expect(button).toHaveTextContent("Download CSV Roster in CATME format");
  });

  test("calls downloadAction and prevents the default form submission", () => {
    const downloadAction = vi.fn();
    render(
      <CatmeDownloadForm
        downloadAction={downloadAction}
        testIdPrefix="TestPrefix"
      />,
    );

    const form = screen
      .getByTestId("TestPrefix-btn-download-catme-csv")
      .closest("form");
    const submitEvent = createEvent.submit(form);
    fireEvent(form, submitEvent);

    expect(downloadAction).toHaveBeenCalledTimes(1);
    expect(submitEvent.defaultPrevented).toBe(true);
  });
});
