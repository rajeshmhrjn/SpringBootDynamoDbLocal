package com.rm.sbddl.model;

import java.util.List;

public record BatchResult<T>(List<T> succeeded, List<T> failed) {}
