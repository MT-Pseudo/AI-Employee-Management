package com.company.ems.ai;
import java.util.List;
public interface NarrativeGenerator {
    String name();
    String employeeNarrative(EmployeeMetrics m, List<String> insights, List<String> recommendations);
    String organizationNarrative(OrgMetrics m, List<String> insights);
}