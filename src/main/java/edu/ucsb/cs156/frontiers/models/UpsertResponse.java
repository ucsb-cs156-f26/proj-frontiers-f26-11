package edu.ucsb.cs156.frontiers.models;

import edu.ucsb.cs156.frontiers.enums.InsertStatus;

public record UpsertResponse<T>(InsertStatus insertStatus, T entity) {
  public InsertStatus getInsertStatus() {
    return insertStatus;
  }
}
