package com.island.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Response containing a list of saved snapshot filenames.")
public record SnapshotListResponse(
    @Schema(description = "List of snapshot filenames")
    List<String> filenames,
    @Schema(description = "Total count of available snapshots")
    int totalCount
) {}
