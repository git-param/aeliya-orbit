package com.aeliya.orbit.modules.permission.dto;

import java.util.Set;

public record PermissionGrantRequest(Set<String> permissions) {}
