import { render, screen } from "@testing-library/react";
import { describe, expect, test } from "vitest";

import CatmeTabComponent from "main/components/TabComponent/CatmeTabComponent";

describe("CatmeTabComponent tests", () => {
  test("renders the placeholder text", () => {
    render(<CatmeTabComponent testIdPrefix="TestPrefix" />);

    const tab = screen.getByTestId("TestPrefix-catme-tab-component");
    expect(tab).toHaveTextContent("CATME features coming soon");
  });
});
