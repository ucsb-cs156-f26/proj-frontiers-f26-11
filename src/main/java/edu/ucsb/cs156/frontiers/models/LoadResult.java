package edu.ucsb.cs156.frontiers.models;

import java.util.List;

public record LoadResult<T>(Integer created, Integer updated, Integer dropped, List<T> rejected) {}
