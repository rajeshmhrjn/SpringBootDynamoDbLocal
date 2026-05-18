package com.rm.sbddl.dto;

import java.util.List;

public record BatchResult<T>(List<T> succeeded, List<T> failed) {}
