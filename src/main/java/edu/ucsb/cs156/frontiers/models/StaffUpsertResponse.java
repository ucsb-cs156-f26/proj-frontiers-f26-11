package edu.ucsb.cs156.frontiers.models;

import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.enums.InsertStatus;

public record StaffUpsertResponse(InsertStatus insertStatus, CourseStaff courseStaff) {
  public InsertStatus getInsertStatus() {
    return insertStatus;
  }
}
