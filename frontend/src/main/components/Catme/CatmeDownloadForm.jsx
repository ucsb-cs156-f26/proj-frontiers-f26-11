import React from "react";
import { Button, Form } from "react-bootstrap";

export default function CatmeDownloadForm({ downloadAction, testIdPrefix }) {
  const handleSubmit = (event) => {
    event.preventDefault();
    downloadAction();
  };

  return (
    <Form onSubmit={handleSubmit}>
      <Form.Group>
        <h3 data-testid={`${testIdPrefix}-catme-download-header`}>
          CATME Downloads
        </h3>
        <Form.Text className="text-muted d-block mb-3">
          Export the course roster in CATME format.
        </Form.Text>
      </Form.Group>
      <Button
        type="submit"
        variant="primary"
        data-testid={`${testIdPrefix}-btn-download-catme-csv`}
      >
        Download CSV Roster in CATME format
      </Button>
    </Form>
  );
}
