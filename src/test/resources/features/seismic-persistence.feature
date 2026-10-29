Feature: Seismic event and risk persistence
  As a building administrator
  I want seismic events and their building risk analysis to be persisted
  So that I can review event history and affected zones

  Scenario: List stored seismic events in a time range
    Given PostgreSQL contains seismic events inside and outside a date range
    When I request seismic events for that date range
    Then I receive only the stored events in the range newest first

  Scenario: Retrieve the latest building risk index and affected zones
    Given PostgreSQL contains a risk index with affected zones for the building
    When I request the latest risk index for the building
    Then I receive the stored risk level and its affected zones ordered by severity
